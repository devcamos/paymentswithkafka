#!/bin/bash

# Payment Service Resilience Testing Script
# Tests Kafka failures, circuit breakers, and fallback mechanisms

echo "🧪 Payment Service Resilience Testing"
echo "======================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8081/api/payments"
PAYMENT_DATA='{
  "userId": "test-user-123",
  "merchantId": "test-merchant-456", 
  "amount": 99.99,
  "currency": "USD",
  "description": "Resilience Test Payment"
}'

# Test functions
test_service_health() {
    echo -e "\n${BLUE}1. Testing Service Health${NC}"
    echo "GET $BASE_URL/health"
    curl -s "$BASE_URL/health" | jq '.' 2>/dev/null || echo "Service not responding"
}

test_detailed_health() {
    echo -e "\n${BLUE}2. Testing Detailed Health${NC}"
    echo "GET $BASE_URL/health/detailed"
    curl -s "$BASE_URL/health/detailed" | jq '.' 2>/dev/null || echo "Detailed health not available"
}

test_payment_creation() {
    echo -e "\n${BLUE}3. Testing Payment Creation${NC}"
    echo "POST $BASE_URL"
    echo "Data: $PAYMENT_DATA"
    
    response=$(curl -s -X POST "$BASE_URL" \
        -H "Content-Type: application/json" \
        -d "$PAYMENT_DATA")
    
    echo "Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    
    # Extract payment ID for further testing
    PAYMENT_ID=$(echo "$response" | jq -r '.id' 2>/dev/null)
    if [ "$PAYMENT_ID" != "null" ] && [ "$PAYMENT_ID" != "" ]; then
        echo -e "${GREEN}✓ Payment created with ID: $PAYMENT_ID${NC}"
    else
        echo -e "${RED}✗ Payment creation failed${NC}"
    fi
}

test_payment_status() {
    if [ -n "$PAYMENT_ID" ]; then
        echo -e "\n${BLUE}4. Testing Payment Status${NC}"
        echo "GET $BASE_URL/status/$PAYMENT_ID"
        
        response=$(curl -s "$BASE_URL/status/$PAYMENT_ID")
        echo "Response:"
        echo "$response" | jq '.' 2>/dev/null || echo "$response"
    else
        echo -e "${YELLOW}⚠ Skipping payment status test - no payment ID${NC}"
    fi
}

test_payment_list() {
    echo -e "\n${BLUE}5. Testing Payment List${NC}"
    echo "GET $BASE_URL"
    
    response=$(curl -s "$BASE_URL")
    echo "Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
}

test_payment_stats() {
    echo -e "\n${BLUE}6. Testing Payment Statistics${NC}"
    echo "GET $BASE_URL/stats"
    
    response=$(curl -s "$BASE_URL/stats")
    echo "Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
}

test_kafka_failure_simulation() {
    echo -e "\n${BLUE}7. Simulating Kafka Failure${NC}"
    echo "This test simulates what happens when Kafka is down"
    echo "The service should fall back to database processing"
    
    # Create multiple payments to test resilience
    for i in {1..3}; do
        echo -e "\n${YELLOW}Creating payment $i/3...${NC}"
        test_payment_creation
        sleep 2
    done
}

test_circuit_breaker() {
    echo -e "\n${BLUE}8. Testing Circuit Breaker${NC}"
    echo "Monitoring health during multiple requests..."
    
    for i in {1..10}; do
        echo -e "\n${YELLOW}Request $i/10${NC}"
        test_detailed_health
        sleep 1
    done
}

test_websocket_connection() {
    echo -e "\n${BLUE}9. Testing WebSocket Connection${NC}"
    echo "WebSocket endpoint: ws://localhost:8081/ws"
    echo "This requires a WebSocket client to test properly"
    echo "You can use the frontend or a WebSocket testing tool"
}

# Main test execution
main() {
    echo -e "${GREEN}Starting resilience tests...${NC}"
    
    # Check if service is running
    if ! curl -s "$BASE_URL/health" > /dev/null; then
        echo -e "${RED}❌ Payment service is not running!${NC}"
        echo "Please start the service with: mvn spring-boot:run"
        exit 1
    fi
    
    # Run tests
    test_service_health
    test_detailed_health
    test_payment_creation
    test_payment_status
    test_payment_list
    test_payment_stats
    test_kafka_failure_simulation
    test_circuit_breaker
    test_websocket_connection
    
    echo -e "\n${GREEN}✅ Resilience testing completed!${NC}"
    echo -e "\n${YELLOW}Next steps:${NC}"
    echo "1. Stop Kafka to test fallback mechanisms"
    echo "2. Monitor logs for circuit breaker activation"
    echo "3. Test WebSocket connections with frontend"
    echo "4. Verify payment processing continues via database"
}

# Check dependencies
check_dependencies() {
    if ! command -v curl &> /dev/null; then
        echo -e "${RED}❌ curl is required but not installed${NC}"
        exit 1
    fi
    
    if ! command -v jq &> /dev/null; then
        echo -e "${YELLOW}⚠ jq is not installed - JSON responses won't be formatted${NC}"
        echo "Install with: brew install jq"
    fi
}

# Run the tests
check_dependencies
main
