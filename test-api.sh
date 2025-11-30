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

echo -e "${YELLOW}Setting up port-forwarding...${NC}"
pkill -f "kubectl port-forward" 2>/dev/null || true
sleep 2

kubectl port-forward -n shop-system svc/krakend 8080:8080 > /tmp/krakend-pf.log 2>&1 &
KRAKEND_PID=$!
kubectl port-forward -n shop-system svc/auth-service 9000:9000 > /tmp/auth-pf.log 2>&1 &
AUTH_PID=$!
kubectl port-forward -n shop-system svc/grafana 3000:3000 > /tmp/grafana-pf.log 2>&1 &
kubectl port-forward -n shop-system svc/jaeger-ui 16686:16686 > /tmp/jaeger-pf.log 2>&1 &
kubectl port-forward -n shop-system svc/prometheus-server 9090:9090 > /tmp/prometheus-pf.log 2>&1 &

echo "Waiting for port-forwarding to be ready..."
sleep 10

if ! timeout 10 curl -s http://localhost:8080/__health > /dev/null 2>&1; then
    echo -e "${RED}KrakenD port-forward failed${NC}"
    exit 1
fi

if ! timeout 10 curl -s http://localhost:9000/actuator/health > /dev/null 2>&1; then
    echo -e "${RED}Auth-service port-forward failed${NC}"
    exit 1
fi

echo -e "${GREEN}Port-forwarding ready${NC}"
echo ""

echo -e "${GREEN}Starting tests...${NC}"
echo ""

FAILED_TESTS=0
TIMESTAMP=$(date +%s)
USERNAME="testuser_${TIMESTAMP}"
EMAIL="test_${TIMESTAMP}@example.com"
PRODUCT_NAME="Product_${TIMESTAMP}"
CUSTOMER_NAME="Customer_${TIMESTAMP}"

echo -e "${BLUE}[1/6] Creating user (${USERNAME})...${NC}"
USER_RESPONSE=$(timeout 5 curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/auth/users" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"Test123!\",\"email\":\"${EMAIL}\"}")

