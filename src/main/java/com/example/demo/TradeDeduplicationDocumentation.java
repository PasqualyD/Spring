package com.example.demo;

/**
 * TRADE DEDUPLICATION SERVICE - IMPLEMENTATION GUIDE
 * 
 * This module implements a robust trade deduplication service using Kafka and Redis.
 * It demonstrates the Check-and-Set pattern with atomic operations and proper error handling.
 * 
 * KEY COMPONENTS:
 * ================
 * 
 * 1. TradeEvent (TradeEvent.java)
 *    - Message model for trades consumed from Kafka
 *    - Contains: tradeId, symbol, quantity, price, side, timestamp, correlationId
 * 
 * 2. TradeDeduplicationService (TradeDeduplicationService.java)
 *    - Core deduplication logic using Redis Check-and-Set pattern
 *    - Kafka listener consumes events from 'trade-events' topic
 *    - Uses StringRedisTemplate.opsForValue().setIfAbsent() for atomic operations
 *    - 24-hour TTL on Redis keys
 *    - Manual Kafka acknowledgment for exactly-once processing
 * 
 * 3. TradeProcessor (TradeProcessor.java)
 *    - Validates and processes trade events
 *    - Simulates business logic (validation, persistence, market data updates)
 * 
 * 4. Configuration Classes
 *    - KafkaTradeConfig: Kafka consumer setup with manual acknowledgment
 *    - RedisTradeConfig: Redis connection and StringRedisTemplate
 * 
 * 5. TradeDeduplicationController
 *    - REST endpoints for monitoring and testing
 * 
 * ================================================================================
 * DEDUPLICATION LOGIC - CHECK-AND-SET PATTERN
 * ================================================================================
 * 
 * FLOW:
 * -----
 * 1. Message arrives from Kafka:
 *    consumeTradeEvent(payload, topic, partition, offset, acknowledgment)
 * 
 * 2. Parse TradeEvent JSON and create Redis key:
 *    key = "trade:dedup:" + tradeId
 * 
 * 3. ATOMIC CHECK-AND-SET OPERATION:
 *    Boolean keySet = redisTemplate.opsForValue().setIfAbsent(
 *        key,                    // Redis key
 *        tradeId,               // Value
 *        24 * 60 * 60,          // 24-hour TTL
 *        TimeUnit.SECONDS       // TimeUnit
 *    );
 * 
 *    This operation is ATOMIC - only ONE thread can set this key successfully.
 * 
 * ================================================================================
 * 4 SCENARIOS
 * ================================================================================
 * 
 * SCENARIO 1: FIRST OCCURRENCE (NEW TRADE)
 * -----------------------------------------
 * 1. Redis key does NOT exist
 * 2. setIfAbsent() returns TRUE
 * 3. Call tradeProcessor.process(event)
 * 4. On success, call acknowledgment.acknowledge()
 * 5. Kafka offset advances, message is not replayed
 * 
 * LOG OUTPUT:
 *   [INFO] Received trade event from Kafka - TradeID: TRADE-001
 *   [INFO] Trade is new (dedup key set in Redis) - Processing trade: TRADE-001
 *   [INFO] Trade processed successfully: TRADE-001
 *   [INFO] Trade processed and Kafka message acknowledged: TRADE-001
 * 
 * 
 * SCENARIO 2: DUPLICATE WITHIN 24 HOURS
 * ----------------------------------------
 * 1. Redis key already EXISTS (from previous processing)
 * 2. setIfAbsent() returns FALSE (key not set)
 * 3. DO NOT call tradeProcessor.process()
 * 4. Call acknowledgment.acknowledge() to skip the duplicate
 * 5. Kafka offset advances, but trade is not processed twice
 * 
 * LOG OUTPUT:
 *   [INFO] Received trade event from Kafka - TradeID: TRADE-001
 *   [WARN] Duplicate trade received (dedup key exists in Redis) - TradeID: TRADE-001
 *   [INFO] Duplicate trade acknowledged and skipped: TRADE-001
 * 
 * 
 * SCENARIO 3: EXCEPTION DURING PROCESSING
 * -------------------------------------------
 * 1. Redis key set successfully (first time)
 * 2. Call tradeProcessor.process(event)
 * 3. Processing throws exception
 * 4. CATCH THE EXCEPTION and DELETE Redis key: redisTemplate.delete(key)
 * 5. DO NOT acknowledge the message
 * 6. Kafka redelivers message after backoff (retry)
 * 7. On retry, Redis key is deleted, so trade can be processed again
 * 
 * LOG OUTPUT (FIRST ATTEMPT):
 *   [INFO] Received trade event from Kafka - TradeID: TRADE-002
 *   [INFO] Trade is new (dedup key set in Redis) - Processing trade: TRADE-002
 *   [ERROR] Error processing trade TRADE-002: Database connection failed. Deleting dedup key for retry.
 *   [INFO] Deduplication key deleted for trade: TRADE-002
 *   (No acknowledgment sent - message not removed from Kafka)
 * 
 * LOG OUTPUT (RETRY AFTER BACKOFF):
 *   [INFO] Received trade event from Kafka - TradeID: TRADE-002
 *   [INFO] Trade is new (dedup key set in Redis) - Processing trade: TRADE-002
 *   [INFO] Trade processed successfully: TRADE-002
 *   [INFO] Trade processed and Kafka message acknowledged: TRADE-002
 * 
 * 
 * SCENARIO 4: REDIS KEY EXPIRES (AFTER 24 HOURS)
 * ------------------------------------------------
 * 1. Trade processed at 2024-01-01 10:00 AM
 * 2. Redis key set with 24-hour TTL
 * 3. At 2024-01-02 10:00 AM, TTL expires and key is automatically deleted
 * 4. If same trade arrives again, it will be processed as new (setIfAbsent returns TRUE)
 * 5. This allows reprocessing if needed after the TTL window
 * 
 * ================================================================================
 * ATOMICITY AND THREAD SAFETY
 * ================================================================================
 * 
 * The setIfAbsent operation is ATOMIC in Redis:
 * - Command: SET key value NX EX 86400
 *   - NX = Only set if Not eXists
 *   - EX = Expire in X seconds
 * - Redis executes this as a SINGLE atomic operation
 * - No race conditions between check and set
 * - Multiple processes/instances can safely call this concurrently
 * 
 * Benefits:
 * - NO two threads will both get TRUE from setIfAbsent
 * - NO duplicate processing even with multiple instances
 * - NO need for distributed locks
 * 
 * ================================================================================
 * MANUAL KAFKA ACKNOWLEDGMENT
 * ================================================================================
 * 
 * Why Manual Acknowledgment?
 * - Ensures "exactly-once" processing semantics
 * - Gives us control over when to commit offset
 * 
 * Config:
 * - spring.kafka.consumer.enable-auto-commit=false
 * - factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL)
 * 
 * Flow:
 * 1. Message consumed from Kafka
 * 2. Deduplication check performed
 * 3. If new: process trade, then acknowledge
 * 4. If duplicate: acknowledge without processing
 * 5. If error: DON'T acknowledge (triggers redelivery)
 * 
 * ================================================================================
 * REST API ENDPOINTS
 * ================================================================================
 * 
 * POST /api/trades/dedup/send-test
 *   - Send a test trade event to Kafka
 *   - Body: { "tradeId": "TRADE-001", "symbol": "AAPL", "quantity": 100, ... }
 * 
 * GET /api/trades/dedup/check/{tradeId}
 *   - Check if trade has been processed
 *   - Returns: { "isProcessed": true, "ttlSeconds": 86340, ... }
 * 
 * DELETE /api/trades/dedup/{tradeId}
 *   - Delete dedup key (for testing retry scenarios)
 *   - Use this to simulate retry after exception
 * 
 * GET /api/trades/dedup/status
 *   - Get service status and configuration
 * 
 * ================================================================================
 * TESTING SCENARIOS
 * ================================================================================
 * 
 * Test 1: First Trade Processing
 * ---- 
 * POST /api/trades/dedup/send-test
 * Body: { "tradeId": "TEST-001", "symbol": "AAPL", "quantity": 100, "price": 150.0, 
 *         "side": "BUY", "timestamp": 1704096000000, "correlationId": "CORR-001" }
 * 
 * GET /api/trades/dedup/check/TEST-001
 * Expected: { "isProcessed": true, "ttlSeconds": 86340 }
 * 
 * 
 * Test 2: Duplicate Trade (Send same message again)
 * ----
 * POST /api/trades/dedup/send-test
 * Body: { same as Test 1 }
 * 
 * GET /api/trades/dedup/check/TEST-001
 * Expected: { "isProcessed": true, "ttlSeconds": 86300 } (TTL decreased)
 * 
 * Check logs: Should show "Duplicate trade received" message
 * Verify: Trade processor NOT called a second time
 * 
 * 
 * Test 3: Retry After Exception
 * ----
 * DELETE /api/trades/dedup/TEST-001
 * GET /api/trades/dedup/check/TEST-001
 * Expected: { "isProcessed": false, "ttlSeconds": -1 }
 * 
 * POST /api/trades/dedup/send-test
 * Body: { same as Test 1 }
 * 
 * GET /api/trades/dedup/check/TEST-001
 * Expected: { "isProcessed": true, "ttlSeconds": 86340 } (Fresh TTL)
 * 
 * ================================================================================
 * DEPENDENCIES (Maven)
 * ================================================================================
 * 
 * <dependency>
 *     <groupId>org.springframework.kafka</groupId>
 *     <artifactId>spring-kafka</artifactId>
 * </dependency>
 * 
 * <dependency>
 *     <groupId>org.springframework.boot</groupId>
 *     <artifactId>spring-boot-starter-data-redis</artifactId>
 * </dependency>
 * 
 * <dependency>
 *     <groupId>io.lettuce</groupId>
 *     <artifactId>lettuce-core</artifactId>
 * </dependency>
 * 
 * ================================================================================
 * PREREQUISITES (LOCAL SETUP)
 * ================================================================================
 * 
 * 1. Start Kafka:
 *    docker-compose up -d kafka zookeeper
 * 
 * 2. Create Kafka topic:
 *    kafka-topics --create --topic trade-events --bootstrap-server localhost:9092
 * 
 * 3. Start Redis:
 *    docker run -d -p 6379:6379 redis:latest
 * 
 * 4. Start Spring Boot application:
 *    mvn spring-boot:run
 * 
 * ================================================================================
 */
public class TradeDeduplicationDocumentation {
    // This class serves as documentation only
}
