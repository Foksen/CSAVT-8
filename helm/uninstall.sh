#!/bin/bash

echo "Uninstalling Shop Microservices from Minikube..."

GREEN='\033[0;32m'
NC='\033[0m'

echo "Removing services..."
helm uninstall krakend -n shop-system 2>/dev/null || true
helm uninstall order-service -n shop-system 2>/dev/null || true
helm uninstall customer-service -n shop-system 2>/dev/null || true
helm uninstall product-service -n shop-system 2>/dev/null || true
helm uninstall auth-service -n shop-system 2>/dev/null || true

echo "Removing observability..."
helm uninstall grafana -n shop-system 2>/dev/null || true
helm uninstall prometheus -n shop-system 2>/dev/null || true
helm uninstall jaeger -n shop-system 2>/dev/null || true
helm uninstall graylog -n shop-system 2>/dev/null || true

kubectl delete clusterrole prometheus grafana jaeger --ignore-not-found 2>/dev/null || true
kubectl delete clusterrolebinding prometheus grafana jaeger --ignore-not-found 2>/dev/null || true

echo "Removing infrastructure..."
helm uninstall kafka -n shop-system 2>/dev/null || true
helm uninstall redis -n shop-system 2>/dev/null || true
helm uninstall postgresql -n shop-system 2>/dev/null || true

echo "Removing PVCs (force)..."
kubectl delete pvc --all -n shop-system --grace-period=0 --force 2>/dev/null || true

echo "Removing namespace (force)..."
kubectl delete namespace shop-system --grace-period=0 --force 2>/dev/null || true

sleep 5

echo -e "${GREEN}Cleanup complete${NC}"

