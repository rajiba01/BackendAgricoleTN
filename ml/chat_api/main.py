
from __future__ import annotations

import datetime as _dt
import os
import re
from typing import Any, Dict, List, Literal, Optional, Tuple
import json
from pathlib import Path
import httpx
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from fastapi import UploadFile, File
from PIL import Image
import io
import pytesseract
pytesseract.pytesseract.tesseract_cmd = "C:\\Program Files\\Tesseract-OCR\\tesseract.exe"
import cv2
import numpy as np
from learning.sell_advice import get_sell_advice
ML_API_BASE = os.getenv("ML_API_BASE", "http://127.0.0.1:8000")
OLLAMA_BASE = os.getenv("OLLAMA_BASE", "http://127.0.0.1:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5:7b-instruct")

JAVA_API_BASE = os.getenv("JAVA_API_BASE", "http://127.0.0.1:8080/api")
Role = Literal["user", "assistant"]


class ChatMessage(BaseModel):
    role: Role
    content: str


class ChatRequest(BaseModel):
    messages: List[ChatMessage] = Field(min_length=1)
    langMode: Literal["auto", "fr", "ar", "en", "tn"] = "auto"
    context: Dict[str, Any] = Field(default_factory=dict)  # Optional context for marketplace mode


class ChatResponse(BaseModel):
    reply: str
    languageHint: Optional[Literal["fr", "ar", "en", "tn"]] = None


class ImageAnalysisResponse(BaseModel):
    score: int = Field(ge=0, le=100)
    verdict: Literal["HAUTE", "MOYENNE", "FAIBLE"]
    languageHint: Optional[Literal["fr", "ar", "en", "tn"]] = None
    extracted_text: str
    checklist: Dict[str, Any]
    advice: str


app = FastAPI(title="Chat API", version="1.0")



app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ---- simple parsing helpers ----

REGION_ALIASES = {
    "sfax": "sfax",
    "sfax.": "sfax",
    "صفاقس": "sfax",
    "nord": "nord",
    "الشمال": "nord",
    "sahel": "sahel",
    "الساحل": "sahel",
    "centre": "centre",
    "الوسط": "centre",
    "sud": "sud",
    "الجنوب": "sud",
}

def detect_language(text: str) -> Literal["fr", "ar", "en", "tn"]:
    # Heuristic: Arabic letters => ar/tn
    if re.search(r"[\u0600-\u06FF]", text):
        return "ar"
    # English keywords
    if re.search(r"\b(buy|wait|price|today|region)\b", text, re.IGNORECASE):
        return "en"
    return "fr"

def extract_region(text: str) -> Optional[str]:
    t = text.lower().strip()
    for k, v in REGION_ALIASES.items():
        if k.lower() in t:
            return v
    return None

def extract_horizon(text: str) -> Optional[int]:
    # look for 7 / 30 with words
    m = re.search(r"\b(7|30)\b", text)
    if m:
        return int(m.group(1))
    if re.search(r"\b(semaine|week)\b", text, re.IGNORECASE):
        return 7
    if re.search(r"\b(mois|month)\b", text, re.IGNORECASE):
        return 30
    return None

def extract_price(text: str) -> Optional[float]:
    # Accept "11.8", "11,8", "12 dt", "12 دينار"
    m = re.search(r"(\d{1,2}(?:[.,]\d{1,2})?)\s*(dt|dinar|دينار)?", text, re.IGNORECASE)
    if not m:
        return None
    val = m.group(1).replace(",", ".")
    try:
        price = float(val)
    except:
        return None
    # sanity range
    if price <= 0 or price > 100:
        return None
    return price

def latest_user_text(req: ChatRequest) -> str:
    # take last user message
    for m in reversed(req.messages):
        if m.role == "user":
            return m.content
    return req.messages[-1].content


