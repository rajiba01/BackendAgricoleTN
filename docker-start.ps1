# Docker startup script for Tunisian Economic Intelligence System

## Prerequisites Check
echo "Checking Docker installation..."
docker --version
docker-compose --version

echo ""
echo "========================================="
echo "Tunisian Economic Intelligence System"
echo "Docker Deployment"
echo "========================================="
echo ""

## Build images
echo "Building Docker images..."
docker-compose build --no-cache

echo ""
echo "Starting services..."
docker-compose up -d

echo ""
echo "Waiting for services to start..."
Start-Sleep -Seconds 5

## Check service health
echo ""
echo "Service Status:"
echo "==============="
docker-compose ps

echo ""
echo "Checking API connectivity..."
echo ""

# Check Java API
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/annonces/by-type?type=HUILE" -UseBasicParsing -ErrorAction Stop
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ Java API (port 8080): RUNNING" -ForegroundColor Green
    }
} catch {
    Write-Host "✗ Java API (port 8080): FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# Check Chat API
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8010/docs" -UseBasicParsing -ErrorAction Stop
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ Chat API (port 8010): RUNNING" -ForegroundColor Green
    }
} catch {
    Write-Host "✗ Chat API (port 8010): FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

echo ""
echo "========================================="
echo "Docker Environment Ready!"
echo "========================================="
echo ""
echo "Services available at:"
echo "  - Java API:     http://localhost:8080"
echo "  - Chat API:     http://localhost:8010"
echo "  - Chat API Docs: http://localhost:8010/docs"
echo ""
echo "View logs: docker-compose logs -f"
echo "Stop services: docker-compose down"
