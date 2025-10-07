package com.jpd.web.service.utils;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class CodeGenerator {
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Generate a 6-digit random code
     */
    public String generate6DigitCode() {
        int number = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(number);
    }
    
    /**
     * Generate code with custom length
     */
    public String generateCode(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Length must be at least 1");
        }
        
        int min = (int) Math.pow(10, length - 1);
        int max = (int) Math.pow(10, length) - 1;
        int number = min + secureRandom.nextInt(max - min + 1);
        
        return String.valueOf(number);
    }
}