def ocr_image(img: Image.Image) -> str:
    """Extract text from an image using Tesseract.

    Keeps it simple and robust: returns empty string on failure.
    """
    try:
        # Basic preprocessing helps OCR quite a bit.
        arr = np.array(img)
        gray = cv2.cvtColor(arr, cv2.COLOR_RGB2GRAY)
        gray = cv2.bilateralFilter(gray, 9, 75, 75)
        thr = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1]
        config = "--oem 3 --psm 6"
        text = pytesseract.image_to_string(thr, lang="ara+fra+eng", config=config)
        return text.strip()
    except Exception:
        return ""


async def call_predict_calibrated(region: str, horizon: int, current_price: float) -> Dict[str, Any]:
    url = f"{ML_API_BASE}/predict_calibrated"
    params = {"region": region, "horizon": horizon, "current_price": current_price, "stable_pct": 1.0}
    async with httpx.AsyncClient(timeout=20) as client:
        r = await client.get(url, params=params)
        if r.status_code != 200:
            raise HTTPException(status_code=r.status_code, detail=r.text)
        return r.json()


async def ollama_chat(system: str, user: str) -> str:
    """
    Minimal Ollama chat call.
    """
    payload = {
        "model": OLLAMA_MODEL,
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": user},
        ],
        "stream": False,
        "options": {
            "temperature": 0.4,
            "top_p": 0.9,
        },
    }
    async with httpx.AsyncClient(timeout=60) as client:
        r = await client.post(f"{OLLAMA_BASE}/api/chat", json=payload)
        r.raise_for_status()
        data = r.json()
        return data["message"]["content"]


def build_system_prompt(lang: Literal["fr", "ar", "en", "tn"]) -> str:
    if lang == "fr":
        return (
            "Tu es un assistant d'achat pour l'huile d'olive en Tunisie. "
            "Tu dois être clair, poli, et orienté conversion, mais honnête. "
            "Tu expliques en 3-6 phrases maximum. "
            "Tu donnes une recommandation finale: Acheter maintenant / Attendre / Surveiller. "
            "N'invente jamais des chiffres: utilise uniquement les valeurs fournies."
            "Ajoute aussi une recommandation personnalisée pour un fournisseur si cette information est fournie."
        )
    if lang == "en":
        return (
            "You are a shopping assistant for olive oil in Tunisia. "
            "Be concise, honest, and conversion-oriented. "
            "Give a final recommendation: Buy now / Wait / Monitor. "
            "Never invent numbers; use only the provided values."
            "Also provide a personalized recommendation for a supplier if that information is given."
        )
    # ar/tn
    return (
        "أنت مساعد شراء لزيت الزيتون في تونس. "
        "كن واضحًا ومختصرًا وصادقًا. "
        "أعطِ توصية نهائية: اشري توّا / استنّى / راقب. "
        "لا تخترع أرقامًا: استعمل فقط الأرقام المعطاة."
        "أضف أيضًا توصية مخصصة لمورد إذا تم توفير هذه المعلومة."
    )


