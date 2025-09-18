# 🚀 Payment System - Non-Blocking Architecture

A comprehensive monorepo demonstrating asynchronous payment processing using Spring Boot, Apache Kafka, and WebSockets. This project showcases non-blocking architecture patterns with real-time updates.

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

## 📁 Project Structure

```
paymentswithkafka/
├── payment-service/          # Spring Boot Payment API
│   ├── src/main/java/
│   │   └── com/example/demo/
│   │       ├── controller/   # REST Controllers
│   │       ├── service/      # Kafka Producer/Consumer
│   │       ├── model/        # Payment Models
│   │       ├── dto/          # Data Transfer Objects
│   │       └── config/       # WebSocket Configuration
│   └── src/main/resources/
│       └── application.properties
├── frontend/                 # React-like Frontend
│   └── index.html           # Single Page Application
├── docker-compose.yml       # Multi-service Setup
└── README.md
```

## 🚀 Quick Start

### 1. Start All Services

```bash
# Start Kafka infrastructure and frontend
docker-compose up -d

# Check service status
docker-compose ps
```

### 2. Run Payment Service

```bash
cd payment-service
mvn spring-boot:run
```

### 3. Access Applications

- **Frontend**: http://localhost:3000 - Interactive payment interface
- **Payment API**: http://localhost:8081 - REST API endpoints
- **Kafka UI**: http://localhost:8080 - Kafka management interface
- **API Health**: http://localhost:8081/api/payments/health

## 🎯 Key Features

### Non-Blocking Architecture
- **Immediate Response**: Payment requests return instantly with a pending status
- **Asynchronous Processing**: Kafka handles payment processing in the background
- **Real-time Updates**: WebSocket provides live status updates to the frontend

### Payment Flow
1. **Create Payment**: POST to `/api/payments` returns immediately
2. **Queue Processing**: Payment is sent to Kafka for async processing
3. **Status Updates**: WebSocket broadcasts status changes in real-time
4. **Event Publishing**: Payment events are published to Kafka topics

### Technology Stack
- **Backend**: Spring Boot 3.5.5, Java 21
- **Message Queue**: Apache Kafka 7.4.0
- **Real-time**: WebSocket with STOMP
- **Frontend**: Vanilla JavaScript with SockJS
- **Infrastructure**: Docker Compose

## 🔧 Services & Ports

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 3000 | Payment UI with real-time updates |
| Payment API | 8081 | Spring Boot REST API |
| Kafka UI | 8080 | Kafka management interface |
| Kafka Broker | 9092 | Message broker |
| Zookeeper | 2181 | Kafka coordination |
| Schema Registry | 8082 | Avro schema management |

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
```

### WebSocket
```javascript
// Connect to WebSocket
const socket = new SockJS('http://localhost:8081/ws');
const stompClient = new StompJs.Client({webSocketFactory: () => socket});

// Subscribe to payment updates
stompClient.subscribe('/topic/payments/user123', (message) => {
    const payment = JSON.parse(message.body);
    // Handle real-time payment updates
});
```

## 🎮 Demo the Non-Blocking Behavior

1. **Open Frontend**: Navigate to http://localhost:3000
2. **Create Payment**: Fill out the form and submit
3. **Observe**: 
   - Immediate response with "PENDING" status
   - Real-time status updates via WebSocket
   - Background processing simulation (1-5 seconds)
   - Final status: COMPLETED or FAILED

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

### Kafka UI
- Access http://localhost:8080
- Monitor topics, consumers, and messages
- View message content and headers

## 🛠️ Development

### Adding New Features
1. **Models**: Add to `payment-service/src/main/java/com/example/demo/model/`
2. **Controllers**: Add to `payment-service/src/main/java/com/example/demo/controller/`
3. **Services**: Add to `payment-service/src/main/java/com/example/demo/service/`
4. **Frontend**: Modify `frontend/index.html`

### Configuration
- **Kafka**: `payment-service/src/main/resources/application.properties`
- **Docker**: `docker-compose.yml`
- **Topics**: Auto-created, configurable in application.properties

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
- **WebSocket Integration**: Real-time bidirectional communication

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test with `docker-compose up -d`
5. Submit a pull request

## 📄 License

This project is for educational purposes demonstrating non-blocking architecture patterns.
