#!/bin/bash

set -e

echo "🗑️  Uninstalling Shop Microservices from Minikube..."

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

# Uninstall services
echo "Removing services..."
helm uninstall krakend -n shop-system 2>/dev/null || true
helm uninstall order-service -n shop-system 2>/dev/null || true
helm uninstall customer-service -n shop-system 2>/dev/null || true
helm uninstall product-service -n shop-system 2>/dev/null || true
helm uninstall auth-service -n shop-system 2>/dev/null || true

# Удаление ingressов Grafana и Jaeger
kubectl delete ingress grafana-ingress -n shop-system 2>/dev/null || true
kubectl delete ingress jaeger-ingress -n shop-system 2>/dev/null || true

# Удаление остаточных ClusterRole и ClusterRoleBinding (мешают helm install)
kubectl delete clusterrole prometheus grafana jaeger --ignore-not-found
kubectl delete clusterrolebinding prometheus grafana jaeger --ignore-not-found

# Uninstall infrastructure
echo "Removing infrastructure..."
helm uninstall kafka -n shop-system 2>/dev/null || true
helm uninstall redis -n shop-system 2>/dev/null || true
helm uninstall postgresql -n shop-system 2>/dev/null || true

# Delete PVCs
echo "Removing PersistentVolumeClaims..."
kubectl delete pvc --all -n shop-system 2>/dev/null || true

# Delete namespace
echo "Removing namespace..."
kubectl delete namespace shop-system 2>/dev/null || true

echo -e "${GREEN}✅ Cleanup complete!${NC}"