def format_user_prompt(lang: str, region: str, horizon: int, current_price: float, ml: Dict[str, Any]) -> str:
    # ml returns: predicted_calibrated, pct_vs_current, advice_label, dataset_baseline_mean7, etc.
    predicted = ml.get("predicted_calibrated")
    pct = ml.get("pct_vs_current")
    advice_label = ml.get("advice_label")
    mean7 = ml.get("dataset_baseline_mean7")
    asof_stats = ml.get("asof_date_stats")
    fournisseur = ml.get("fournisseur")

    if lang == "en":
        return (
            f"Customer context:\n"
            f"- Region: {region}\n- Today price: {current_price:.2f} DT/L\n- Horizon: {horizon} days\n"
            f"ML facts (use exactly):\n"
            f"- 7-point mean (dataset): {mean7:.2f}\n- Dataset last date: {asof_stats}\n"
            f"- Calibrated forecast: {predicted:.2f} DT/L\n- Change vs today: {pct:.2f}%\n"
            f"- Recommendation label: {advice_label}\n\n"
            f"Write the best short advice to the customer."
        )

    if lang == "fr":
        return (
            f"Contexte client:\n"
            f"- Région: {region}\n- Prix aujourd'hui: {current_price:.2f} DT/L\n- Horizon: {horizon} jours\n"
            f"Faits ML (utilise exactement):\n"
            f"- Moyenne 7 points (dataset): {mean7:.2f}\n- Dernière date dataset: {asof_stats}\n"
            f"- Prévision calibrée: {predicted:.2f} DT/L\n- Variation vs aujourd'hui: {pct:.2f}%\n"
            f"- Libellé recommandation: {advice_label}\n\n"
            f"Rédige un conseil court et convaincant au client."
        )

    # Arabic / Darija (we keep Arabic script; Darija tone can be added later)
    return (
        f"معلومات الحريف:\n"
        f"- الجهة: {region}\n- سعر اليوم: {current_price:.2f} دينار/لتر\n- الأفق: {horizon} يوم\n"
        f"معطيات من النموذج (استعملها كما هي):\n"
        f"- معدل آخر 7 نقاط (من الداتا): {mean7:.2f}\n- آخر تاريخ في الداتا: {asof_stats}\n"
        f"- التوقع بعد المعايرة: {predicted:.2f} دينار/لتر\n- التغيير مقارنة باليوم: {pct:.2f}%\n"
        f"- التوصية: {advice_label}\n\n"
        f"اكتب نصيحة قصيرة للحريف."
    )


@app.get("/health")
def health():
    return {"status": "ok", "ollama_model": OLLAMA_MODEL, "ml_api_base": ML_API_BASE}
@app.get("/advice/sell")
def sell_advice(region: str):
    advice = get_sell_advice(region.lower())
    if not advice:
        raise HTTPException(
            status_code=400,
            detail="داتا غير كافية للجهة هاذي"
        )
    return advice
@app.get("/stats/top_products")
async def top_products():
    top = await fetch_top_products_from_java()
    return {"top_products": top}

@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    text = latest_user_text(req)

    # 1️⃣ Détecter la langue
    lang: Literal["fr", "ar", "en", "tn"]
    if req.langMode == "auto":
        lang = detect_language(text)
    else:
        lang = "ar" if req.langMode == "tn" else req.langMode  # type: ignore
    supplier_msg = await handle_supplier_question(text)
    if supplier_msg:
        return ChatResponse(reply=supplier_msg, languageHint="fr" if lang=="fr" else "ar")

    # 2️⃣ Vérifier si c'est une question qualité
    if is_quality_question(text):
        system = build_system_prompt(lang)
        user_prompt = build_quality_prompt_fr(user_text=text)
        reply = await ollama_chat(system=system, user=user_prompt)
        language_hint: Optional[Literal["fr", "ar", "en", "tn"]] = "fr" if lang == "fr" else "en" if lang == "en" else "ar"
        return ChatResponse(reply=reply, languageHint=language_hint)

    # 3️⃣ Vérifier si c'est une question marketplace / recommandation
    ctx_type = (req.context.get("type") if isinstance(req.context, dict) else None)
    if ctx_type and is_reco_intent(text):
        type_ = str(ctx_type).upper().strip()
        budget = extract_price(text)

        annonces = await fetch_annonces_by_type(type_)

        scored = []
        for a in annonces:
            uid = a.get("userId")
            trust = await fetch_trust(int(uid)) if uid is not None else {"trustScore": 0}
            s = score_annonce(a, trust, budget=budget)
            scored.append({
                "id": a.get("id"),
                "titre": a.get("titre"),
                "prixVente": a.get("prixVente"),
                "qtyOnHand": a.get("qtyOnHand"),
                "qualiteScore": a.get("qualiteScore"),
                "qualiteVerdict": a.get("qualiteVerdict"),
                "userId": uid,
                "trustScore": trust.get("trustScore"),
                "deliveredRate": trust.get("deliveredRate"),
                "scoring": s
            })

        scored.sort(key=lambda x: x["scoring"]["score"], reverse=True)
        top = scored[:3]

        system = build_market_system_prompt(lang)
        user_prompt = build_market_user_prompt(lang=lang, user_text=text, annonces_ranked=top)

        # 4️⃣ Ajouter les top produits les plus demandés
        supplier_msg = await handle_supplier_question(text)
        if supplier_msg:
            user_prompt += "\n\nInfos produits les plus demandés:\n" + supplier_msg

        reply = await ollama_chat(system=system, user=user_prompt)
        language_hint: Optional[Literal["fr", "ar", "en", "tn"]] = "fr" if lang == "fr" else "en" if lang == "en" else "ar"
        return ChatResponse(reply=reply, languageHint=language_hint)

    # 5️⃣ Mode forecast / prédiction
    region = extract_region(text) or "sfax"
    horizon = extract_horizon(text) or 7
    price = extract_price(text)

    if price is None:
        # Demande le prix si absent
        if lang == "en":
            return ChatResponse(reply="What is the price today (DT/L)? Example: 11.8", languageHint="en")
        if lang == "fr":
            return ChatResponse(reply="Quel est le prix aujourd’hui (DT/L) ? Exemple: 11.8", languageHint="fr")
        return ChatResponse(reply="قدّاش سعر اليوم (دينار/لتر)؟ مثال: 11.8", languageHint="ar")

    ml = await call_predict_calibrated(region=region, horizon=horizon, current_price=price)

    system = build_system_prompt(lang)
    user_prompt = format_user_prompt(lang, region, horizon, price, ml)

    # 6️⃣ Ajouter les top produits les plus demandés ici aussi
    supplier_msg = await handle_supplier_question(text)
    if supplier_msg:
        user_prompt += "\n\nInfos produits les plus demandés:\n" + supplier_msg

    reply = await ollama_chat(system=system, user=user_prompt)
    language_hint: Optional[Literal["fr", "ar", "en", "tn"]] = "fr" if lang == "fr" else "en" if lang == "en" else "ar"
    return ChatResponse(reply=reply, languageHint=language_hint)



