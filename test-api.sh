#!/bin/bash

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

BASE_URL="http://localhost:8080"
AUTH_URL="http://localhost:9000"

echo "==============================================="
echo "Testing Shop Microservices API"
echo "==============================================="
echo ""

if ! timeout 3 curl -s "$BASE_URL/products" > /dev/null 2>&1; then
    echo -e "${YELLOW}Port-forwarding is not active, starting...${NC}"
    pkill -f "kubectl port-forward" 2>/dev/null || true
    sleep 2
    kubectl port-forward -n shop-system svc/krakend 8080:8080 > /tmp/krakend-pf.log 2>&1 &
    kubectl port-forward -n shop-system svc/auth-service 9000:9000 > /tmp/auth-pf.log 2>&1 &
    sleep 3
    echo ""
fi

echo -e "${GREEN}Starting tests...${NC}"
echo ""

echo -e "${BLUE}[1/6] Creating user...${NC}"
USER_RESPONSE=$(timeout 5 curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/auth/users" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test123!","email":"test@example.com"}')

HTTP_CODE=$(echo "$USER_RESPONSE" | tail -n1)
BODY=$(echo "$USER_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "400" ]; then
    echo -e "${GREEN}User created or already exists${NC}"
    echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
else
    echo -e "${RED}Failed to create user (HTTP $HTTP_CODE)${NC}"
    echo "$BODY"
fi
echo ""

echo -e "${BLUE}[2/6] Getting OAuth2 token...${NC}"
TOKEN_RESPONSE=$(timeout 5 curl -s -X POST "${AUTH_URL}/oauth2/token" \
  -u "shop-client:shop-secret" \
  -d "grant_type=client_credentials")

echo "$TOKEN_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$TOKEN_RESPONSE"

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('access_token', ''))" 2>/dev/null || echo "")

if [ -n "$ACCESS_TOKEN" ]; then
  echo ""
  echo -e "${GREEN}Access Token received${NC}"
  echo "Token: ${ACCESS_TOKEN:0:50}..."
  echo ""
  
  echo -e "${BLUE}[3/6] Creating product...${NC}"
  PRODUCT_RESPONSE=$(timeout 5 curl -s -X POST "${BASE_URL}/products" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"Laptop Dell XPS 15","price":1499.99,"quantity":10}')
  
  echo "$PRODUCT_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$PRODUCT_RESPONSE"
  PRODUCT_ID=$(echo "$PRODUCT_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null || echo "1")
  echo ""
  
  echo -e "${BLUE}[4/6] Getting products list...${NC}"
  timeout 5 curl -s -X GET "${BASE_URL}/products" \
    -H "Authorization: Bearer $ACCESS_TOKEN" | python3 -m json.tool 2>/dev/null | head -30
  echo ""
  
  echo -e "${BLUE}[5/6] Creating customer...${NC}"
  CUSTOMER_RESPONSE=$(timeout 5 curl -s -X POST "${BASE_URL}/customers" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"userId":1,"name":"John Doe","phone":"+1234567890","address":"123 Main St, City"}')
  
  echo "$CUSTOMER_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$CUSTOMER_RESPONSE"
  CUSTOMER_ID=$(echo "$CUSTOMER_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null || echo "1")
  echo ""
  
  echo -e "${BLUE}[6/6] Creating order...${NC}"
  ORDER_RESPONSE=$(timeout 5 curl -s -X POST "${BASE_URL}/orders" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"customerId\":$CUSTOMER_ID,\"productId\":$PRODUCT_ID,\"quantity\":2}")
  
  echo "$ORDER_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$ORDER_RESPONSE"
  echo ""
  
  echo -e "${GREEN}Testing completed successfully${NC}"
  echo ""
  echo "Observability:"
  echo "  Grafana:    http://localhost:3000 (admin/admin)"
  echo "  Jaeger:     http://localhost:16686"
  echo "  Prometheus: http://localhost:9090"
else
  echo -e "${RED}Failed to get token${NC}"
  echo ""
  echo "Check logs: kubectl logs -n shop-system -l app=auth-service --tail=50"
fi

echo ""
echo "==============================================="

