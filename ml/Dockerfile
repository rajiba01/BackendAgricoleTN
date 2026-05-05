FROM python:3.11-slim
WORKDIR /app

# Needed for OpenCV (cv2) on slim images
RUN apt-get update \
	&& apt-get install -y --no-install-recommends libgl1 libglib2.0-0 \
	&& rm -rf /var/lib/apt/lists/*

# Copier requirements depuis le bon répertoire
COPY chat_api/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copier le code
COPY chat_api/ .

ENV PORT=8010
EXPOSE 8010

# main.py est le bon point d'entrée
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port 8010"]