@app.post("/analyze_image", response_model=ImageAnalysisResponse)
async def analyze_image(file: UploadFile = File(...), langMode: str = "auto"):
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="Veuillez envoyer une image.")

    raw = await file.read()

    # Robust image decode: PIL first, fallback to OpenCV (helps for some JPG/WEBP cases)
    try:
        img = Image.open(io.BytesIO(raw)).convert("RGB")
    except Exception:
        try:
            arr = np.frombuffer(raw, np.uint8)
            cv_img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
            if cv_img is None:
                raise ValueError("cv2.imdecode failed")
            cv_img = cv2.cvtColor(cv_img, cv2.COLOR_BGR2RGB)
            img = Image.fromarray(cv_img)
        except Exception:
            raise HTTPException(status_code=400, detail="Image invalide ou format non supporté (essayez JPG/PNG).")

    extracted = ocr_image(img)
    score, checklist = heuristic_quality_score(extracted)

    # Choose language
    if langMode == "auto":
        lang = detect_language(extracted) if extracted.strip() else "fr"
    else:
        lang = "ar" if langMode == "tn" else langMode

    language_hint: Optional[Literal["fr", "ar", "en", "tn"]] = "fr" if lang == "fr" else "en" if lang == "en" else "ar"

    # If OCR too short: do NOT ask the LLM to guess
    if len(extracted.strip()) < 20:
        return ImageAnalysisResponse(
            score=35,
            verdict="FAIBLE",
            languageHint=language_hint,
            extracted_text=extracted,
            checklist={
                "ok": [],
                "missing": ["Texte étiquette illisible (OCR vide ou trop court)"],
                "note": "Je ne peux pas juger la qualité sans infos lisibles sur l’étiquette."
            },
            advice=(
                "Je ne peux pas confirmer la qualité à partir de cette photo (étiquette illisible).\n"
                "Envoie:\n"
                "1) Photo très proche de l’étiquette (face avant)\n"
                "2) Photo du dos (lot + date)\n"
                "3) Photo de la bouteille entière (verre sombre/plastique)\n"
            ),
        )

    # Normal case: build marketing + quality explanation using knowledge base
    system = build_system_prompt(lang if lang in ["fr", "en", "ar"] else "fr")
    user_prompt = build_quality_prompt_fr(
        user_text="L'utilisateur a envoyé une photo pour évaluer la qualité.",
        extracted_ocr=extracted,
        checklist=checklist,
    )

    try:
        advice = await ollama_chat(system=system, user=user_prompt)
    except Exception:
        advice = "Analyse faite, mais le service IA n'est pas disponible pour générer une explication détaillée."

    verdict = "HAUTE" if score >= 75 else "MOYENNE" if score >= 55 else "FAIBLE"

    return ImageAnalysisResponse(
        score=score,
        verdict=verdict,
        languageHint=language_hint,
        extracted_text=extracted,
        checklist=checklist,
        advice=advice,
    )
