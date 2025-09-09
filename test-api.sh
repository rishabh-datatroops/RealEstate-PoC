#!/bin/bash

# API Test Script for Real Estate POC

BASE_URL="http://localhost:8080"

echo "Testing Real Estate POC API..."
echo "================================"

# Helper to pretty print JSON if possible, otherwise print raw
pp() {
  local input
  input=$(cat)
  if echo "$input" | jq -e . >/dev/null 2>&1; then
    echo "$input" | jq .
  else
    echo "$input"
  fi
}

# Test health endpoint
echo "1. Testing health endpoint..."
curl -s "$BASE_URL/health" | pp
echo ""

# Test property types endpoint
echo "2. Getting available property types..."
curl -s "$BASE_URL/listings/types" | pp
echo ""

# Test listing creation with property type
echo "3. Creating a commercial listing..."
LISTING_RESPONSE=$(curl -s -X POST "$BASE_URL/listings" \
  -H "Content-Type: application/json" \
  -d '{
    "address": "456 Business Ave, New York, NY",
    "price": 1200000,
    "propertyType": "COMMERCIAL"
  }')
echo "$LISTING_RESPONSE" | pp

# Extract listing ID for further tests
LISTING_ID=$(echo "$LISTING_RESPONSE" | jq -r '.data.id')
echo ""

# Test listing creation without property type (should default to RESIDENTIAL)
echo "4. Creating a residential listing (default)..."
RESIDENTIAL_RESPONSE=$(curl -s -X POST "$BASE_URL/listings" \
  -H "Content-Type: application/json" \
  -d '{
    "address": "789 Home St, New York, NY",
    "price": 750000
  }')
echo "$RESIDENTIAL_RESPONSE" | pp
echo ""

# Test get all listings
echo "5. Getting all listings..."
curl -s "$BASE_URL/listings" | pp
echo ""

# Test get specific listing
echo "6. Getting specific listing..."
curl -s "$BASE_URL/listings/$LISTING_ID" | pp
echo ""

# Test filtering by property type
echo "7. Getting commercial listings..."
curl -s "$BASE_URL/listings/type/COMMERCIAL" | pp
echo ""

echo "8. Getting residential listings..."
curl -s "$BASE_URL/listings/type/RESIDENTIAL" | pp
echo ""

# Test price update
echo "9. Updating listing price..."
curl -s -X PUT "$BASE_URL/listings/$LISTING_ID/price" \
  -H "Content-Type: application/json" \
  -d '{
    "price": 1100000
  }' | pp
echo ""

# Test property type update
echo "10. Updating property type to MIXED_USE..."
curl -s -X PUT "$BASE_URL/listings/$LISTING_ID/propertyType" \
  -H "Content-Type: application/json" \
  -d '{
    "propertyType": "MIXED_USE"
  }' | pp
echo ""

# Test subscription creation
echo "11. Creating a subscription..."
SUBSCRIPTION_RESPONSE=$(curl -s -X POST "$BASE_URL/subscriptions" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "address": "Business Ave",
    "price": 1500000
  }')
echo "$SUBSCRIPTION_RESPONSE" | pp

# Extract subscription ID
SUBSCRIPTION_ID=$(echo "$SUBSCRIPTION_RESPONSE" | jq -r '.data.id')
echo ""

# Test get all subscriptions
echo "12. Getting all subscriptions..."
curl -s "$BASE_URL/subscriptions" | pp
echo ""

# Test get specific subscription
echo "13. Getting specific subscription..."
curl -s "$BASE_URL/subscriptions/$SUBSCRIPTION_ID" | pp
echo ""

# Test get user subscriptions
echo "14. Getting user subscriptions..."
curl -s "$BASE_URL/subscriptions/user/user123" | pp
echo ""

# Test delete subscription
echo "15. Deleting subscription..."
curl -s -X DELETE "$BASE_URL/subscriptions/$SUBSCRIPTION_ID" | pp
echo ""

# Test invalid property type
echo "16. Testing invalid property type..."
curl -s -X POST "$BASE_URL/listings" \
  -H "Content-Type: application/json" \
  -d '{
    "address": "Invalid St",
    "price": 500000,
    "propertyType": "INVALID_TYPE"
  }' | pp
echo ""

echo "API testing completed!"
