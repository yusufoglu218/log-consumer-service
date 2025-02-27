# Log Consumer Microservice

This microservice reads messages from Kafka and writes them to Redis.

## 📌 Project Description  
- Messages are consumed from Kafka using multiple threads.  
- Each message is **processed with a 500ms delay** before being stored in Redis.  
- Messages are stored in Redis with a **TTL of 1 minute**.  

## 📋 Requirements  
- Java 17+  
- Apache Kafka  
- Redis  
- Maven  
- Docker (optional, for running Kafka and Redis)  

## 🚀 Run the Application  

```bash
mvn spring-boot:run