KNOWLEDGE_PATH = Path(__file__).parent / "knowledge" / "quality_faq.json"

def load_quality_knowledge() -> Dict[str, Any]:
    with open(KNOWLEDGE_PATH, "r", encoding="utf-8") as f:
        return json.load(f)

def retrieve_faq_snippets(user_text: str, k: int = 4) -> List[Dict[str, Any]]:
    kb = load_quality_knowledge()
    faq = kb.get("faq_fr", [])
    t = user_text.lower()

    scored: List[tuple[int, Dict[str, Any]]] = []
    for item in faq:
        tags = item.get("tags", [])
        score = 0
        for tag in tags:
            if tag.lower() in t:
                score += 2
        # also match question words
        if any(w in t for w in (item.get("q","").lower().split())):
            score += 1
        if score > 0:
            scored.append((score, item))

    scored.sort(key=lambda x: x[0], reverse=True)
    return [x[1] for x in scored[:k]]

def build_quality_prompt_fr(user_text: str, extracted_ocr: Optional[str] = None, checklist: Optional[Dict[str, Any]] = None) -> str:
    kb = load_quality_knowledge()
    disclaimer = kb.get("disclaimer_fr", "")
    rules = kb.get("rules_fr", [])
    snippets = retrieve_faq_snippets(user_text, k=4)

    parts = []
    parts.append(f"DISCLAMER (à reformuler et garder): {disclaimer}")
    parts.append("Règle anti-invention: si une information n’apparaît pas dans 'Texte OCR' ou 'Checklist', écris 'Non visible sur la photo'.")
    parts.append("Règles qualité (à utiliser comme base):\n- " + "\n- ".join(rules))

    if extracted_ocr is not None:
        parts.append(f"Texte OCR étiquette:\n{extracted_ocr if extracted_ocr.strip() else '[VIDE]'}")
    if checklist is not None:
        parts.append(f"Checklist JSON:\n{checklist}")

    if snippets:
        parts.append("Extraits FAQ pertinents:")
        for s in snippets:
            parts.append(f"Q: {s.get('q')}\nA: {s.get('a')}")
    else:
        parts.append("Aucun extrait FAQ fortement pertinent trouvé; répondre avec les règles générales.")

    parts.append(f"Question utilisateur:\n{user_text}")

    parts.append(
    "Tu es un conseiller qualité + marketing pour huile d’olive en Tunisie.\n"
    "Objectif: aider le client à acheter en confiance.\n"
    "Contraintes:\n"
    "- Ne mens jamais: si une info n’est pas visible, dis-le.\n"
    "- Une photo/étiquette ne prouve pas la qualité chimique.\n"
    "- Ne donne pas de chiffres inventés.\n\n"
    "Format de réponse OBLIGATOIRE (avec titres):\n"
    "Verdict: <HAUTE/MOYENNE/FAIBLE> — <score>/100\n"
    "Points forts:\n- ...\n"
    "Points manquants:\n- ...\n"
    "Questions au vendeur:\n1) ...\n2) ...\n3) ...\n"
    "Conseil d’achat: <Acheter maintenant / Demander infos / Éviter>\n"
    "Conseils de conservation:\n- ...\n- ...\n"
    "Réponds dans la langue de l’utilisateur (FR/AR/Darija/EN)."
)
    return "\n\n".join(parts)
