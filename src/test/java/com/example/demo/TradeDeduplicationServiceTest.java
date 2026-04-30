package com.example.demo;

import com.example.demo.dto.TradeEvent;
import com.example.demo.messaging.TradeDeduplicationService;
import com.example.demo.service.TradeProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.support.Acknowledgment;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TradeDeduplicationService
 * Tests deduplication logic and Redis key handling for trade events.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TradeDeduplicationService Tests")
public class TradeDeduplicationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private TradeProcessor tradeProcessor;

    @Mock
    private Acknowledgment mockAcknowledgment;

    private TradeDeduplicationService tradeDeduplicationService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private TradeEvent testTradeEvent;
    private String testTradeJson;

    @BeforeEach
    public void setUp() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        tradeDeduplicationService = new TradeDeduplicationService(redisTemplate, tradeProcessor);

        testTradeEvent = new TradeEvent(
            "TRADE-001",
            "AAPL",
            100,
            150.50,
            "BUY",
            System.currentTimeMillis(),
            "CORR-001"
        );

        testTradeJson = objectMapper.writeValueAsString(testTradeEvent);
    }

    @Test
    @DisplayName("First trade should be processed successfully with Redis key set")
    public void testFirstTradeProcessedSuccessfully() {
        tradeDeduplicationService.consumeTradeEvent(testTradeJson, "trade-events", mockAcknowledgment);

        verify(tradeProcessor, times(1)).process(testTradeEvent);
        verify(mockAcknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("Duplicate trade should be skipped without processing")
    public void testDuplicateTradeSkipped() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true, false);

        tradeDeduplicationService.consumeTradeEvent(testTradeJson, "trade-events", mockAcknowledgment);
        reset(mockAcknowledgment, tradeProcessor);

        tradeDeduplicationService.consumeTradeEvent(testTradeJson, "trade-events", mockAcknowledgment);

        verify(tradeProcessor, never()).process(any());
        verify(mockAcknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("Exception during processing should delete Redis key for retry")
    public void testExceptionDeletesKeyForRetry() {
        doThrow(new RuntimeException("Processing failed")).when(tradeProcessor).process(testTradeEvent);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        tradeDeduplicationService.consumeTradeEvent(testTradeJson, "trade-events", mockAcknowledgment);

        verify(redisTemplate, times(1)).delete(anyString());
        verify(mockAcknowledgment, times(1)).acknowledge();
    }
}
