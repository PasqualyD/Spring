package com.example.demo.controller;

import com.example.demo.messaging.TradeDeduplicationService;
import com.example.demo.service.TradeProcessor;
import com.example.demo.dto.TradeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/trades/dedup")
public class TradeDeduplicationController {

    private static final Logger logger = LoggerFactory.getLogger(TradeDeduplicationController.class);

    @Autowired(required = false)
    private TradeDeduplicationService tradeDeduplicationService;

    @Autowired
    private TradeProcessor tradeProcessor;

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ResponseEntity<Map<String, Object>> kafkaUnavailable() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "unavailable");
        body.put("message", "Kafka not configured on this instance");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    /**
     * POST /api/trades/dedup/send-test
     */
    @PostMapping("/send-test")
    public ResponseEntity<Map<String, Object>> sendTestTradeEvent(@RequestBody TradeEvent tradeEvent) {
        if (kafkaTemplate == null) {
            return kafkaUnavailable();
        }
        try {
            String tradeJson = objectMapper.writeValueAsString(tradeEvent);
            kafkaTemplate.send("trade-events", tradeEvent.getTradeId(), tradeJson);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "sent");
            response.put("tradeId", tradeEvent.getTradeId());
            response.put("message", "Test trade event sent to Kafka");

            logger.info("Test trade event sent: {}", tradeEvent.getTradeId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error sending test trade event", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * GET /api/trades/dedup/check/{tradeId}
     */
    @GetMapping("/check/{tradeId}")
    public ResponseEntity<Map<String, Object>> checkTradeProcessed(@PathVariable String tradeId) {
        if (tradeDeduplicationService == null) {
            return kafkaUnavailable();
        }
        boolean isProcessed = tradeDeduplicationService.isTradeProcessed(tradeId);
        long ttl = tradeDeduplicationService.getDeduplicationKeyTTL(tradeId);

        Map<String, Object> response = new HashMap<>();
        response.put("tradeId", tradeId);
        response.put("isProcessed", isProcessed);
        response.put("ttlSeconds", ttl);
        response.put("processedCount", tradeProcessor.getProcessedTradesCount());

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/trades/dedup/{tradeId}
     */
    @DeleteMapping("/{tradeId}")
    public ResponseEntity<Map<String, Object>> deleteDeduplicationKey(@PathVariable String tradeId) {
        if (tradeDeduplicationService == null) {
            return kafkaUnavailable();
        }
        boolean deleted = tradeDeduplicationService.deleteDeduplicationKey(tradeId);

        Map<String, Object> response = new HashMap<>();
        response.put("tradeId", tradeId);
        response.put("deleted", deleted);
        response.put("message", deleted ?
            "Deduplication key deleted - trade can be reprocessed" :
            "Deduplication key not found");

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/trades/dedup/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "TradeDeduplicationService");
        response.put("kafkaEnabled", tradeDeduplicationService != null);
        response.put("kafkaConfigured", kafkaTemplate != null);
        response.put("processedTradesCount", tradeProcessor.getProcessedTradesCount());
        response.put("deduplicationMethod", "Redis Check-and-Set with 24-hour TTL");
        response.put("manualAcknowledgment", tradeDeduplicationService != null);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}
