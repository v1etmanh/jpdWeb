package com.jpd.web.controller.customer;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.service.PayPalService;
import com.paypal.orders.LinkDescription;
import com.paypal.orders.Order;

import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/paypal")
public class PayPalController {
    
    @Autowired
    private PayPalService payPalService;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @PostMapping("/create-order/{courseId}")
    public ResponseEntity<?> createOrder(
         @Positive   @RequestParam("amount") double amount,
         @Positive   @PathVariable("courseId") long courseId,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            // Lấy customer ID từ JWT
       
            long customerId = customerRepository
                .findByEmail(jwt.getClaimAsString("email"))
                .orElseThrow(() -> new RuntimeException("Customer not found"))
                .getCustomerId();
            
            // Tạo order qua service (service sẽ tự tạo description và currency)
            Order order = payPalService.createOrderWithTracking(amount, courseId, customerId);
            
            // Tìm approval URL
            String approvalUrl = extractApprovalUrl(order);
             
            return ResponseEntity.ok().body(Map.of(
                "status", "success",
                "order_id", order.id(),
                "approval_url", approvalUrl,
                "message", "Order created successfully"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/capture-order/{orderId}")
    public ResponseEntity<?> captureOrder(@PathVariable String orderId) {
        try {
            Order order = payPalService.captureOrderAndEnroll(orderId);
            
            return ResponseEntity.ok().body(Map.of(
                "status", "success",
                "order_id", order.id(),
                "order_status", order.status(),
                "message", "Payment captured successfully"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/success")
    public ResponseEntity<?> paymentSuccess(@RequestParam("token") String orderId) {
        try {
            Order order = payPalService.captureOrderAndEnroll(orderId);
            
            return ResponseEntity.ok().body(Map.of(
                "status", "success",
                "message", "Payment completed successfully!"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/cancel")
    public ResponseEntity<?> paymentCancel(@RequestParam("token") String orderId) {
        payPalService.cancelOrder(orderId);
        
        return ResponseEntity.ok().body(Map.of(
            "status", "cancelled",
            "message", "Payment was cancelled"
        ));
    }
    
    /**
     * Helper method để extract approval URL từ PayPal order
     */
    private String extractApprovalUrl(Order order) {
        for (LinkDescription link : order.links()) {
            if ("approve".equals(link.rel())) {
                return link.href();
            }
        }
        return "";
    }
}