#!/bin/bash

# Kafka Failure Simulation Script
# This script helps test the resilience system by simulating Kafka failures

echo "🔥 Kafka Failure Simulation"
echo "=========================="

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

BASE_URL="http://localhost:8081/api/payments"

# Function to create a payment
create_payment() {
    local payment_data='{
        "userId": "user-'$(date +%s)'",
        "merchantId": "merchant-456",
        "amount": '$(echo "scale=2; $RANDOM/100" | bc)',
        "currency": "USD",
        "description": "Kafka Failure Test Payment"
    }'
    
    echo -e "${BLUE}Creating payment...${NC}"
    response=$(curl -s -X POST "$BASE_URL" \
        -H "Content-Type: application/json" \
        -d "$payment_data")
    
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    return $?
}

# Function to check health
check_health() {
    echo -e "\n${BLUE}Checking system health...${NC}"
    curl -s "$BASE_URL/health/detailed" | jq '.' 2>/dev/null || echo "Health check failed"
}

# Function to monitor payments
monitor_payments() {
    echo -e "\n${BLUE}Current payments:${NC}"
    curl -s "$BASE_URL" | jq '.[] | {id: .id, status: .status, amount: .amount}' 2>/dev/null || echo "Failed to fetch payments"
}

# Function to stop Kafka (if using Docker)
stop_kafka() {
    echo -e "\n${RED}🛑 Stopping Kafka...${NC}"
    if command -v docker-compose &> /dev/null; then
        docker-compose stop kafka
        echo "Kafka stopped via Docker Compose"
    else
        echo "Please stop Kafka manually to test fallback mechanisms"
    fi
}

# Function to start Kafka
start_kafka() {
    echo -e "\n${GREEN}🚀 Starting Kafka...${NC}"
    if command -v docker-compose &> /dev/null; then
        docker-compose start kafka
        echo "Kafka started via Docker Compose"
        sleep 10  # Wait for Kafka to be ready
    else
        echo "Please start Kafka manually"
    fi
}

# Main simulation
main() {
    echo -e "${YELLOW}This script will simulate Kafka failures to test resilience${NC}"
    echo "Press Ctrl+C to stop at any time"
    echo ""
    
    # Initial state
    echo -e "${GREEN}1. Initial state - Kafka running${NC}"
    check_health
    create_payment
    sleep 2
    
    # Stop Kafka
    echo -e "\n${RED}2. Stopping Kafka to test fallback${NC}"
    stop_kafka
    sleep 5
    
    # Test fallback
    echo -e "\n${YELLOW}3. Testing fallback mechanisms${NC}"
    for i in {1..5}; do
        echo -e "\n${BLUE}Payment attempt $i/5${NC}"
        create_payment
        check_health
        sleep 3
    done
    
    # Monitor payments
    monitor_payments
    
    # Restart Kafka
    echo -e "\n${GREEN}4. Restarting Kafka to test recovery${NC}"
    start_kafka
    
    # Test recovery
    echo -e "\n${YELLOW}5. Testing recovery mechanisms${NC}"
    for i in {1..3}; do
        echo -e "\n${BLUE}Recovery test $i/3${NC}"
        create_payment
        check_health
        sleep 2
    done
    
    # Final state
    echo -e "\n${GREEN}6. Final state${NC}"
    monitor_payments
    check_health
    
    echo -e "\n${GREEN}✅ Kafka failure simulation completed!${NC}"
}

# Check if service is running
if ! curl -s "$BASE_URL/health" > /dev/null; then
    echo -e "${RED}❌ Payment service is not running!${NC}"
    echo "Please start the service with: mvn spring-boot:run"
    exit 1
fi

# Run simulation
main
