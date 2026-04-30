package com.example.demo.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.dto.TradeEvent;
import com.example.demo.service.TradeProcessor;

import java.util.concurrent.TimeUnit;

/**
 * Service for consuming TradeEvent from Kafka and implementing deduplication
 * using Redis Check-and-Set pattern with setIfAbsent and TTL
 */
@Service
public class TradeDeduplicationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TradeDeduplicationService.class);
    
    private static final String TRADE_DEDUP_KEY_PREFIX = "trade:dedup:";
    private static final long TTL_24_HOURS = 24 * 60 * 60;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private TradeProcessor tradeProcessor;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Kafka listener for trade events
     */
    @KafkaListener(
        topics = "trade-events",
        groupId = "trade-dedup-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTradeEvent(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        TradeEvent tradeEvent = null;
        String deduplicationKey = null;
        
        try {
            tradeEvent = objectMapper.readValue(payload, TradeEvent.class);
            deduplicationKey = TRADE_DEDUP_KEY_PREFIX + tradeEvent.getTradeId();
            
            logger.info("Received trade event from Kafka - TradeID: {}, Topic: {}",
                    tradeEvent.getTradeId(), topic);
            
            // Check-and-Set pattern using setIfAbsent with TTL
            Boolean keySet = redisTemplate.opsForValue().setIfAbsent(
                    deduplicationKey,
                    tradeEvent.getTradeId(),
                    TTL_24_HOURS,
                    TimeUnit.SECONDS
            );
            
            if (Boolean.TRUE.equals(keySet)) {
                logger.info("Trade is new (dedup key set in Redis) - Processing trade: {}", 
                        tradeEvent.getTradeId());
                
                try {
                    tradeProcessor.process(tradeEvent);
                    acknowledgment.acknowledge();
                    logger.info("Trade processed and Kafka message acknowledged: {}", 
                            tradeEvent.getTradeId());
                    
                } catch (Exception e) {
                    logger.error("Error processing trade {}: {}. Deleting dedup key for retry.",
                            tradeEvent.getTradeId(), e.getMessage(), e);
                    
                    redisTemplate.delete(deduplicationKey);
                    throw new RuntimeException("Trade processing failed - key deleted for retry", e);
                }
            } else {
                logger.warn("Duplicate trade received - TradeID: {}, CorrelationID: {}.",
                        tradeEvent.getTradeId(), tradeEvent.getCorrelationId());
                
                acknowledgment.acknowledge();
                logger.info("Duplicate trade acknowledged and skipped: {}", tradeEvent.getTradeId());
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error in trade deduplication handler: {}", e.getMessage(), e);
            
            if (deduplicationKey != null && tradeEvent != null) {
                redisTemplate.delete(deduplicationKey);
            }
            
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        }
    }
    
    /**
     * Manually check if a trade has been processed
     */
    public boolean isTradeProcessed(String tradeId) {
        String deduplicationKey = TRADE_DEDUP_KEY_PREFIX + tradeId;
        Boolean exists = redisTemplate.hasKey(deduplicationKey);
        return exists != null && exists;
    }
    
    /**
     * Get the TTL of a deduplication key
     */
    public long getDeduplicationKeyTTL(String tradeId) {
        String deduplicationKey = TRADE_DEDUP_KEY_PREFIX + tradeId;
        Long ttl = redisTemplate.getExpire(deduplicationKey, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }
    
    /**
     * Manually delete a deduplication key
     */
    public boolean deleteDeduplicationKey(String tradeId) {
        String deduplicationKey = TRADE_DEDUP_KEY_PREFIX + tradeId;
        Boolean deleted = redisTemplate.delete(deduplicationKey);
        boolean wasDeleted = deleted != null && deleted;
        logger.info("Deduplication key deleted for trade: {}", tradeId);
        return wasDeleted;
    }
}
