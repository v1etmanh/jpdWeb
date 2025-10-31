package com.jpd.web.controller.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/redis-test")
@CrossOrigin(origins = "*")
public class RedisTestController {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    // ===== PING TEST =====
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> testConnection() {
        try {
            stringRedisTemplate.opsForValue().set("test:ping", "PONG", 10, TimeUnit.SECONDS);
            String result = stringRedisTemplate.opsForValue().get("test:ping");
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "✅ Redis connected!");
            response.put("result", result != null ? result : "null");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "❌ " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // ===== SET STRING =====
    @PostMapping("/set")
    public ResponseEntity<Map<String, Object>> testSet(
            @RequestParam String key, 
            @RequestParam String value) {
        try {
            // Lưu vào Redis
            stringRedisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);
            
            // Verify ngay
            String verified = stringRedisTemplate.opsForValue().get(key);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Saved successfully");
            response.put("key", key);
            response.put("value", value);
            response.put("verified", verified);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // ===== GET STRING =====
    @GetMapping("/get")
    public ResponseEntity<Map<String, Object>> testGet(@RequestParam String key) {
        try {
            // Thử lấy bằng StringRedisTemplate (cho String thuần)
            String stringValue = null;
            try {
                stringValue = stringRedisTemplate.opsForValue().get(key);
            } catch (Exception e) {
                // Ignore
            }
            
            // Thử lấy bằng RedisTemplate (cho Object/JSON)
            Object objectValue = null;
            try {
                objectValue = redisTemplate.opsForValue().get(key);
            } catch (Exception e) {
                // Ignore serialization errors
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("key", key);
            response.put("stringValue", stringValue != null ? stringValue : "null");
            response.put("objectValue", objectValue != null ? objectValue.toString() : "null");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // ===== TEST OBJECT =====
    @PostMapping("/test-object")
    public ResponseEntity<Map<String, Object>> testObject() {
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("name", "Quiz App");
            testData.put("version", "1.0");
            testData.put("active", true);
            
            // Save
            redisTemplate.opsForValue().set("test:object", testData, 1, TimeUnit.HOURS);
            
            // Get
            Object saved = redisTemplate.opsForValue().get("test:object");
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Object saved successfully");
            response.put("original", testData);
            response.put("retrieved", saved);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("trace", e.getClass().getName());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // ===== TEST SESSION =====
    @PostMapping("/test-session")
    public ResponseEntity<Map<String, Object>> testSession() {
        try {
            Map<String, Object> session = new HashMap<>();
            session.put("sessionCode", "ABC123");
            session.put("title", "Test Quiz");
            session.put("teacherName", "Teacher");
            session.put("status", "WAITING");
            session.put("totalParticipants", 0);
            
            // Save
            redisTemplate.opsForValue().set("quiz:session:ABC123", session, 2, TimeUnit.HOURS);
            
            // Get
            Object saved = redisTemplate.opsForValue().get("quiz:session:ABC123");
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Session saved");
            response.put("data", saved);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // ===== GET ALL KEYS =====
    @GetMapping("/all-keys")  // Đổi từ /keys thành /all-keys
    public ResponseEntity<Map<String, Object>> getAllKeys() {
        try {
            Set<String> keys = stringRedisTemplate.keys("*");
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", keys != null ? keys.size() : 0);
            response.put("keys", keys);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // ===== CLEAR TEST DATA =====
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearTestKeys() {
        try {
            Set<String> keys = stringRedisTemplate.keys("test:*");
            Long deleted = 0L;
            if (keys != null && !keys.isEmpty()) {
                deleted = stringRedisTemplate.delete(keys);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cleared " + deleted + " keys");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}