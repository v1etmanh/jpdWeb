package com.jpd.web.service.utils;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;

@Service
public class CommentFilterService {
    
    private Map<String, Double> blacklistMap = new HashMap<>();
    private static final double THRESHOLD = 2.0;
    
    @PostConstruct
    public void init() {
        loadBlacklist();
    }
    
    private void loadBlacklist() {
        try {
            ClassPathResource resource = new ClassPathResource("blacklist.json");
            Gson gson = new Gson();
            
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> data = gson.fromJson(
                new InputStreamReader(resource.getInputStream()), 
                type
            );
            
            List<Map<String, Object>> blacklist = 
                (List<Map<String, Object>>) data.get("blacklist");
            
            for (Map<String, Object> item : blacklist) {
                String word = (String) item.get("word");
                double weight = ((Number) item.get("weight")).doubleValue();
                
                blacklistMap.put(word.toLowerCase(), weight);
                
                // Add variants
                List<String> variants = (List<String>) item.get("variants");
                if (variants != null) {
                    for (String variant : variants) {
                        blacklistMap.put(variant.toLowerCase(), weight);
                    }
                }
            }
            
            System.out.println("✅ Loaded " + blacklistMap.size() + " blacklist words");
            
        } catch (Exception e) {
            System.err.println("❌ Error loading blacklist: " + e.getMessage());
        }
    }
    
    /**
     * Check comment có toxic không
     * @param comment - Bình luận cần check
     * @return true nếu TOXIC (không hợp lý), false nếu OK
     */
    public boolean isToxic(String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            return false;
        }
        
        String normalized = normalizeText(comment);
        double score = calculateScore(normalized);
        System.out.print(score);
        return score >= THRESHOLD;
    }
    
    /**
     * Chuẩn hóa text
     */
    private String normalizeText(String text) {
        return text.toLowerCase()
            .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
    
    /**
     * Tính điểm toxic
     */
    private double calculateScore(String text) {
        double score = 0;
        
        for (Map.Entry<String, Double> entry : blacklistMap.entrySet()) {
            if (text.contains(entry.getKey())) {
                score += entry.getValue();
            }
        }
        
        return score;
    }
    
    /**
     * Phân tích chi tiết (optional)
     */
    public Map<String, Object> analyze(String comment) {
        String normalized = normalizeText(comment);
        double score = calculateScore(normalized);
        boolean toxic = score >= THRESHOLD;
        
        List<String> detectedWords = new ArrayList<>();
        for (String word : blacklistMap.keySet()) {
            if (normalized.contains(word)) {
                detectedWords.add(word);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("comment", comment);
        result.put("toxic", toxic);
        result.put("score", score);
        result.put("detectedWords", detectedWords);
        
        return result;
    }
}