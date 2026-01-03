from fastapi import FastAPI, HTTPException
import pandas as pd
import numpy as np
from xgboost import XGBRegressor
from fastapi.middleware.cors import CORSMiddleware
CSV_PATH = "olive_oil_merged_filtered.csv"
MODEL_7D_PATH = "xgb_olive_7d.json"
MODEL_30D_PATH = "xgb_olive_30d.json"
FEATS_7D_PATH = "features_7d.txt"
FEATS_30D_PATH = "features_30d.txt"

app = FastAPI(title="Olive Oil Forecast API", version="1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200", "http://127.0.0.1:4200"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

def load_feature_list(path: str):
    with open(path, "r", encoding="utf-8") as f:
        return [line.strip() for line in f if line.strip()]


FEATURES_7D = load_feature_list(FEATS_7D_PATH)
FEATURES_30D = load_feature_list(FEATS_30D_PATH)


def load_model(path: str) -> XGBRegressor:
    m = XGBRegressor()
    m.load_model(path)
    return m


MODEL_7D = load_model(MODEL_7D_PATH)
MODEL_30D = load_model(MODEL_30D_PATH)


def make_features(df: pd.DataFrame) -> pd.DataFrame:
    df["date"] = pd.to_datetime(df["date"], errors="coerce")
    df = df.dropna(subset=["date"]).copy()

    df["region"] = df["region"].astype(str).str.lower().str.strip()
    df["produit"] = df["produit"].astype(str).str.lower().str.strip()
    df["produit"] = "huile_olive"

    for col in ["qte_tonne", "px_min", "px_max", "px_moyen", "spread"]:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors="coerce")

    df = df.sort_values(["region", "date"]).reset_index(drop=True)

    df["dow"] = df["date"].dt.dayofweek
    df["month"] = df["date"].dt.month
    df["weekofyear"] = df["date"].dt.isocalendar().week.astype(int)

    grp = df.groupby("region", group_keys=False)

    for lag in [1, 2, 3, 7, 14, 21, 30]:
        df[f"px_moyen_lag_{lag}"] = grp["px_moyen"].shift(lag)

    df["px_moyen_roll_mean_7"] = grp["px_moyen"].shift(1).rolling(7).mean()
    df["px_moyen_roll_mean_30"] = grp["px_moyen"].shift(1).rolling(30).mean()
    df["px_moyen_roll_std_7"] = grp["px_moyen"].shift(1).rolling(7).std()

    if "qte_tonne" in df.columns:
        df["qte_roll_mean_7"] = grp["qte_tonne"].shift(1).rolling(7).mean()
        df["qte_lag_7"] = grp["qte_tonne"].shift(7)

    if "spread" in df.columns:
        df["spread_lag_1"] = grp["spread"].shift(1)
        df["spread_roll_mean_7"] = grp["spread"].shift(1).rolling(7).mean()

    df = pd.get_dummies(df, columns=["region"], prefix="region", drop_first=False)
    return df


def get_last_row_for_region(df_feat: pd.DataFrame, region: str) -> pd.Series:
    region = region.lower().strip()
    col = f"region_{region}"
    if col not in df_feat.columns:
        raise HTTPException(status_code=404, detail=f"Unknown region '{region}'. Available example: sfax, centre, sahel, nord, sud")

    sub = df_feat[df_feat[col] == 1].sort_values("date")
    if sub.empty:
        raise HTTPException(status_code=404, detail=f"No data for region '{region}'")

    last = sub.iloc[-1]

    # Need some lags present to predict
    if pd.isna(last.get("px_moyen_lag_1")) or pd.isna(last.get("px_moyen_lag_7")):
        raise HTTPException(status_code=400, detail=f"Not enough history to predict for region '{region}'")

    return last


def predict(region: str, horizon: int):
    df_raw = pd.read_csv(CSV_PATH)
    df_feat = make_features(df_raw)

    last = get_last_row_for_region(df_feat, region)

    if horizon == 7:
        feats = FEATURES_7D
        model = MODEL_7D
    elif horizon == 30:
        feats = FEATURES_30D
        model = MODEL_30D
    else:
        raise HTTPException(status_code=400, detail="horizon must be 7 or 30")

    # Build a 1-row dataframe with the expected feature columns
    x = pd.DataFrame([{c: last.get(c) for c in feats}])

    # Fill missing feature columns with 0 (safety for one-hot)
    x = x.fillna(0)

    y_pred = float(model.predict(x)[0])

    return {
        "region": region.lower().strip(),
        "horizon_days": horizon,
        "asof_date": str(pd.to_datetime(last["date"]).date()),
        "predicted_px_moyen": y_pred
    }

