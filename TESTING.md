# 🧪 Payment Service Resilience Testing Guide

This guide explains how to test the production-grade resilience system locally, including Kafka failure scenarios and fallback mechanisms.

## 🚀 Quick Start

### 1. Start All Services
```bash
# Start Kafka and dependencies
docker-compose up -d

# Start payment service
cd payment-service
mvn spring-boot:run
```

### 2. Run Basic Tests
```bash
# Run the resilience test script
./test-resilience.sh

# Or run individual tests
curl http://localhost:8081/api/payments/health
```

### 3. Open Frontend Testing Interface
```bash
# Open the resilience testing page
open frontend/resilience-test.html
```

## 🔧 Testing Scenarios

### Scenario 1: Normal Operation
**Goal**: Verify system works correctly when Kafka is healthy

```bash
# 1. Check health
curl http://localhost:8081/api/payments/health/detailed

# 2. Create payment
curl -X POST http://localhost:8081/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "merchantId": "merchant456",
    "amount": 99.99,
    "currency": "USD",
    "description": "Test Payment"
  }'

# 3. Check payment status
curl http://localhost:8081/api/payments/status/{payment-id}
```

**Expected Result**: Payment created successfully, processed via Kafka

### Scenario 2: Kafka Failure
**Goal**: Test fallback mechanisms when Kafka is down

```bash
# 1. Stop Kafka
docker-compose stop kafka

# 2. Create payments (should use database fallback)
curl -X POST http://localhost:8081/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "merchantId": "merchant456", 
    "amount": 99.99,
    "currency": "USD",
    "description": "Kafka Down Test"
  }'

# 3. Check health (should show degraded status)
curl http://localhost:8081/api/payments/health/detailed
```

**Expected Result**: 
- Payment created successfully
- Health status shows "DEGRADED"
- Payment processed via database fallback

### Scenario 3: Circuit Breaker Activation
**Goal**: Test circuit breaker pattern

```bash
# 1. Simulate multiple failures
for i in {1..10}; do
  curl -X POST http://localhost:8081/api/payments \
    -H "Content-Type: application/json" \
    -d '{"userId":"user'$i'","merchantId":"merchant","amount":10.00,"currency":"USD","description":"Test"}'
  sleep 1
done

# 2. Check circuit breaker status
curl http://localhost:8081/api/payments/health/detailed
```

**Expected Result**: Circuit breaker activates after threshold

### Scenario 4: Recovery Testing
**Goal**: Test system recovery when Kafka comes back

```bash
# 1. Restart Kafka
docker-compose start kafka

# 2. Wait for recovery
sleep 30

# 3. Check health
curl http://localhost:8081/api/payments/health/detailed

# 4. Create new payment
curl -X POST http://localhost:8081/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "merchantId": "merchant456",
    "amount": 99.99,
    "currency": "USD", 
    "description": "Recovery Test"
  }'
```

**Expected Result**: System recovers, payments processed via Kafka

## 🎯 Automated Testing

### Run All Tests
```bash
# Run the complete test suite
./test-resilience.sh

# Run Kafka failure simulation
./simulate-kafka-failure.sh
```

### Unit Tests
```bash
cd payment-service
mvn test
```

### Integration Tests
```bash
cd payment-service
mvn test -Dtest=PaymentResilienceIntegrationTest
```

## 📊 Monitoring and Observability

### Health Endpoints
- **Basic Health**: `GET /api/payments/health`
- **Detailed Health**: `GET /api/payments/health/detailed`
- **Payment Stats**: `GET /api/payments/stats`

### WebSocket Monitoring
- **Endpoint**: `ws://localhost:8081/ws`
- **Topic**: `/topic/payments/{userId}`

### Kafka UI
- **URL**: http://localhost:8080
- **Topics**: Monitor `payments` topic
- **Consumers**: Check consumer groups

## 🔍 Troubleshooting

### Common Issues

#### 1. Service Won't Start
```bash
# Check if port is in use
lsof -i :8081

# Check logs
cd payment-service
mvn spring-boot:run
```

#### 2. Kafka Connection Issues
```bash
# Check Kafka status
docker-compose ps kafka

# Check Kafka logs
docker-compose logs kafka

# Restart Kafka
docker-compose restart kafka
```

#### 3. WebSocket Connection Fails
```bash
# Check if WebSocket endpoint is accessible
curl http://localhost:8081/ws

# Check browser console for errors
```

### Debug Mode
```bash
# Enable debug logging
export SPRING_PROFILES_ACTIVE=debug
mvn spring-boot:run
```

## 📈 Performance Testing

### Load Testing
```bash
# Install Apache Bench
brew install httpd

# Test payment creation
ab -n 100 -c 10 -p payment.json -T application/json http://localhost:8081/api/payments

# Test health endpoint
ab -n 1000 -c 50 http://localhost:8081/api/payments/health
```

### Stress Testing
```bash
# Create many payments quickly
for i in {1..100}; do
  curl -X POST http://localhost:8081/api/payments \
    -H "Content-Type: application/json" \
    -d '{"userId":"user'$i'","merchantId":"merchant","amount":10.00,"currency":"USD","description":"Load Test"}' &
done
wait
```

## 🎭 Frontend Testing

### Open Testing Interface
```bash
# Open the resilience testing page
open frontend/resilience-test.html
```

### Test Features
- ✅ Create payments
- ✅ Monitor health status
- ✅ View payment statistics
- ✅ Real-time updates via WebSocket
- ✅ Simulate Kafka failures
- ✅ Test offline scenarios

## 📝 Test Data

### Sample Payment Request
```json
{
  "userId": "user123",
  "merchantId": "merchant456",
  "amount": 99.99,
  "currency": "USD",
  "description": "Test Payment"
}
```

### Expected Health Response (Healthy)
```json
{
  "status": "HEALTHY",
  "kafkaHealthy": true,
  "kafkaAvailable": true,
  "consecutiveFailures": 0,
  "queuedPayments": 0,
  "timestamp": 1758204000000
}
```

### Expected Health Response (Degraded)
```json
{
  "status": "DEGRADED",
  "kafkaHealthy": false,
  "kafkaAvailable": false,
  "consecutiveFailures": 5,
  "queuedPayments": 23,
  "timestamp": 1758204000000
}
```

## 🚨 Error Scenarios

### 1. Kafka Completely Down
- **Behavior**: Circuit breaker activates
- **Fallback**: Database processing
- **User Impact**: None (seamless fallback)

### 2. Network Issues
- **Behavior**: Retry with exponential backoff
- **Fallback**: Local queue for offline processing
- **User Impact**: Minimal (automatic retry)

### 3. Database Issues
- **Behavior**: Manual review queue
- **Fallback**: Store for later processing
- **User Impact**: Clear error message

### 4. Complete System Failure
- **Behavior**: Graceful degradation
- **Fallback**: Error handling with retry
- **User Impact**: Informative error messages

## 📚 Additional Resources

- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)
- [Kafka Testing Best Practices](https://kafka.apache.org/documentation/#testing)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Resilience4j Documentation](https://resilience4j.readme.io/)

## 🤝 Contributing

To add new test scenarios:

1. Add test methods to `PaymentResilienceServiceTest.java`
2. Update `test-resilience.sh` with new scenarios
3. Add frontend tests to `resilience-test.html`
4. Update this documentation

## 📞 Support

If you encounter issues:

1. Check the logs: `docker-compose logs payment-service`
2. Verify all services are running: `docker-compose ps`
3. Check health endpoints: `curl http://localhost:8081/api/payments/health`
4. Review this documentation for common solutions
