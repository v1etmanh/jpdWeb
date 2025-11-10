package com.jpd.web.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.jpd.web.dto.ModerationBatchResponse;
import com.jpd.web.dto.ModerationBatchResult;
import com.jpd.web.dto.ModerationResult;
import com.jpd.web.dto.RejectedContentInfo;
import com.jpd.web.dto.StandardizedContentDto;
import com.jpd.web.exception.ModerationException;
import com.jpd.web.model.Language;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j

public class ContentModerationService {
    
    private final RestTemplate restTemplate;
    
    @Value("${moderation.api.url:http://localhost:5000}")
    private String apiBaseUrl;
    
    @Value("${moderation.api.version:v1}")
    private String apiVersion;
    
    @Value("${moderation.allowed.languages:vi,en}")
    private String defaultAllowedLanguages;
    
    public ContentModerationService(RestTemplateBuilder builder) {
        this.restTemplate = builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
    }
    
    public List<StandardizedContentDto> moderateBatch(List<StandardizedContentDto> contents) {
        if (contents == null || contents.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            Map<String, Object> request = new HashMap<>();
            List<Map<String, Object>> items = contents.stream()
                .map(this::toModerationRequest)
                .collect(Collectors.toList());
            
            request.put("items", items);
            
            String url = String.format("%s/api/%s/moderate/batch", apiBaseUrl, apiVersion);
            log.info("Calling moderation API: {} with {} items", url, items.size());
            
            ResponseEntity<ModerationBatchResponse> response = restTemplate.postForEntity(
                url,
                request,
                ModerationBatchResponse.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ModerationException("Moderation API error: " + response.getStatusCode());
            }
            System.out.print(response.getBody());
            ModerationBatchResponse body = response.getBody();
            if (body == null || !body.isSuccess()) {
                throw new ModerationException("Moderation failed");
            }
            
            return filterApprovedContents(contents, body.getResults());
            
        } catch (RestClientException e) {
            log.error("Failed to call moderation API", e);
            throw new ModerationException("Cannot connect to moderation service");
        }
    }
    
    /**
     * Convert DTO to moderation request
     * ✅ Simplified - no more manual language conversion
     */
    private Map<String, Object> toModerationRequest(StandardizedContentDto dto) {
        Map<String, Object> item = new HashMap<>();
        item.put("content_id", String.valueOf(dto.getContent_id()));
        
        // ✅ Handle List<String> languages
        List<String> langs = dto.getLang();
        if (langs != null && !langs.isEmpty()) {
            if (langs.size() == 1) {
                item.put("lang", langs.get(0)); // Single: "vi"
            } else {
                item.put("lang", langs); // Multiple: ["vi", "en"]
            }
        } else {
            item.put("lang", defaultAllowedLanguages.split(","));
        }
        
        item.put("content", dto.getContent());
        return item;
    }
    
    private List<StandardizedContentDto> filterApprovedContents(
            List<StandardizedContentDto> original,
            List<ModerationResult> results
    ) {
        Map<Long, String> statusMap = new HashMap<>();
        
        for (ModerationResult result : results) {
            if (result.isSuccess() && result.getData() != null) {
                try {
                    Long contentId = Long.parseLong(result.getData().getContent_id());
                    statusMap.put(contentId, result.getData().getStatus());
                } catch (NumberFormatException e) {
                    log.warn("Invalid content_id: {}", result.getData().getContent_id());
                }
            }
        }
        
        List<StandardizedContentDto> approved = new ArrayList<>();
        List<Long> rejected = new ArrayList<>();
        
        for (StandardizedContentDto dto : original) {
            String status = statusMap.get(dto.getContent_id());
            
            if ("APPROVED".equals(status)) {
                approved.add(dto);
            } else {
                rejected.add(dto.getContent_id());
                log.warn("Content {} REJECTED (status: {})", dto.getContent_id(), status);
            }
        }
        
        if (!rejected.isEmpty()) {
            log.info("Moderation: {} approved, {} rejected", approved.size(), rejected.size());
        }
        
        return approved;
    }
    public ModerationBatchResult moderateBatchWithDetails(List<StandardizedContentDto> contents) {
        if (contents == null || contents.isEmpty()) {
            return new ModerationBatchResult(Collections.emptyList(), Collections.emptyList());
        }
        
        try {
            Map<String, Object> request = new HashMap<>();
            List<Map<String, Object>> items = contents.stream()
                .map(this::toModerationRequest)
                .collect(Collectors.toList());
            
            request.put("items", items);
            
            String url = String.format("%s/api/%s/moderate/batch", apiBaseUrl, apiVersion);
            log.info("Calling moderation API: {} with {} items", url, items.size());
            
            ResponseEntity<ModerationBatchResponse> response = restTemplate.postForEntity(
                url,
                request,
                ModerationBatchResponse.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ModerationException("Moderation API error: " + response.getStatusCode());
            }
            
            ModerationBatchResponse body = response.getBody();
            if (body == null || !body.isSuccess()) {
                throw new ModerationException("Moderation failed");
            }
            
            return processResultsWithDetails(contents, body.getResults());
            
        } catch (RestClientException e) {
            log.error("Failed to call moderation API", e);
            throw new ModerationException("Cannot connect to moderation service");
        }
    }

    /**
     * Process results and separate approved vs rejected with reasons
     */
    private ModerationBatchResult processResultsWithDetails(
            List<StandardizedContentDto> original,
            List<ModerationResult> results
    ) {
        Map<Long, ModerationResult> resultMap = new HashMap<>();
        
        for (ModerationResult result : results) {
            if (result.isSuccess() && result.getData() != null) {
                try {
                    Long contentId = Long.parseLong(result.getData().getContent_id());
                    resultMap.put(contentId, result);
                } catch (NumberFormatException e) {
                    log.warn("Invalid content_id: {}", result.getData().getContent_id());
                }
            }
        }
        
        List<StandardizedContentDto> approved = new ArrayList<>();
        List<RejectedContentInfo> rejected = new ArrayList<>();
        
        for (StandardizedContentDto dto : original) {
            ModerationResult result = resultMap.get(dto.getContent_id());
            
            if (result != null && "APPROVED".equals(result.getData().getStatus())) {
                approved.add(dto);
            } else {
                String reason = "Unknown reason";
                if (result != null && result.getData() != null) {
                    reason = result.getData().getRejection_reason() != null 
                        ? result.getData().getRejection_reason() 
                        : result.getData().getStatus();
                }
                
                rejected.add(new RejectedContentInfo(
                    dto.getContent_id(),
                    dto.getContent(),
                    reason
                ));
                
                log.warn("Content {} REJECTED: {}", dto.getContent_id(), reason);
            }
        }
        
        log.info("Moderation complete: {} approved, {} rejected", approved.size(), rejected.size());
        
        return new ModerationBatchResult(approved, rejected);
    }
}