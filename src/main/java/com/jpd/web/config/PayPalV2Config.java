package com.jpd.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;



@Component
public class PayPalV2Config {
    
    @Value("${paypal.client.id}")
    private String clientId;
    
    @Value("${paypal.client.secret}")
    private String clientSecret;
    
    @Value("${paypal.mode:sandbox}")
    private String mode;
    
    public String getBaseUrl() {
        return "sandbox".equals(mode) ? 
            "https://api-m.sandbox.paypal.com" : 
            "https://api-m.paypal.com";
    }
    
    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }
    public String getMode() { return mode; }
}