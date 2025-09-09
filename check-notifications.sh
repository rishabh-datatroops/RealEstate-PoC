#!/bin/bash

# Notification Checker Script for Real Estate POC

BASE_URL="http://localhost:8080"

echo "🔔 Real Estate Notification Checker"
echo "===================================="

# Function to pretty print JSON
pp() {
  local input
  input=$(cat)
  if echo "$input" | jq -e . >/dev/null 2>&1; then
    echo "$input" | jq .
  else
    echo "$input"
  fi
}

# Check if server is running
if ! curl -s "$BASE_URL/health" > /dev/null; then
  echo "❌ Server is not running at $BASE_URL"
  echo "Please start the server with: sbt run"
  exit 1
fi

echo "✅ Server is running"
echo ""

# Get all recent notifications
echo "📋 Recent Notifications:"
echo "------------------------"
NOTIFICATIONS=$(curl -s "$BASE_URL/notifications")

if echo "$NOTIFICATIONS" | jq -e '.data | length' > /dev/null 2>&1; then
  COUNT=$(echo "$NOTIFICATIONS" | jq -r '.data | length')
  echo "Found $COUNT notifications:"
  echo ""
  
  if [ "$COUNT" -gt 0 ]; then
    echo "$NOTIFICATIONS" | jq -r '.data[] | 
      "🚨 \(.notificationType) for \(.userId) at \(.timestamp)
      📋 Listing: \(.listingId)
      💬 \(.message)
      " + ("=" * 50)'
  else
    echo "No notifications found yet."
    echo "💡 Tip: Create a listing that matches a subscription to see notifications!"
  fi
else
  echo "Error getting notifications:"
  echo "$NOTIFICATIONS" | pp
fi

echo ""
echo "🔄 To see live notifications, run:"
echo "   tail -f <your-sbt-output> | grep '\[NOTIFICATION\]'"
echo ""
echo "🌐 Or check via API:"
echo "   curl -s $BASE_URL/notifications | jq ."
echo "   curl -s $BASE_URL/notifications/user/john_doe | jq ."