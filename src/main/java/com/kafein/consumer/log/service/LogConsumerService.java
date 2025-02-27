package com.kafein.consumer.log.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafein.common.model.LogMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LogConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(LogConsumerService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private int redisTtlTime;

    private final RedisTemplate<String, Object> redisTemplate;

    private final TaskExecutor taskExecutor;

    public LogConsumerService(RedisTemplate<String, Object> redisTemplate, TaskExecutor taskExecutor, @Value("${spring.redis.ttl}") int redisTtlTime) {
        this.redisTemplate = redisTemplate;
        this.taskExecutor = taskExecutor;
        this.redisTtlTime = redisTtlTime;
    }


    /**
     * Listens to Kafka and processes messages asynchronously.
     */
    @KafkaListener(
            topics = "${spring.kafka.topic.name}",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "${spring.kafka.consumer.concurrency}"
    )
    public void consume(ConsumerRecord<String, String> consumerRecord) {
        taskExecutor.execute(() -> processRecord(consumerRecord));
    }

    /**
     * Main method to process the Kafka message asynchronously.
     * @param consumerRecord The Kafka consumerRecord containing the message
     */
    @Async
    public void processRecord(ConsumerRecord<String, String> consumerRecord) {
        logger.info("Thread: {} - Processing message from Kafka: {}", Thread.currentThread().getName(), consumerRecord.key());

        try {
            TimeUnit.MILLISECONDS.sleep(500);
            processMessage(consumerRecord.value());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Error during processing message", e);
        }

        logger.info("Thread: {} - Message consumed and processed: {}", Thread.currentThread().getName(), consumerRecord.key());
    }

    /**
     * Converts the received Kafka message into a LogMessage object and sends it to Redis.
     * @param message The Kafka message as a string
     */
    public void processMessage(String message) {
        try {
            LogMessage logMessage = objectMapper.readValue(message, LogMessage.class);
            sendToRedis(logMessage);
        } catch (JsonProcessingException e) {
            logger.error("Error during converting the message to LogMessage object:", e);
        }
    }


    /**
     * Sends the LogMessage to Redis with a 1-minute expiration.
     * @param logMessage The LogMessage object to be sent
     */
    public void sendToRedis(LogMessage logMessage) {
        redisTemplate.opsForValue().set(logMessage.getEventId(), logMessage, redisTtlTime, TimeUnit.MINUTES);
        logger.info("Message sent to Redis: {}", logMessage);
    }
}


