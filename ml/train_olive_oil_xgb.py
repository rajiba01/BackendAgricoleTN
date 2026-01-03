import pandas as pd
import numpy as np
import requests
from sklearn.metrics import mean_absolute_error, mean_squared_error
from xgboost import XGBRegressor

CSV_PATH = "olive_oil_merged_filtered.csv"

HORIZONS = [7, 30]  # predict t+7 and t+30
TEST_DAYS = 45      # last 45 days for test (adjust if you want)
JAVA_API_BASE = "http://localhost:8080/api"

def fetch_annonces_daily(from_date: str, to_date: str) -> pd.DataFrame:
    """
    Fetch /api/annonces/daily for HUILE and return df with date, region and annonce_* cols.
    """
    url = f"{JAVA_API_BASE}/annonces/daily"
    r = requests.get(url, params={"type": "HUILE", "from": from_date, "to": to_date}, timeout=20)
    r.raise_for_status()
    data = r.json()

    df = pd.DataFrame([{
        "date": p.get("date"),
        "region": (p.get("region") or "").lower().strip(),
        "annonce_count": p.get("annonceCount", 0),
        "annonce_price_mean": p.get("annoncePriceMean"),
        "annonce_price_min": p.get("annoncePriceMin"),
        "annonce_price_max": p.get("annoncePriceMax"),
        "annonce_quality_mean": p.get("annonceQualityMean"),
        "annonce_stock_sum": p.get("annonceStockSum"),
    } for p in data])

    if df.empty:
        return pd.DataFrame(columns=[
            "date","region","annonce_count","annonce_price_mean","annonce_price_min","annonce_price_max",
            "annonce_quality_mean","annonce_stock_sum"
        ])

    df["date"] = pd.to_datetime(df["date"], errors="coerce")
    df = df.dropna(subset=["date"]).copy()

    for c in ["annonce_count","annonce_price_mean","annonce_price_min","annonce_price_max","annonce_quality_mean","annonce_stock_sum"]:
        df[c] = pd.to_numeric(df[c], errors="coerce").fillna(0)

    return df

def merge_annonces_signals(df_raw: pd.DataFrame) -> pd.DataFrame:
    """
    Merge annonces daily features into the official dataset on (date, region).
    """
    df = df_raw.copy()
    df["date"] = pd.to_datetime(df["date"], errors="coerce")
    df = df.dropna(subset=["date"]).copy()
    df["region"] = df["region"].astype(str).str.lower().str.strip()

    from_date = str(df["date"].min().date())
    to_date = str(df["date"].max().date())
    annonces = fetch_annonces_daily(from_date, to_date)

    df = df.merge(annonces, how="left", on=["date", "region"])

    for c in ["annonce_count","annonce_price_mean","annonce_price_min","annonce_price_max","annonce_quality_mean","annonce_stock_sum"]:
        df[c] = pd.to_numeric(df[c], errors="coerce").fillna(0)

    return df
def rmse(y_true, y_pred):
    return float(np.sqrt(mean_squared_error(y_true, y_pred)))

def make_features(df: pd.DataFrame) -> pd.DataFrame:
    df["date"] = pd.to_datetime(df["date"], errors="coerce")
    df = df.dropna(subset=["date"]).copy()

    df["region"] = df["region"].astype(str).str.lower().str.strip()
    df["produit"] = df["produit"].astype(str).str.lower().str.strip()

    # keep only olive oil rows (be tolerant with labels)
    df = df[df["produit"].isin(["huile_olive", "huile d'olive", "huile d’olive", "huile_d_olive"])].copy()
    df["produit"] = "huile_olive"

    for col in ["qte_tonne", "px_min", "px_max", "px_moyen", "spread"]:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors="coerce")

    df = df.sort_values(["region", "date"]).reset_index(drop=True)

    # calendar features
    df["dow"] = df["date"].dt.dayofweek
    df["month"] = df["date"].dt.month
    df["weekofyear"] = df["date"].dt.isocalendar().week.astype(int)

    # lag & rolling features per region
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

    # one-hot region
    df = pd.get_dummies(df, columns=["region"], prefix="region", drop_first=False)

    return df

def train_for_horizon(df_feat: pd.DataFrame, horizon: int):
    df = df_feat.copy()

    region_cols = [c for c in df.columns if c.startswith("region_")]
    df["_region_key"] = df[region_cols].idxmax(axis=1)

    df = df.sort_values(["_region_key", "date"]).reset_index(drop=True)
    df["y"] = df.groupby("_region_key")["px_moyen"].shift(-horizon)

    # must have target + essential lags
    df = df.dropna(subset=["y", "px_moyen_lag_1", "px_moyen_lag_7"])

    max_date = df["date"].max()
    cutoff = max_date - pd.Timedelta(days=TEST_DAYS)

    train = df[df["date"] <= cutoff].copy()
    test = df[df["date"] > cutoff].copy()

    feature_cols = [c for c in df.columns if c not in ["y", "date", "produit", "source_dataset", "_region_key"]]
    X_train, y_train = train[feature_cols], train["y"]
    X_test, y_test = test[feature_cols], test["y"]

    model = XGBRegressor(
        n_estimators=800,
        learning_rate=0.03,
        max_depth=6,
        subsample=0.9,
        colsample_bytree=0.9,
        reg_lambda=1.0,
        objective="reg:squarederror",
        random_state=42
    )

    model.fit(X_train, y_train)

    preds = model.predict(X_test)
    mae = float(mean_absolute_error(y_test, preds))
    r = rmse(y_test, preds)

    return model, feature_cols, mae, r, cutoff, max_date, len(train), len(test)

def main():
    df_raw = pd.read_csv(CSV_PATH)
    df_raw = merge_annonces_signals(df_raw)   # NEW
    df_feat = make_features(df_raw)

    print("Rows after feature build:", len(df_feat))
    print("Date range:", df_feat["date"].min(), "->", df_feat["date"].max())

    for h in HORIZONS:
        model, feature_cols, mae, r, cutoff, max_date, n_train, n_test = train_for_horizon(df_feat, h)

        print(f"\n=== Horizon t+{h} ===")
        print("train rows:", n_train, "test rows:", n_test)
        print("test cutoff:", cutoff.date(), "max date:", max_date.date())
        print("MAE:", mae)
        print("RMSE:", r)

        model_path = f"xgb_olive_{h}d.json"
        model.get_booster().save_model(model_path)
        print("Saved:", model_path)

        feat_path = f"features_{h}d.txt"
        with open(feat_path, "w", encoding="utf-8") as f:
            for c in feature_cols:
                f.write(c + "\n")
        print("Saved:", feat_path)

if __name__ == "__main__":
    main()