def parse_label_fields(extracted: str) -> Dict[str, Any]:
    t = extracted.lower()

    def has_any(*phrases: str) -> bool:
        return any(p.lower() in t for p in phrases)

    category = (
        "extra_virgin" if has_any("extra vierge", "extra virgin", "بكر ممتاز")
        else "virgin" if has_any("vierge", "virgin", "بكر")
        else "unknown"
    )

    # Dates (simple heuristics)
    years = sorted(set(re.findall(r"\b20\d{2}\b", extracted)))
    year_hint = years[-1] if years else None

    lot = None
    m = re.search(r"(lot|batch)\s*[:#]?\s*([a-z0-9\-\/]{3,})", t, re.IGNORECASE)
    if m:
        lot = m.group(2)

    origin = None
    if has_any("tunisie", "tunisia", "تونس"):
        origin = "Tunisie"
    # crude region detection
    reg = extract_region(extracted)
    if reg:
        origin = f"Tunisie - {reg}"

    acidity = None
    m2 = re.search(r"(acidité|acidity|حموضة)\s*[:=]?\s*(\d+(?:[.,]\d+)?)\s*%?", t, re.IGNORECASE)
    if m2:
        try:
            acidity = float(m2.group(2).replace(",", "."))
        except Exception:
            acidity = None

    fields = {
        "category": category,
        "origin": origin,
        "lot": lot,
        "year_hint": year_hint,
        "acidity": acidity,
        "raw_years_found": years,
    }
    return fields
def heuristic_quality_score(extracted: str) -> Tuple[int, Dict[str, Any]]:
    fields = parse_label_fields(extracted)

    has_extra = fields["category"] == "extra_virgin"
    has_origin = bool(fields["origin"])
    has_lot = bool(fields["lot"])
    has_date = bool(fields["year_hint"])  # simple for now
    has_acidity = fields["acidity"] is not None

    score = 35
    if has_extra: score += 20
    if has_origin: score += 10
    if has_lot: score += 10
    if has_date: score += 15
    if has_acidity: score += 10
    score = max(0, min(95, score))

    checklist = {
        "label_mentions_extra_virgin": has_extra,
        "origin_or_traceability_present": has_origin,
        "lot_batch_present": has_lot,
        "date_or_harvest_present": has_date,
        "acidity_mentioned": has_acidity,
        "parsed_fields": fields,
        "note": "Analyse basée sur étiquette + indices. La photo ne prouve pas la qualité chimique."
    }
    return score, checklist
QUALITY_KEYWORDS = [
    "qualité", "quality", "bonne", "meilleure", "mauvaise",
    "extra vierge", "extra-vierge", "extra virgin", "vierge", "virgin",
    "acidité", "acidity", "polyphénol", "polyphenol",
    "bouteille", "verre", "plastique", "bidon", "emballage", "packaging",
    "récolte", "harvest", "mise en bouteille", "bottling",
    "lot", "batch", "origine", "origin", "trace", "traçabilité",
    "authentique", "mélange", "adultération", "conserver", "conservation",
    "huile d'olive", "huile olive", "زيت", "زيتون"
]

def is_quality_question(text: str) -> bool:
    t = text.lower()
    return any(k in t for k in QUALITY_KEYWORDS)
async def fetch_annonces_by_type(type_: str) -> list[dict]:
    async with httpx.AsyncClient(timeout=20) as client:
        r = await client.get(f"{JAVA_API_BASE}/annonces/by-type", params={"type": type_})
        r.raise_for_status()
        return r.json()

async def fetch_trust(user_id: int) -> dict:
    async with httpx.AsyncClient(timeout=20) as client:
        r = await client.get(f"{JAVA_API_BASE}/public/fournisseurs/{user_id}/trust")
        if r.status_code != 200:
            return {"trustScore": 0, "deliveredRate": 0.0, "total": 0, "delivered": 0}
        return r.json()


