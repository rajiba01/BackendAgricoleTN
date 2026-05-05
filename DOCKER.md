# Docker Configuration - Tunisian Economic Intelligence System

## 📋 Overview

Cette configuration Docker déploie l'ensemble de l'application:
- **Java Jetty API** (port 8080) - Backend REST avec Jersey/JAX-RS
- **Python FastAPI Chat** (port 8010) - Chat API avec support multilingue
- **Python ML API** (port 8000) - API prédictions/forecasting

## 🐳 Architecture

```
tunisian-network
├── java-api (Jetty 11)
│   ├── Port: 8080
│   ├── Database: Oracle (host.docker.internal:1521)
│   └── Endpoints: /api/annonces, /api/chat, etc.
│
├── fastapi-chat (FastAPI/Uvicorn)
│   ├── Port: 8010
│   ├── Depends on: java-api
│   └── Docs: /docs, /redoc
│
└── python-ml (Uvicorn)
    └── Port: 8000
```

## 📦 Prerequisites

- Docker installé (Docker Desktop sur Windows/macOS, Docker Engine sur Linux)
- Docker Compose v2.0+
- Oracle Database accessible à `host.docker.internal:1521/FREE` (Windows/macOS)
  - Sur Linux, `host.docker.internal` n'est pas toujours disponible: utilisez l'IP/hostname du serveur Oracle.

### Télécharger Docker Desktop (Windows/macOS)

- Lien officiel: https://www.docker.com/products/docker-desktop/

Après installation:

- **Windows**: activer WSL 2 (Docker Desktop vous le propose automatiquement).
- **macOS**: autoriser les extensions système si demandé.

### Vérifier l'installation

Ouvrir un terminal et vérifier:

```powershell
docker --version
docker compose version
```

> Note: selon la version, la commande peut être `docker compose` (recommandé) plutôt que `docker-compose`.

## 🚀 Quick Start

### Option 1: Script PowerShell automatisé
```powershell
.\docker-start.ps1
```

### Option 2: Commandes manuelles

**1. Construire les images:**
```bash
docker-compose build --no-cache
```

**2. Démarrer les services:**
```bash
docker-compose up -d
```

**3. Vérifier le statut:**
```bash
docker-compose ps
```

**4. Consulter les logs:**
```bash
docker-compose logs -f java-api      # Java API logs
docker-compose logs -f fastapi-chat  # Chat API logs
docker-compose logs -f python-ml     # ML API logs
```

**5. Arrêter les services:**
```bash
docker-compose down
```

## 🔧 Configuration

### Variables d'environnement (.env.docker)

```env
# Database
DB_URL=jdbc:oracle:thin:@//host.docker.internal:1521/FREE
DB_USER=SYSTEM
DB_PASSWORD=system

# Java
JAVA_OPTS=-Xms256m -Xmx512m

# Python
PYTHONUNBUFFERED=1

# Inter-service communication
JAVA_API_BASE=http://java-api:8080/api
```

### Modifier pour Production

1. **Sécurité:**
   - Changer `DB_PASSWORD` dans `.env.docker`
   - Utiliser secrets Docker: `docker secret create db_password -`
   - Mettre `restart_policy: on-failure` dans `docker-compose.yml`

2. **Performance:**
   - Augmenter `JAVA_OPTS`: `-Xms512m -Xmx2g`
   - Ajouter `resource limits` au compose:
     ```yaml
     resources:
       limits:
         cpus: '2'
         memory: 2G
     ```

3. **Database (Oracle sur Linux):**
   - Remplacer `host.docker.internal` par l'IP du serveur Oracle
   - Exemple: `DB_URL=jdbc:oracle:thin:@//192.168.1.100:1521/FREE`

## ✅ Tests de Santé

### 🌐 Liens navigateur (fonctionne sur toutes les machines)

Après `docker-compose up -d`, ouvrez:

- Java API: http://localhost:8080/
- Chat API: http://localhost:8010/
- Chat API (Swagger UI): http://localhost:8010/docs
- Python ML API: http://localhost:8000/

Si vous accédez depuis un autre PC sur le même réseau, remplacez `localhost` par l'IP de la machine qui exécute Docker.

### Java API
```bash
curl http://localhost:8080/api/annonces/by-type?type=HUILE
```

### Chat API
```bash
# Docs UI
http://localhost:8010/docs

# Health check
curl http://localhost:8010/health
```

### Logs complets
```bash
docker-compose logs --tail=50
```

## 🐛 Troubleshooting

### Docker Desktop ne démarre pas (Windows)

- Vérifier que la virtualisation est activée dans le BIOS
- Vérifier que WSL 2 fonctionne (PowerShell):
  - `wsl --status`
  - `wsl --update`

### `host.docker.internal` ne marche pas (Linux)

Sur Linux, remplacez l'URL Oracle dans `docker-compose.yml` (service `java-api`):

- Exemple: `jdbc:oracle:thin:@//192.168.1.100:1521/FREE`

Ensuite redémarrez:

```bash
docker-compose down
docker-compose up -d
```

### Port déjà utilisé
```bash
# Trouver quel processus utilise le port
netstat -ano | findstr :8080
# Tuer le processus
taskkill /PID <PID> /F
```

### Erreur de connexion base de données
```bash
docker-compose logs java-api | grep -i database
docker-compose logs java-api | grep -i connection
```

### Network issues entre conteneurs
```bash
# Vérifier le réseau Docker
docker network ls
docker network inspect tunisian-network

# Re-créer le réseau
docker-compose down
docker network prune
docker-compose up -d
```

### Rebuild sans cache
```bash
docker-compose build --no-cache --pull
docker-compose up -d
```

## 📊 Monitoring

### Resource usage
```bash
docker stats
```

### Container details
```bash
docker inspect java-api
docker inspect fastapi-chat
```

## 🔄 CI/CD Integration

Pour intégrer avec GitHub Actions, créer `.github/workflows/docker-build.yml`:

```yaml
name: Build and Push Docker Images
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: docker/setup-buildx-action@v1
      - uses: docker/build-push-action@v2
        with:
          context: .
          file: ./Dockerfile.java
          tags: waelbenrejiba/java-api:latest
```

## 📚 Ressources

- [Docker Documentation](https://docs.docker.com)
- [Docker Compose Reference](https://docs.docker.com/compose/compose-file/)
- [Jetty Documentation](https://www.eclipse.org/jetty/)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)

---

**Last Updated:** January 3, 2026  
**Maintained by:** Wael Ben Rejiba
