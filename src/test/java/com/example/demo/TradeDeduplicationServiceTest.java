package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for TradeDeduplicationService
 * Tests the Check-and-Set pattern, deduplication logic, and retry scenarios
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TradeDeduplicationService Tests")
public class TradeDeduplicationServiceTest {
    
    @Autowired
    private TradeDeduplicationService tradeDeduplicationService;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @MockBean
    private TradeProcessor tradeProcessor;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private TradeEvent testTradeEvent;
    private String testTradeJson;
    private Acknowledgment mockAcknowledgment;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Clean up Redis before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        
        // Create test trade event
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
        mockAcknowledgment = mock(Acknowledgment.class);
    }
    
    @Test
    @DisplayName("First trade should be processed successfully with Redis key set")
    public void testFirstTradeProcessedSuccessfully() {
        // Act
        tradeDeduplicationService.consumeTradeEvent(
            testTradeJson,
            "trade-events",
            mockAcknowledgment
        );
        
        // Assert
        verify(tradeProcessor, times(1)).process(testTradeEvent);
        verify(mockAcknowledgment, times(1)).acknowledge();
        
        // Verify Redis key was set with TTL
        assertTrue(tradeDeduplicationService.isTradeProcessed("TRADE-001"));
        long ttl = tradeDeduplicationService.getDeduplicationKeyTTL("TRADE-001");
        assertTrue(ttl > 0 && ttl <= 24 * 60 * 60);
    }
    
    @Test
    @DisplayName("Duplicate trade should be skipped without processing")
    public void testDuplicateTradeSkipped() {
        // Arrange - Process first trade
        tradeDeduplicationService.consumeTradeEvent(
            testTradeJson,
            "trade-events",
            mockAcknowledgment
        );
        
        reset(mockAcknowledgment, tradeProcessor);
        
        // Act - Process duplicate
        tradeDeduplicationService.consumeTradeEvent(
            testTradeJson,
            "trade-events",
            mockAcknowledgment
        );
        
        // Assert - Should NOT call processor for duplicate
        verify(tradeProcessor, never()).process(any());
        // Should still acknowledge to prevent Kafka retry
        verify(mockAcknowledgment, times(1)).acknowledge();
    }
    
    @Test
    @DisplayName("Exception during processing should delete Redis key for retry")
    public void testExceptionDeletesKeyForRetry() {
        // Arrange
        doThrow(new RuntimeException("Processing failed"))
            .when(tradeProcessor).process(testTradeEvent);
        
        // Act & Assert - Should throw exception
        assertThrows(RuntimeException.class, () -> {
            tradeDeduplicationService.consumeTradeEvent(
                testTradeJson,
                "trade-events",
                mockAcknowledgment
            );
        });
        
        // Verify Redis key was deleted for retry
        assertFalse(tradeDeduplicationService.isTradeProcessed("TRADE-001"));
        
        // Message should NOT be acknowledged
        verify(mockAcknowledgment, never()).acknowledge();
    }
    
    @Test
    @DisplayName("Check-and-Set atomicity: setIfAbsent returns true for new key")
    public void testSetIfAbsentAtomicity() {
        String key = "trade:dedup:TRADE-002";
        
        // First call should succeed (setIfAbsent)
        Boolean firstSet = redisTemplate.opsForValue().setIfAbsent(
            key,
            "TRADE-002",
            24 * 60 * 60,
            TimeUnit.SECONDS
        );
        assertTrue(firstSet, "First setIfAbsent should return true");
        
        // Second call should fail (key exists)
        Boolean secondSet = redisTemplate.opsForValue().setIfAbsent(
            key,
            "TRADE-002",
            24 * 60 * 60,
            TimeUnit.SECONDS
        );
        assertFalse(secondSet, "Second setIfAbsent should return false");
    }
    
    @Test
    @DisplayName("TTL is set correctly to 24 hours")
    public void testTTLSet24Hours() {
        String key = "trade:dedup:TRADE-003";
        
        redisTemplate.opsForValue().setIfAbsent(
            key,
            "TRADE-003",
            24 * 60 * 60,
            TimeUnit.SECONDS
        );
        
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 24 * 60 * 60, 
            "TTL should be within 24 hours");
    }
    
    @Test
    @DisplayName("Successful processing follows Check-and-Set pattern")
    public void testCheckAndSetPattern() throws Exception {
        // Arrange
        String tradeId = "TRADE-PATTERN-001";
        TradeEvent event = new TradeEvent(
            tradeId,
            "GOOGL",
            50,
            140.00,
            "SELL",
            System.currentTimeMillis(),
            "CORR-002"
        );
        String eventJson = objectMapper.writeValueAsString(event);
        
        // Act
        tradeDeduplicationService.consumeTradeEvent(
            eventJson,
            "trade-events",
            mockAcknowledgment
        );
        
        // Assert - Verify Check-and-Set behavior
        assertTrue(tradeDeduplicationService.isTradeProcessed(tradeId));
        verify(tradeProcessor, times(1)).process(event);
        verify(mockAcknowledgment, times(1)).acknowledge();
    }
    
    @Test
    @DisplayName("After retry (key deletion), duplicate should be processed")
    public void testRetryAfterKeyDeletion() throws Exception {
        // Arrange - First processing
        tradeDeduplicationService.consumeTradeEvent(
            testTradeJson,
            "trade-events",
            mockAcknowledgment
        );
        
        verify(tradeProcessor, times(1)).process(testTradeEvent);
        assertTrue(tradeDeduplicationService.isTradeProcessed("TRADE-001"));
        
        // Simulate exception - key gets deleted
        tradeDeduplicationService.deleteDeduplicationKey("TRADE-001");
        assertFalse(tradeDeduplicationService.isTradeProcessed("TRADE-001"));
        
        reset(mockAcknowledgment, tradeProcessor);
        
        // Act - Retry processing
        tradeDeduplicationService.consumeTradeEvent(
            testTradeJson,
            "trade-events",
            mockAcknowledgment
        );
        
        // Assert - Should process again
        verify(tradeProcessor, times(1)).process(testTradeEvent);
        verify(mockAcknowledgment, times(1)).acknowledge();
    }
    
    @Test
    @DisplayName("Manual deduplication key deletion works correctly")
    public void testManualKeyDeletion() {
        // Arrange
        tradeDeduplicationService.consumeTradeEvent(
            testTradeJson,
            "trade-events",
            mockAcknowledgment
        );
        
        assertTrue(tradeDeduplicationService.isTradeProcessed("TRADE-001"));
        
        // Act
        boolean deleted = tradeDeduplicationService.deleteDeduplicationKey("TRADE-001");
        
        // Assert
        assertTrue(deleted);
        assertFalse(tradeDeduplicationService.isTradeProcessed("TRADE-001"));
    }
}
