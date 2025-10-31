package com.jpd.web.controller.customer;

import com.jpd.web.exception.ResourceNotFoundException;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/vnpay")
@Validated
public class VNPayController {

    private final CustomerRepository customerRepository;
    private final VNPayService vnPayService;

    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("uuuuMMddHHmmss");

    public VNPayController(CustomerRepository customerRepository, VNPayService vnPayService) {
        this.customerRepository = customerRepository;
        this.vnPayService = vnPayService;
    }

    @PostMapping("/create-order/{courseId}")
    public ResponseEntity<?> createOrder(
            @Positive @RequestParam("amount") long amount,
            @Positive @PathVariable("courseId") long courseId,
            @AuthenticationPrincipal Jwt jwt
            ) {
    	String customerEmail=jwt.getClaimAsString("email");
        try {
            log.info("Creating VNPay order: courseId={}, amount={}, customerEmail={}", courseId, amount);
           
            long customerId = customerRepository.findByEmail(customerEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"))
                    .getCustomerId();

            String vnpayUrl = vnPayService.createOrderWithTracking(amount, courseId, customerId);

            log.info("VNPay order created for customerId={} -> url generated", customerId);
            return ResponseEntity.ok(Map.of("paymentUrl", vnpayUrl));
        } catch (ResourceNotFoundException rnfe) {
            log.warn("Customer not found for email {}", customerEmail);
            return ResponseEntity.status(404).body(Map.of("error", "CustomerNotFound"));
        } catch (ValidationException ve) {
            log.warn("Validation error: {}", ve.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "InvalidRequest"));
        } catch (Exception e) {
            log.error("Failed to create VNPay order", e);
            return ResponseEntity.status(500).body(Map.of("error", "ServerError"));
        }
    }

    @GetMapping("/vnpay-payment")
    public ResponseEntity<?> handleVnPayCallback(
            @RequestParam("vnp_TxnRef") String vnpTxnRef,
            @RequestParam(value = "vnp_OrderInfo", required = false) String orderInfo,
            @RequestParam(value = "vnp_PayDate", required = false) String vnpPayDate,
            @RequestParam(value = "vnp_TransactionNo", required = false) String transactionNo,
            @RequestParam(value = "vnp_Amount", required = false) String vnpAmount, HttpServletRequest request) {

        try {
            log.info("VNPay callback received: txnRef={}", vnpTxnRef);

            int payStatus = vnPayService.captureOrderAndEnroll(vnpTxnRef, request);

            LocalDateTime paymentTime = null;
            if (vnpPayDate != null && !vnpPayDate.isBlank()) {
                try {
                    paymentTime = LocalDateTime.parse(vnpPayDate, VNPAY_DATE_FORMAT);
                } catch (DateTimeParseException dtpe) {
                    log.warn("Unable to parse vnp_PayDate='{}'", vnpPayDate);
                    return ResponseEntity.badRequest().body(Map.of("error", "InvalidPayDate"));
                }
            }

            Long parsedTransactionId = null;
            if (transactionNo != null && !transactionNo.isBlank()) {
                try {
                    parsedTransactionId = Long.parseLong(transactionNo);
                } catch (NumberFormatException nfe) {
                    log.warn("Invalid transaction number '{}'", transactionNo);
                    return ResponseEntity.badRequest().body(Map.of("error", "InvalidTransactionNo"));
                }
            }

            Long parsedAmount = null;
            if (vnpAmount != null && !vnpAmount.isBlank()) {
                try {
                    parsedAmount = Long.parseLong(vnpAmount);
                } catch (NumberFormatException nfe) {
                    log.warn("Invalid amount '{}'", vnpAmount);
                    return ResponseEntity.badRequest().body(Map.of("error", "InvalidAmount"));
                }
            }

            if (payStatus != 1) {
                log.error("Payment status not successful: status={}", payStatus);
                return ResponseEntity.badRequest().body(Map.of("status", "PaymentFailed"));
            }

            log.info("VNPay payment successful for txnRef={}", vnpTxnRef);
            return ResponseEntity.ok(Map.of(
                    "status", "PaymentSuccess",
                    "orderInfo", orderInfo,
                    "paymentTime", paymentTime,
                    "transactionId", parsedTransactionId,
                    "totalPrice", parsedAmount
            ));
        } catch (Exception e) {
            log.error("Error handling VNPay callback for txnRef={}", vnpTxnRef, e);
            return ResponseEntity.status(500).body(Map.of("status", "PaymentFailed"));
        }
    }

}