def load_raw_df() -> pd.DataFrame:
    df = pd.read_csv(CSV_PATH)
    df["date"] = pd.to_datetime(df["date"], errors="coerce")
    df = df.dropna(subset=["date"]).copy()

    df["region"] = df["region"].astype(str).str.lower().str.strip()
    df["px_moyen"] = pd.to_numeric(df["px_moyen"], errors="coerce")
    df = df.dropna(subset=["px_moyen"])

    return df
@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/predict")
def predict_endpoint(region: str, horizon: int = 7):
    return predict(region, horizon)

@app.get("/series")
def series(region: str, days: int = 120):
    region = region.lower().strip()
    if days < 7 or days > 2000:
        raise HTTPException(status_code=400, detail="days must be between 7 and 2000")

    df = load_raw_df()  # fonction que tu as ajoutée pour recent_stats
    sub = df[df["region"] == region].sort_values("date")

    if sub.empty:
        raise HTTPException(status_code=404, detail=f"No data for region '{region}'")

    tail = sub.tail(days)

    return {
        "region": region,
        "days": days,
        "points": [
            {"date": str(pd.to_datetime(d).date()), "px_moyen": float(p)}
            for d, p in zip(tail["date"], tail["px_moyen"])
        ]
    }
@app.get("/recent_stats")
def recent_stats(region: str, window: int = 7):
    """
    Returns last price + mean/min/max over last 'window' points (not necessarily consecutive days).
    """
    region = region.lower().strip()
    if window < 2 or window > 60:
        raise HTTPException(status_code=400, detail="window must be between 2 and 60")

    df = load_raw_df()
    sub = df[df["region"] == region].sort_values("date")
    if sub.empty:
        raise HTTPException(status_code=404, detail=f"No data for region '{region}'")

    tail = sub.tail(window)
    last = sub.iloc[-1]

    return {
        "region": region,
        "window": window,
        "count": int(len(tail)),
        "last_date": str(pd.to_datetime(last["date"]).date()),
        "last_px_moyen": float(last["px_moyen"]),
        "mean": float(tail["px_moyen"].mean()),
        "min": float(tail["px_moyen"].min()),
        "max": float(tail["px_moyen"].max()),
    }

@app.get("/predict_calibrated")
def predict_calibrated(region: str, horizon: int = 7, current_price: float = 0.0, stable_pct: float = 1.0):
    """
    Calibration simple: on recale le niveau du modèle sur un prix actuel fourni par l'utilisateur.
    - baseline = mean7 (moyenne des 7 dernières valeurs du dataset)
    - offset = current_price - baseline
    - pred_calibrated = pred_model + offset
    Decision: BUY_NOW / WAIT / NEUTRAL selon % variation pred_calibrated vs current_price.
    """
    if current_price <= 0:
        raise HTTPException(status_code=400, detail="current_price must be > 0")
    if stable_pct <= 0 or stable_pct > 10:
        raise HTTPException(status_code=400, detail="stable_pct must be between 0 and 10")

    # 1) stats récentes (baseline)
    stats = recent_stats(region=region, window=7)   # uses your existing endpoint function
    baseline = float(stats["mean"])

    # 2) prediction modèle
    pred = predict(region=region, horizon=horizon)  # uses your existing endpoint function
    pred_model = float(pred["predicted_px_moyen"])

    # 3) calibration par offset
    offset = current_price - baseline
    pred_cal = pred_model + offset

    # 4) recommendation vs current price
    pct = ((pred_cal - current_price) / current_price) * 100.0

    if abs(pct) <= stable_pct:
        advice = "NEUTRAL"
        advice_label = "Stable / Surveiller"
    elif pct > 0:
        advice = "BUY_NOW"
        advice_label = "Acheter maintenant (prix risque de monter)"
    else:
        advice = "WAIT"
        advice_label = "Attendre (prix risque de baisser)"

    return {
        "region": region.lower().strip(),
        "horizon_days": int(horizon),
        "dataset_baseline_mean7": baseline,
        "current_price_input": float(current_price),
        "model_pred": pred_model,
        "calibration_offset": offset,
        "predicted_calibrated": float(pred_cal),
        "pct_vs_current": float(pct),
        "advice": advice,
        "advice_label": advice_label,
        "asof_date_model": pred["asof_date"],
        "asof_date_stats": stats["last_date"],
    }