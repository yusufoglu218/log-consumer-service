package com.kafein.consumer.log.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafein.common.model.LogMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class LogConsumerServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;


    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private TaskExecutor taskExecutor;

    @Mock
    private ObjectMapper objectMapper;

    private LogConsumerService logConsumerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        logConsumerService = new LogConsumerService(redisTemplate, taskExecutor,1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testConsume_Success() throws JsonProcessingException {
        String message = "{\"eventId\": \"12345\", \"payload\": \"Test message\", \"timestamp\": 1234567890}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("log-topic", 0, 0, "12345", message);

        LogMessage logMessage = new LogMessage();
        logMessage.setEventId("12345");
        logMessage.setPayload("Test message");
        logMessage.setTimestamp(1234567890);

        when(objectMapper.readValue(any(String.class), eq(LogMessage.class))).thenReturn(logMessage);

        logConsumerService.consume(record);

        verify(taskExecutor, times(1)).execute(any(Runnable.class));
    }

    @Test
    void testProcessMessage_Success() throws JsonProcessingException {
        // Prepare valid test data
        String validMessage = "{\"eventId\": \"12345\", \"payload\": \"Test message\", \"timestamp\": 1234567890}";
        LogMessage logMessage = new LogMessage();
        logMessage.setEventId("12345");
        logMessage.setPayload("Test message");
        logMessage.setTimestamp(1234567890);

        when(objectMapper.readValue(any(String.class), eq(LogMessage.class))).thenReturn(logMessage);

        logConsumerService.processMessage(validMessage);

        verify(valueOperations, times(1)).set(eq(logMessage.getEventId()), eq(logMessage), eq(1L), eq(TimeUnit.MINUTES));
    }

    @Test
    void testProcessMessage_JsonProcessingException() throws JsonProcessingException {
        String invalidMessage = "{\"eventId\": \"12345\", \"payload\": \"Test message\"";  // Invalid JSON

        when(objectMapper.readValue(any(String.class), eq(LogMessage.class))).thenThrow(JsonProcessingException.class);

        logConsumerService.processMessage(invalidMessage);

        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void testSendToRedis() {
        // Prepare test LogMessage
        LogMessage logMessage = new LogMessage();
        logMessage.setEventId("12345");
        logMessage.setPayload("Test message");
        logMessage.setTimestamp(1234567890);

        // Call the method directly
        logConsumerService.sendToRedis(logMessage);

        // Verify that the message was sent to Redis with the correct TTL
        verify(valueOperations, times(1)).set(eq(logMessage.getEventId()), eq(logMessage), eq(1L), eq(TimeUnit.MINUTES));
    }
}
