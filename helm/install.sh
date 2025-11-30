#!/bin/bash
set -e

if ! minikube status | grep -q "Running"; then
    echo "Start minikube first: minikube start --cpus=4 --memory=6144 --driver=docker"
    exit 1
fi

kubectl create namespace shop-system --dry-run=client -o yaml | kubectl apply -f -

helm upgrade --install postgresql ./shop-system/charts/postgresql -n shop-system --create-namespace
helm upgrade --install redis ./shop-system/charts/redis -n shop-system
helm upgrade --install kafka ./shop-system/charts/kafka -n shop-system

sleep 10
kubectl wait --for=condition=ready pod -l app=redis -n shop-system --timeout=300s 2>/dev/null || true
kubectl wait --for=condition=ready pod -l app=kafka -n shop-system --timeout=300s 2>/dev/null || true
kubectl wait --for=condition=ready pod -l app=zookeeper -n shop-system --timeout=300s 2>/dev/null || true
kubectl wait --for=condition=ready pod -l app=auth-db-postgresql -n shop-system --timeout=300s 2>/dev/null || true
kubectl wait --for=condition=ready pod -l app=product-db-postgresql -n shop-system --timeout=300s 2>/dev/null || true
kubectl wait --for=condition=ready pod -l app=customer-db-postgresql -n shop-system --timeout=300s 2>/dev/null || true
kubectl wait --for=condition=ready pod -l app=order-db-postgresql -n shop-system --timeout=300s 2>/dev/null || true

eval $(minikube docker-env --shell bash)
cd ..

echo "Building auth-service..."
docker build -t shop-auth-service:latest --build-arg SERVICE_NAME=infra/auth-service -f Dockerfile .
echo "Building product-service..."
docker build -t shop-product-service:latest --build-arg SERVICE_NAME=service/product-service -f Dockerfile .
echo "Building customer-service..."
docker build -t shop-customer-service:latest --build-arg SERVICE_NAME=service/customer-service -f Dockerfile .
echo "Building order-service..."
docker build -t shop-order-service:latest --build-arg SERVICE_NAME=service/order-service -f Dockerfile .

cd helm

helm upgrade --install auth-service ./shop-system/charts/auth-service -n shop-system
helm upgrade --install product-service ./shop-system/charts/product-service -n shop-system
helm upgrade --install customer-service ./shop-system/charts/customer-service -n shop-system
helm upgrade --install order-service ./shop-system/charts/order-service -n shop-system

helm upgrade --install graylog ./shop-system/charts/graylog -n shop-system
helm upgrade --install jaeger ./shop-system/charts/jaeger -n shop-system
helm upgrade --install prometheus ./shop-system/charts/prometheus -n shop-system
helm upgrade --install grafana ./shop-system/charts/grafana -n shop-system

helm upgrade --install krakend ./shop-system/charts/krakend -n shop-system

sleep 20
kubectl get pods -n shop-system

pkill -f "kubectl port-forward" 2>/dev/null || true
sleep 2

kubectl port-forward -n shop-system svc/krakend 8080:8080 > /tmp/krakend-pf.log 2>&1 &
kubectl port-forward -n shop-system svc/auth-service 9000:9000 > /tmp/auth-pf.log 2>&1 &
kubectl port-forward -n shop-system svc/grafana 3000:3000 > /tmp/grafana-pf.log 2>&1 &
kubectl port-forward -n shop-system svc/jaeger-ui 16686:16686 > /tmp/jaeger-pf.log 2>&1 &
kubectl port-forward -n shop-system svc/prometheus-server 9090:9090 > /tmp/prometheus-pf.log 2>&1 &

sleep 5

echo ""
echo "KrakenD:    http://localhost:8080"
echo "Auth:       http://localhost:9000"
echo "Grafana:    http://localhost:3000 (admin/admin)"
echo "Jaeger:     http://localhost:16686"
echo "Prometheus: http://localhost:9090"
echo ""
echo "Run tests: cd .. && ./test-api.sh"

