package com.jpd.web.controller.creator;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.repository.PayoutTrackingRepository;
import com.jpd.web.service.PayPalPayoutServiceV2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/webhook")
@Slf4j
public class WebHookController {
	@Autowired
    private PayPalPayoutServiceV2 payoutService;
 

    // Helper method
    private boolean isPayoutEvent(String eventType) {
        return eventType != null && (
            eventType.startsWith("PAYMENT.PAYOUTSBATCH.") ||
            eventType.startsWith("PAYMENT.PAYOUTS-ITEM.")
        );
    }
    @PostMapping("/paypal")
    public ResponseEntity<?> handleWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader Map<String, String> headers) {
        
        try {
            log.info("=== Webhook received via Hookdeck ===");
            log.info("Headers: {}", headers);
            log.info("Payload: {}", payload);
            
            String eventType = (String) payload.get("event_type");
            
            if (eventType != null && isPayoutEvent(eventType)) {
                log.info("Processing payout webhook: {}", eventType);
                
                // Gọi service để xử lý webhook
                payoutService.updatePayoutStatusFromWebhook(payload);
                
                
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Webhook processed successfully",
                    "event_type", eventType,
                    "processed_at", java.time.LocalDateTime.now().toString()
                ));
            } else {
                log.info("Ignoring non-payout event: {}", eventType);
                return ResponseEntity.ok(Map.of(
                    "status", "ignored",
                    "message", "Not a payout-related event",
                    "event_type", eventType != null ? eventType : "UNKNOWN"
                ));
            }
            
        } catch (Exception e) {
            log.error("Webhook processing failed", e);
            
            // Vẫn return 200 để PayPal không retry
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", "Webhook processing failed: " + e.getMessage(),
                "error_time", java.time.LocalDateTime.now().toString()
            ));
        }
    }
    
}