HTTP_CODE=$(echo "$USER_RESPONSE" | tail -n1)
BODY=$(echo "$USER_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "200" ]; then
    if [ "$BODY" = "{}" ] || [ "$BODY" = "[]" ] || [ -z "$BODY" ]; then
        echo -e "${RED}FAIL: Empty response${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    else
        echo -e "${GREEN}PASS${NC}"
        echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
        USER_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    fi
elif [ "$HTTP_CODE" = "400" ]; then
    echo -e "${YELLOW}User already exists - OK${NC}"
else
    echo -e "${RED}FAIL: HTTP $HTTP_CODE${NC}"
    echo "$BODY"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

echo -e "${BLUE}[2/6] Getting OAuth2 token...${NC}"
TOKEN_RESPONSE=$(timeout 5 curl -s -X POST "${AUTH_URL}/oauth2/token" \
  -u "shop-client:shop-secret" \
  -d "grant_type=client_credentials&scope=shop-api")

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

if [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "" ]; then
  echo -e "${GREEN}PASS${NC}"
else
  echo -e "${RED}FAIL: No access token${NC}"
  echo "$TOKEN_RESPONSE"
  FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

echo -e "${BLUE}[3/6] Creating product (${PRODUCT_NAME})...${NC}"
PRODUCT_RESPONSE=$(timeout 5 curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/products" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"${PRODUCT_NAME}\",\"price\":1499.99,\"quantity\":10}")

PRODUCT_HTTP=$(echo "$PRODUCT_RESPONSE" | tail -n1)
PRODUCT_BODY=$(echo "$PRODUCT_RESPONSE" | head -n-1)

if [ "$PRODUCT_HTTP" = "201" ] || [ "$PRODUCT_HTTP" = "200" ]; then
  if [ "$PRODUCT_BODY" = "{}" ] || [ "$PRODUCT_BODY" = "[]" ] || [ -z "$PRODUCT_BODY" ]; then
    echo -e "${RED}FAIL: Empty response${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
  else
    echo -e "${GREEN}PASS${NC}"
    echo "$PRODUCT_BODY" | python3 -m json.tool 2>/dev/null || echo "$PRODUCT_BODY"
    PRODUCT_ID=$(echo "$PRODUCT_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
  fi
else
  echo -e "${RED}FAIL: HTTP $PRODUCT_HTTP${NC}"
  echo "$PRODUCT_BODY"
  FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

echo -e "${BLUE}[4/6] Getting products list...${NC}"
PRODUCTS_RESPONSE=$(timeout 5 curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/products" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
PRODUCTS_HTTP=$(echo "$PRODUCTS_RESPONSE" | tail -n1)
PRODUCTS_BODY=$(echo "$PRODUCTS_RESPONSE" | head -n-1)

if [ "$PRODUCTS_HTTP" = "200" ]; then
  if [ "$PRODUCTS_BODY" = "{}" ] || [ "$PRODUCTS_BODY" = "[]" ] || [ -z "$PRODUCTS_BODY" ]; then
    echo -e "${YELLOW}WARN: Empty list (no products yet)${NC}"
  else
    echo -e "${GREEN}PASS${NC}"
    echo "$PRODUCTS_BODY" | python3 -m json.tool 2>/dev/null | head -30
  fi
else
  echo -e "${RED}FAIL: HTTP $PRODUCTS_HTTP${NC}"
  echo "$PRODUCTS_BODY"
  FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

echo -e "${BLUE}[5/6] Creating customer (${CUSTOMER_NAME})...${NC}"
CUSTOMER_RESPONSE=$(timeout 5 curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/customers" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${USER_ID:-1},\"name\":\"${CUSTOMER_NAME}\",\"phone\":\"+${TIMESTAMP}\",\"address\":\"123 Main St\"}")

CUSTOMER_HTTP=$(echo "$CUSTOMER_RESPONSE" | tail -n1)
CUSTOMER_BODY=$(echo "$CUSTOMER_RESPONSE" | head -n-1)

if [ "$CUSTOMER_HTTP" = "201" ] || [ "$CUSTOMER_HTTP" = "200" ]; then
  if [ "$CUSTOMER_BODY" = "{}" ] || [ "$CUSTOMER_BODY" = "[]" ] || [ -z "$CUSTOMER_BODY" ]; then
    echo -e "${RED}FAIL: Empty response${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
  else
    echo -e "${GREEN}PASS${NC}"
    echo "$CUSTOMER_BODY" | python3 -m json.tool 2>/dev/null || echo "$CUSTOMER_BODY"
    CUSTOMER_ID=$(echo "$CUSTOMER_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
  fi
else
  echo -e "${RED}FAIL: HTTP $CUSTOMER_HTTP${NC}"
  echo "$CUSTOMER_BODY" | python3 -m json.tool 2>/dev/null || echo "$CUSTOMER_BODY"
  FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

if [ -n "$PRODUCT_ID" ] && [ -n "$CUSTOMER_ID" ]; then
  echo -e "${BLUE}[6/6] Creating order...${NC}"
  ORDER_RESPONSE=$(timeout 5 curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/orders" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"customerId\":$CUSTOMER_ID,\"productId\":$PRODUCT_ID,\"quantity\":2}")
  
  ORDER_HTTP=$(echo "$ORDER_RESPONSE" | tail -n1)
  ORDER_BODY=$(echo "$ORDER_RESPONSE" | head -n-1)
  
  if [ "$ORDER_HTTP" = "201" ] || [ "$ORDER_HTTP" = "200" ]; then
    if [ "$ORDER_BODY" = "{}" ] || [ "$ORDER_BODY" = "[]" ] || [ -z "$ORDER_BODY" ]; then
      echo -e "${RED}FAIL: Empty response${NC}"
      FAILED_TESTS=$((FAILED_TESTS + 1))
    else
      echo -e "${GREEN}PASS${NC}"
      echo "$ORDER_BODY" | python3 -m json.tool 2>/dev/null || echo "$ORDER_BODY"
    fi
  else
    echo -e "${RED}FAIL: HTTP $ORDER_HTTP${NC}"
    echo "$ORDER_BODY"
    FAILED_TESTS=$((FAILED_TESTS + 1))
  fi
else
  echo -e "${RED}[6/6] SKIP: Missing product or customer ID${NC}"
  FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

echo "==============================================="
if [ $FAILED_TESTS -eq 0 ]; then
  echo -e "${GREEN}All tests passed (0 failed)${NC}"
else
  echo -e "${RED}Tests completed with $FAILED_TESTS failure(s)${NC}"
fi
echo ""
echo "Observability:"
echo "  Grafana:    http://localhost:3000 (admin/admin)"
echo "  Jaeger:     http://localhost:16686"
echo "  Prometheus: http://localhost:9090"
echo ""
echo "==============================================="

exit $FAILED_TESTS

