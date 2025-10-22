package com.jpd.web.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.jpd.web.model.*;
import com.jpd.web.repository.WithdrawRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint.LastExecution;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicate;
import com.jpd.web.config.PayPalV2Config;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.PayoutTrackingRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PayPalPayoutServiceV2 {

	@Autowired
	private CreatorRepository creatorRepository;
	@Autowired
	private PayPalV2Config paypalConfig;

	@Autowired
	private PayPalAccessTokenService tokenService;

	@Autowired
	private PayoutTrackingRepository payoutRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
	private WithdrawRepository withdrawRepository;
	
	public boolean  isMax(Creator creator) {
		List<PayoutTracking>lpay= creator.getPayoutTrackings();
		if(!lpay.isEmpty()) {
			LocalDateTime startDate=LocalDate.now().atStartOfDay();
			//Predicate laf thuoc ve hom nay
			long count=lpay.stream()
					.filter(b->b.getCreatedAt().isAfter(startDate))
					.count();
			return count>=3;
		}
		return false;
	}
	

	public PayoutTracking createSinglePayout(String recipientEmail, double amount, String currency, String note,Creator creator,TargetPayout targetPayout)
			throws Exception {

		String accessToken = tokenService.getAccessToken();
		String batchId = "batch_" + System.currentTimeMillis();

		// Tạo request body - chỉ cần 1 item trong batch
		Map<String, Object> payoutRequest = new HashMap<>();

		// Sender batch header
		Map<String, Object> senderBatchHeader = new HashMap<>();
		senderBatchHeader.put("sender_batch_id", batchId);
		senderBatchHeader.put("email_subject", "You have a payment");
		senderBatchHeader.put("email_message", "You have received a payment. Thank you!");
		payoutRequest.put("sender_batch_header", senderBatchHeader);
		
		// Items - chỉ có 1 item
		List<Map<String, Object>> items = new ArrayList<>();
		Map<String, Object> item = new HashMap<>();
		item.put("recipient_type", "EMAIL");
		item.put("amount", Map.of("value", String.valueOf(amount), "currency", currency));
		item.put("receiver", recipientEmail);
		item.put("note", note);
		item.put("sender_item_id", "item_" + System.currentTimeMillis());
		items.add(item);
		payoutRequest.put("items", items);

		// Gửi request
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", "Bearer " + accessToken);
		headers.add("PayPal-Request-Id", "payout-" + System.currentTimeMillis());

		HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payoutRequest), headers);

		log.info("Creating single payout: {} {} to {} with batch_id: {}", amount, currency, recipientEmail, batchId);

		ResponseEntity<Map> response = restTemplate.postForEntity(paypalConfig.getBaseUrl() + "/v1/payments/payouts",
				request, Map.class);

		if (response.getStatusCode() == HttpStatus.CREATED) {
			Map<String, Object> responseBody = response.getBody();
			log.info("PayPal Response: {}", responseBody);

			// Lấy batch ID từ response
			Map<String, Object> batchHeader = (Map<String, Object>) responseBody.get("batch_header");
			String payoutBatchId = null;
			if (batchHeader != null) {
				payoutBatchId = (String) batchHeader.get("payout_batch_id");
				log.info("PayPal payout_batch_id: {}", payoutBatchId);
			}

			// Sử dụng batch ID từ PayPal hoặc fallback
			String finalBatchId = payoutBatchId != null ? payoutBatchId : batchId;

			// Lưu vào database - chỉ cần batch_id
			PayoutTracking tracking = new PayoutTracking(finalBatchId, recipientEmail, amount, currency, note);
			tracking.setStatus("PENDING");
			// Lưu sender batch id để reference
			tracking.setCreator(creator);
			tracking.setTargetPayout(targetPayout);
			payoutRepository.save(tracking);

			log.info("Saved single payout tracking with batch_id: {}", finalBatchId);

			return tracking;

		} else {
			log.error("Single payout creation failed with status: {}, body: {}", response.getStatusCode(), response.getBody());
			throw new RuntimeException("Payout creation failed: " + response.getBody());
		}
	}

	public Map<String, Object> getPayoutStatus(String batchId) throws Exception {
		String accessToken = tokenService.getAccessToken();

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", "Bearer " + accessToken);

		HttpEntity<String> request = new HttpEntity<>(headers);

		log.debug("Fetching payout status for batch: {}", batchId);

		ResponseEntity<Map> response = restTemplate.exchange(
				paypalConfig.getBaseUrl() + "/v1/payments/payouts/" + batchId, HttpMethod.GET, request, Map.class);

		if (response.getStatusCode() == HttpStatus.OK) {
			return response.getBody();
		} else {
			throw new RuntimeException("Failed to get payout status: " + response.getBody());
		}
	}

	public void updatePayoutStatus(String batchId) throws Exception {
		log.info("Updating single payout status for batch: {}", batchId);

		Map<String, Object> batchInfo = getPayoutStatus(batchId);

		// Lấy status từ batch header
		Map<String, Object> batchHeader = (Map<String, Object>) batchInfo.get("batch_header");
		String batchStatus = null;
		if (batchHeader != null) {
			batchStatus = (String) batchHeader.get("batch_status");
		}

		// Lấy status từ item đầu tiên (vì chỉ có 1 item)
		List<Map<String, Object>> items = (List<Map<String, Object>>) batchInfo.get("items");
		String itemStatus = null;
		if (items != null && !items.isEmpty()) {
			Map<String, Object> firstItem = items.get(0);
			itemStatus = (String) firstItem.get("transaction_status");
		}

		// Ưu tiên item status, fallback to batch status
		String finalStatus = itemStatus != null ? itemStatus : batchStatus;

		log.info("Batch {} - batch_status: {}, item_status: {}, final_status: {}", 
				batchId, batchStatus, itemStatus, finalStatus);

		// Update trong database
		Optional<PayoutTracking> trackingOpt = payoutRepository.findByPayoutBatchId(batchId);
		if (trackingOpt.isPresent()) {
			PayoutTracking tracking = trackingOpt.get();

			if (finalStatus != null &&!finalStatus.equals("FAILED")&&!finalStatus.equals("CANCELLED")) {
				String oldStatus = tracking.getStatus();
				tracking.setStatus(finalStatus);
				tracking.setUpdatedAt(LocalDateTime.now());
				payoutRepository.save(tracking);
                if(trackingOpt.get().getTargetPayout()== TargetPayout.VERIFY_EMAIL) {
					Creator c = tracking.getCreator();
					c.setPaymentEmail(tracking.getRecipientEmail());
                    
					creatorRepository.save(c);
				}
				else if(trackingOpt.get().getTargetPayout() == TargetPayout.WITHDRAW) {
					Optional<Withdraw> withdraw=withdrawRepository.findByPayoutBatchId(trackingOpt.get().getPayoutBatchId());
					if(withdraw.isPresent()) {
						withdraw.get().setStatus(Status.SUCCESS);
						
						withdrawRepository.save(withdraw.get());
					}
				}
                
				log.info("Updated single payout batch {} status: {} -> {}", batchId, oldStatus, finalStatus);
			} else {
				log.debug("No status change for batch {}: {}", batchId, finalStatus);
			}
		} else {
			log.warn("PayoutTracking not found for batch ID: {}", batchId);
		}
	}

	public void updatePayoutStatusFromWebhook(Map<String, Object> webhookPayload) {
		try {
			String eventType = (String) webhookPayload.get("event_type");
			Map<String, Object> resource = (Map<String, Object>) webhookPayload.get("resource");

			if (eventType.startsWith("PAYMENT.PAYOUTSBATCH.")) {
				// Batch-level event - phù hợp với approach chỉ dùng batch_id
				String batchId = extractBatchId(resource);
				if (batchId != null) {
					updatePayoutStatus(batchId);
				}
			} else if (eventType.startsWith("PAYMENT.PAYOUTS-ITEM.")) {
				// Item-level event - map về batch level
				String batchId = extractBatchIdFromItem(resource);
				if (batchId != null) {
					String status = mapWebhookEventToStatus(eventType);
					updateSinglePayoutStatusByBatch(batchId, status);
				}
			}

		} catch (Exception e) {
			log.error("Webhook processing failed for single payout", e);
			throw new RuntimeException("Webhook processing failed", e);
		}
	}

	private String extractBatchId(Map<String, Object> resource) {
		// Lấy batch ID từ batch header
		Object batchHeader = resource.get("batch_header");
		if (batchHeader instanceof Map) {
			return (String) ((Map<String, Object>) batchHeader).get("payout_batch_id");
		}
		return (String) resource.get("payout_batch_id");
	}

	private String extractBatchIdFromItem(Map<String, Object> resource) {
		// Lấy batch ID từ item resource
		return (String) resource.get("payout_batch_id");
	}

	private String mapWebhookEventToStatus(String eventType) {
		switch (eventType) {
		case "PAYMENT.PAYOUTS-ITEM.SUCCEEDED":
			return "SUCCESS";
		case "PAYMENT.PAYOUTS-ITEM.FAILED":
			return "FAILED";
		case "PAYMENT.PAYOUTS-ITEM.BLOCKED":
			return "BLOCKED";
		case "PAYMENT.PAYOUTS-ITEM.RETURNED":
			return "RETURNED";
		case "PAYMENT.PAYOUTS-ITEM.CANCELED":
			return "CANCELED";
		case "PAYMENT.PAYOUTSBATCH.SUCCESS":
			return "SUCCESS";
		case "PAYMENT.PAYOUTSBATCH.DENIED":
			return "FAILED";
		default:
			return "PENDING";
		}
	}

	private void updateSinglePayoutStatusByBatch(String batchId, String status) {
		Optional<PayoutTracking> trackingOpt = payoutRepository.findByPayoutBatchId(batchId);
		if (trackingOpt.isPresent()) {
			PayoutTracking tracking = trackingOpt.get();

			if (status != null && !status.equals(tracking.getStatus())) {
				String oldStatus = tracking.getStatus();
				tracking.setStatus(status);
				tracking.setUpdatedAt(LocalDateTime.now());
				payoutRepository.save(tracking);

				log.info("Updated single payout batch {} status via webhook: {} -> {}", batchId, oldStatus, status);
			}
		} else {
			log.warn("PayoutTracking not found for batch ID: {}", batchId);
		}
	}
}