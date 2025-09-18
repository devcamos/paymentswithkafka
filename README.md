# 🚀 Payment System - Non-Blocking Architecture

A comprehensive monorepo demonstrating asynchronous payment processing using Spring Boot, Apache Kafka, and WebSockets. This project showcases non-blocking architecture patterns with real-time updates.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-7.4.0-blue.svg)](https://kafka.apache.org/)
[![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-yellow.svg)](https://spring.io/guides/gs/messaging-stomp-websocket/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://docs.docker.com/compose/)

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │  Payment API    │    │     Kafka       │
│   (Port 3000)   │◄──►│  (Port 8081)    │◄──►│  (Port 9092)    │
│                 │    │                 │    │                 │
│ • Real-time UI  │    │ • REST API      │    │ • Message Queue │
│ • WebSocket     │    │ • WebSocket     │    │ • Event Stream  │
│ • Status Updates│    │ • Kafka Producer│    │ • Topic: payments│
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │ Kafka Consumer  │
                       │                 │
                       │ • Async Processing│
                       │ • Status Updates │
                       │ • Event Publishing│
                       └─────────────────┘
```

## ✨ Key Features

### 🔄 Non-Blocking Architecture
- **Immediate Response**: Payment requests return instantly with a pending status
- **Asynchronous Processing**: Kafka handles payment processing in the background
- **Real-time Updates**: WebSocket provides live status updates to the frontend

### 🌐 Real-time Communication
- **User-Specific Updates**: `/topic/payments/{userId}` - Updates for specific users
- **Global Updates**: `/topic/payments/all` - Updates for all connected clients
- **Live Dashboard**: Real-time payment monitoring and statistics

### 🛡️ Production-Ready Features
- **Resilience Patterns**: Circuit breaker, retry logic, and fallback strategies
- **Error Handling**: Comprehensive error handling and recovery mechanisms
- **Monitoring**: Health checks, metrics, and system status monitoring
- **Testing**: Comprehensive test suite with unit and integration tests

## 📁 Project Structure

```
paymentswithkafka/
├── payment-service/          # Spring Boot Payment API
│   ├── src/main/java/
│   │   └── com/example/demo/
│   │       ├── controller/   # REST Controllers
│   │       ├── service/      # Kafka Producer/Consumer & Business Logic
│   │       ├── model/        # Payment Models
│   │       ├── dto/          # Data Transfer Objects
│   │       └── config/       # WebSocket Configuration
│   └── src/main/resources/
│       └── application.properties
├── frontend/                 # Enhanced Frontend
│   ├── index.html           # Payment Dashboard
│   ├── payment-frontend.js  # Frontend Logic
│   └── resilience-test.html # Resilience Testing UI
├── docker-compose.yml       # Multi-service Setup
└── README.md
```

## 🚀 Quick Start

### Prerequisites
- **Java 21**
- **Maven 3.6+**
- **Docker & Docker Compose**

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/paymentswithkafka.git
cd paymentswithkafka
```

### 2. Start Infrastructure Services
```bash
# Start Kafka, Zookeeper, Kafka UI, and Frontend
docker-compose up -d

# Check service status
docker-compose ps
```

### 3. Run Payment Service
```bash
cd payment-service
mvn spring-boot:run
```

### 4. Access Applications
- **🎨 Frontend**: http://localhost:3000 - Interactive payment dashboard
- **🔧 Payment API**: http://localhost:8081 - REST API endpoints  
- **📊 Kafka UI**: http://localhost:8080 - Monitor Kafka topics and messages
- **❤️ Health Check**: http://localhost:8081/api/payments/health

## 🎯 Demo the Non-Blocking Behavior

1. **Open Frontend**: Navigate to http://localhost:3000
2. **Create Payment**: Fill out the form and submit
3. **Observe**: 
   - ✅ **Immediate Response**: You'll get a "PENDING" status instantly
   - 🔄 **Real-time Updates**: Watch the status change via WebSocket
   - ⏱️ **Background Processing**: Payment processes asynchronously (1-5 seconds)
   - ✅ **Final Status**: Eventually shows "COMPLETED" or "FAILED"

## 📡 API Endpoints

### Payment API
```bash
# Create Payment
POST http://localhost:8081/api/payments
Content-Type: application/json

{
  "userId": "user123",
  "merchantId": "merchant456", 
  "amount": 100.00,
  "currency": "USD",
  "description": "Online purchase"
}

# Health Check
GET http://localhost:8081/api/payments/health

# Payment Status
GET http://localhost:8081/api/payments/status/{paymentId}

# Get All Payments
GET http://localhost:8081/api/payments?page=0&size=10

# Payment Statistics
GET http://localhost:8081/api/payments/stats
```

### Resilience API
```bash
# Create Payment with Resilience Patterns
POST http://localhost:8081/api/payments/resilience

# Detailed Health Check
GET http://localhost:8081/api/payments/resilience/health/detailed
```

### WebSocket
```javascript
// Connect to WebSocket
const socket = new SockJS('http://localhost:8081/ws');
const stompClient = new StompJs.Client({webSocketFactory: () => socket});

// Subscribe to user-specific updates
stompClient.subscribe('/topic/payments/user123', (message) => {
    const payment = JSON.parse(message.body);
    // Handle real-time payment updates
});

// Subscribe to global updates
stompClient.subscribe('/topic/payments/all', (message) => {
    const payment = JSON.parse(message.body);
    // Handle global payment updates
});
```

## 🔧 Services & Ports

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 3000 | Payment UI with real-time updates |
| Payment API | 8081 | Spring Boot REST API |
| Kafka UI | 8080 | Kafka management interface |
| Kafka Broker | 9092 | Message broker |
| Zookeeper | 2181 | Kafka coordination |
| Schema Registry | 8082 | Avro schema management |

## 🛠️ Development

### Running Tests
```bash
cd payment-service
mvn test
```

### Building the Application
```bash
cd payment-service
mvn clean package
```

### Docker Build
```bash
docker build -t payment-service ./payment-service
```

## 🔍 Monitoring & Debugging

### Kafka Topics
```bash
# List topics
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# Monitor messages
docker exec -it kafka kafka-console-consumer --topic payments --bootstrap-server localhost:9092 --from-beginning
```

### Logs
```bash
# Payment service logs
docker-compose logs -f payment-service

# Kafka logs
docker-compose logs -f kafka

# All services
docker-compose logs -f
```

## 🚨 Troubleshooting

### Common Issues
1. **Port Conflicts**: Ensure ports 3000, 8080, 8081, 9092 are available
2. **WebSocket Connection**: Check browser console for connection errors
3. **Kafka Connection**: Verify Kafka is running with `docker-compose ps`
4. **Payment Processing**: Check application logs for errors

### Reset Everything
```bash
# Stop and clean all services
docker-compose down -v

# Remove all containers and volumes
docker system prune -a --volumes

# Restart fresh
docker-compose up -d
cd payment-service && mvn spring-boot:run
```

## 📚 Learning Objectives

This project demonstrates:
- **Asynchronous Processing**: Non-blocking payment processing
- **Event-Driven Architecture**: Kafka for message queuing
- **Real-time Communication**: WebSocket for live updates
- **Microservices**: Separate frontend and backend services
- **Container Orchestration**: Docker Compose for multi-service setup
- **Message Patterns**: Producer/Consumer with Kafka
- **Resilience Patterns**: Circuit breaker, retry, and fallback strategies
- **Production Readiness**: Monitoring, health checks, and error handling

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is for educational purposes demonstrating non-blocking architecture patterns.

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Apache Kafka team for the robust messaging system
- Docker team for containerization tools
- All contributors and testers

---

**Happy Coding! 🚀**