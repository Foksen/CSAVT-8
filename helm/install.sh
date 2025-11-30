#!/bin/bash

set -e

echo "Starting Shop Microservices installation in Minikube..."

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

# Check if minikube is running
if ! minikube status | grep -q "Running"; then
    echo "Minikube is not running. Please start it first:"
    echo "   minikube start --cpus=4 --memory=6144 --driver=docker"
    exit 1
fi

# Create namespace if not exists
kubectl create namespace shop-system --dry-run=client -o yaml | kubectl apply -f -

# Install infrastructure components
echo -e "${BLUE}Installing PostgreSQL (4 databases)...${NC}"
helm upgrade --install postgresql ./shop-system/charts/postgresql -n shop-system --create-namespace

echo -e "${BLUE}Installing Redis...${NC}"
helm upgrade --install redis ./shop-system/charts/redis -n shop-system

echo -e "${BLUE}Installing Kafka...${NC}"
helm upgrade --install kafka ./shop-system/charts/kafka -n shop-system

# Wait for infrastructure to be ready
echo -e "${BLUE}Waiting for infrastructure to be ready (this may take 2-3 minutes)...${NC}"
sleep 10

echo "Waiting for Redis..."
kubectl wait --for=condition=ready pod -l app=redis -n shop-system --timeout=300s 2>/dev/null || echo "Redis pods not ready yet, continuing..."

echo "Waiting for Kafka and Zookeeper..."
kubectl wait --for=condition=ready pod -l app=kafka -n shop-system --timeout=300s 2>/dev/null || echo "Kafka pods not ready yet, continuing..."
kubectl wait --for=condition=ready pod -l app=zookeeper -n shop-system --timeout=300s 2>/dev/null || echo "Zookeeper pods not ready yet, continuing..."

echo "Waiting for PostgreSQL databases..."
kubectl wait --for=condition=ready pod -l app=auth-db-postgresql -n shop-system --timeout=300s 2>/dev/null || echo "Auth DB not ready yet, continuing..."
kubectl wait --for=condition=ready pod -l app=product-db-postgresql -n shop-system --timeout=300s 2>/dev/null || echo "Product DB not ready yet, continuing..."
kubectl wait --for=condition=ready pod -l app=customer-db-postgresql -n shop-system --timeout=300s 2>/dev/null || echo "Customer DB not ready yet, continuing..."
kubectl wait --for=condition=ready pod -l app=order-db-postgresql -n shop-system --timeout=300s 2>/dev/null || echo "Order DB not ready yet, continuing..."

echo -e "${GREEN}Infrastructure pods are starting up...${NC}"

# Build Docker images for Minikube
echo -e "${BLUE}Building Docker images...${NC}"
eval $(minikube docker-env --shell bash)
cd ..

docker build -t shop-auth-service:latest --build-arg SERVICE_NAME=infra/auth-service -f Dockerfile .
docker build -t shop-product-service:latest --build-arg SERVICE_NAME=service/product-service -f Dockerfile .
docker build -t shop-customer-service:latest --build-arg SERVICE_NAME=service/customer-service -f Dockerfile .
docker build -t shop-order-service:latest --build-arg SERVICE_NAME=service/order-service -f Dockerfile .

cd helm

# Install microservices
echo -e "${BLUE}Installing Auth Service...${NC}"
helm upgrade --install auth-service ./shop-system/charts/auth-service -n shop-system

echo -e "${BLUE}Installing Product Service...${NC}"
helm upgrade --install product-service ./shop-system/charts/product-service -n shop-system

echo -e "${BLUE}Installing Customer Service...${NC}"
helm upgrade --install customer-service ./shop-system/charts/customer-service -n shop-system

echo -e "${BLUE}Installing Order Service...${NC}"
helm upgrade --install order-service ./shop-system/charts/order-service -n shop-system

# Install observability stack
echo -e "${BLUE}Installing Graylog Service...${NC}"
helm upgrade --install graylog ./shop-system/charts/graylog -n shop-system

echo -e "${BLUE}Installing Jaeger...${NC}"
helm upgrade --install jaeger ./shop-system/charts/jaeger -n shop-system

echo -e "${BLUE}Installing Prometheus...${NC}"
helm upgrade --install prometheus ./shop-system/charts/prometheus -n shop-system

echo -e "${BLUE}Installing Grafana...${NC}"
helm upgrade --install grafana ./shop-system/charts/grafana -n shop-system

# Install KrakenD API Gateway
echo -e "${BLUE}Installing KrakenD API Gateway...${NC}"
helm upgrade --install krakend ./shop-system/charts/krakend -n shop-system

echo -e "${GREEN}Installation complete!${NC}"
echo ""
echo "Waiting for all pods to be ready..."
sleep 20
kubectl get pods -n shop-system
echo ""

echo -e "${BLUE}Starting port forwarding...${NC}"
pkill -f "kubectl port-forward" 2>/dev/null || true
sleep 2

kubectl port-forward -n shop-system svc/krakend 8080:8080 > /tmp/krakend-pf.log 2>&1 &
kubectl port-forward -n shop-system svc/auth-service 9000:9000 > /tmp/auth-pf.log 2>&1 &
kubectl port-forward -n shop-system svc/grafana 3000:3000 > /tmp/grafana-pf.log 2>&1 &
kubectl port-forward -n shop-system svc/jaeger-ui 16686:16686 > /tmp/jaeger-pf.log 2>&1 &
kubectl port-forward -n shop-system svc/prometheus 9090:9090 > /tmp/prometheus-pf.log 2>&1 &

sleep 3
echo ""
echo -e "${GREEN}Services available at:${NC}"
echo "  KrakenD:    http://localhost:8080"
echo "  Auth:       http://localhost:9000"
echo "  Grafana:    http://localhost:3000 (admin/admin)"
echo "  Jaeger:     http://localhost:16686"
echo "  Prometheus: http://localhost:9090"
echo ""
echo "Test API: cd .. && ./test-api.sh"
echo "Stop port-forwarding: pkill -f 'kubectl port-forward'"

