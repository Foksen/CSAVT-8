#!/bin/bash

helm uninstall krakend -n shop-system 2>/dev/null || true
helm uninstall order-service -n shop-system 2>/dev/null || true
helm uninstall customer-service -n shop-system 2>/dev/null || true
helm uninstall product-service -n shop-system 2>/dev/null || true
helm uninstall auth-service -n shop-system 2>/dev/null || true
helm uninstall grafana -n shop-system 2>/dev/null || true
helm uninstall prometheus -n shop-system 2>/dev/null || true
helm uninstall jaeger -n shop-system 2>/dev/null || true
helm uninstall graylog -n shop-system 2>/dev/null || true

kubectl delete clusterrole prometheus grafana jaeger --ignore-not-found 2>/dev/null || true
kubectl delete clusterrolebinding prometheus grafana jaeger --ignore-not-found 2>/dev/null || true

helm uninstall kafka -n shop-system 2>/dev/null || true
helm uninstall redis -n shop-system 2>/dev/null || true
helm uninstall postgresql -n shop-system 2>/dev/null || true

kubectl delete pvc --all -n shop-system --grace-period=0 --force 2>/dev/null || true
kubectl delete namespace shop-system --grace-period=0 --force 2>/dev/null || true

sleep 5
echo "Done"

