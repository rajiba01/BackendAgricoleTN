import json
from collections import defaultdict
from pathlib import Path

FEEDBACK_FILE = Path("../olive_oil_merged_filtered.csv")

stats = defaultdict(lambda: {
    "shown": 0,
    "clicked": 0,
    "purchased": 0
})

with open(FEEDBACK_FILE, encoding="utf-8") as f:
    for line in f:
        e = json.loads(line)
        aid = e["annonce_id"]

        if e["event"] == "RECO_SHOWN":
            stats[aid]["shown"] += 1
        elif e["event"] == "ANNONCE_CLICKED":
            stats[aid]["clicked"] += 1
        elif e["event"] == "PURCHASED":
            stats[aid]["purchased"] += 1

conversion_rates = {
    aid: (v["purchased"] / v["shown"]) if v["shown"] else 0.0
    for aid, v in stats.items()
}

print("Conversion rates:")
for aid, rate in conversion_rates.items():
    print(f"Annonce {aid}: {rate:.2%}")
