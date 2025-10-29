package com.jpd.web.service;


import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.jpd.web.config.PayPalV2Config;
@Service
public class PayPalAccessTokenService {
    
    @Autowired
    private PayPalV2Config paypalConfig;
    
    private String accessToken;
    private LocalDateTime tokenExpiryTime;
    
    public String getAccessToken() throws Exception {
        if (accessToken == null || tokenExpiryTime == null || 
            LocalDateTime.now().isAfter(tokenExpiryTime)) {
            generateAccessToken();
        }
        return accessToken;
    }
    
    private void generateAccessToken() throws Exception {
        String auth = Base64.getEncoder().encodeToString(
            (paypalConfig.getClientId() + ":" + paypalConfig.getClientSecret()).getBytes());
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/json");
        headers.add("Authorization", "Basic " + auth);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "client_credentials");
        
        HttpEntity<?> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            paypalConfig.getBaseUrl() + "/v1/oauth2/token", 
            request, 
            Map.class
        );
        
        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> responseBody = response.getBody();
            accessToken = (String) responseBody.get("access_token");
            Integer expiresIn = (Integer) responseBody.get("expires_in");
            tokenExpiryTime = LocalDateTime.now().plusSeconds(expiresIn - 60); // 60s buffer
        } else {
            throw new RuntimeException("Failed to get access token");
        }
    }
}