RECO_KEYWORDS = [
    "conseille", "recommande", "meilleur", "meilleure", "acheter", "achète",
    "fiable", "sûr", "qualité", "top", "moins cher", "budget", "price", "best"
]

def is_reco_intent(text: str) -> bool:
    t = text.lower()
    return any(k in t for k in RECO_KEYWORDS)
def score_annonce(a: dict, trust: dict, budget: float | None = None) -> dict:
    prix = float(a.get("prixVente") or 0)
    stock = a.get("qtyOnHand")
    stock_ok = (stock is None) or (int(stock) > 0)

    qual = a.get("qualiteScore")
    qual_score = float(qual) if qual is not None else 50.0  # fallback

    trust_score = float(trust.get("trustScore") or 0)

    # price component (cheap is better but don't overweight)
    price_score = max(0.0, 100.0 - prix * 3.0)  # heuristic

    # budget penalty
    budget_penalty = 0.0
    if budget is not None and prix > budget:
        budget_penalty = min(40.0, (prix - budget) * 10.0)

    total = (
        0.45 * qual_score +
        0.35 * trust_score +
        0.20 * price_score
    )
    if not stock_ok:
        total -= 30.0
    total -= budget_penalty

    return {
        "score": round(max(0.0, min(100.0, total)), 1),
        "qualScore": qual_score,
        "trustScore": trust_score,
        "priceScore": round(price_score, 1),
        "budgetPenalty": round(budget_penalty, 1),
        "stockOk": stock_ok
    }
def build_market_system_prompt(lang: Literal["fr", "ar", "en", "tn"]) -> str:
    base = build_system_prompt(lang)
    return base + "\n" + (
      "Tu es aussi un conseiller marketplace.\n"
      "Tu dois recommander des annonces en te basant UNIQUEMENT sur les données 'ANNONCES'.\n"
      "N'invente jamais de certifications ni d'indices chimiques si absents.\n"
      "Tu dois donner un TOP 3 + raisons + 3 questions à poser au vendeur.\n"
      "Si budget/quantité/region manquent et sont nécessaires, pose UNE seule question courte."
    )

def build_market_user_prompt(lang: str, user_text: str, annonces_ranked: list[dict]) -> str:
    return (
      "ANNONCES (JSON, facts only):\n"
      f"{json.dumps(annonces_ranked, ensure_ascii=False)}\n\n"
      "User question:\n"
      f"{user_text}\n\n"
      "FORMAT OBLIGATOIRE:\n"
      "Top 3 annonces:\n"
      "1) ... (prix, stock, score, pourquoi)\n"
      "2) ...\n"
      "3) ...\n"
      "Pourquoi ces choix:\n- ...\n"
      "Questions au vendeur:\n1) ...\n2) ...\n3) ...\n"
      "Conseil final: Acheter maintenant / Demander infos / Éviter\n"
      "Réponds dans la langue de l'utilisateur."
    )
PRODUCTS = ["huile d'olive", "dattes", "bananes", "pêches"]

async def fetch_top_products_from_java(limit=5):
    url = f"http://127.0.0.1:8080/api/commandes/top-products?limit={limit}"
    async with httpx.AsyncClient(timeout=10) as client:
        r = await client.get(url)
        r.raise_for_status()
        return r.json()

def extract_product_keywords(text):
    found = []
    for prod in PRODUCTS:
        if prod.lower() in text.lower():
            found.append(prod)
    return found

async def handle_supplier_question(user_message):
    # Detect if the question is about most demanded products
    keywords = ["demande", "demandé", "plus demandé", "plus demandés", "top produits", "produits populaires", "most demanded", "top products", "popular products","akther heja matlouba"]
    if any(k in user_message.lower() for k in keywords):
        try:
            top_products = await fetch_top_products_from_java()
            if top_products:
                produits_list = ", ".join([p.get('produitType', '') for p in top_products])
                return f"Actuellement, les produits les plus demandés sont : {produits_list}."
            else:
                return "Aucune donnée de demande produit n'est disponible actuellement."
        except Exception:
            return "Impossible de récupérer les statistiques de demande pour le moment."
        
