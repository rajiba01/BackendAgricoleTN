import pandas as pd
from datetime import timedelta

df = pd.read_csv("ml/olive_oil_merged_filtered.csv", parse_dates=["date"])

def get_sell_advice(region: str):
    sub = df[df["region"] == region].sort_values("date")

    if len(sub) < 30:
        return None

    latest = sub.iloc[-1]
    last_price = latest["px_moyen"]

    # trend: compare moyenne récente
    mean_7 = sub.tail(7)["px_moyen"].mean()
    mean_30 = sub.tail(30)["px_moyen"].mean()

    if mean_7 > mean_30 * 1.02:
        trend = "UP"
        advice = "WAIT"
    elif mean_7 < mean_30 * 0.98:
        trend = "DOWN"
        advice = "SELL_NOW"
    else:
        trend = "STABLE"
        advice = "MONITOR"

    # prix conseillé
    min_price = round(mean_7 * 0.98, 2)
    max_price = round(mean_7 * 1.02, 2)

    # timing
    if advice == "WAIT":
        timing = "استنّى 2 إلى 4 أسابيع"
    elif advice == "SELL_NOW":
        timing = "بيع توّا"
    else:
        timing = "راقب السوق أسبوعين"

    return {
        "region": region,
        "current_market_price": round(last_price, 2),
        "trend": trend,
        "recommended_price_range": [min_price, max_price],
        "timing": timing,
        "decision": advice
    }
