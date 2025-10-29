package com.jpd.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.jpd.web.model.SessionInfo;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class QuizService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // === STRING Operations ===
    public void saveSession(String sessionCode, SessionInfo session) {
        String key = "quiz:session:" + sessionCode;
        redisTemplate.opsForValue().set(key, session);
        
        // Set TTL (Time To Live) - tự động xóa sau 2 giờ
        redisTemplate.expire(key, 2, TimeUnit.HOURS);
    }
    
    public SessionInfo getSession(String sessionCode) {
        String key = "quiz:session:" + sessionCode;
        return (SessionInfo) redisTemplate.opsForValue().get(key);
    }
    
    // === HASH Operations ===
    public void updateSessionField(String sessionCode, String field, Object value) {
        String key = "quiz:session:" + sessionCode;
        redisTemplate.opsForHash().put(key, field, value);
    }
    
    public Object getSessionField(String sessionCode, String field) {
        String key = "quiz:session:" + sessionCode;
        return redisTemplate.opsForHash().get(key, field);
    }
    
    // === SET Operations ===
    public void addParticipant(String sessionCode, String participantId) {
        String key = "quiz:session:" + sessionCode + ":participants";
        redisTemplate.opsForSet().add(key, participantId);
    }
    
    public Long getParticipantCount(String sessionCode) {
        String key = "quiz:session:" + sessionCode + ":participants";
        return redisTemplate.opsForSet().size(key);
    }
    
    // === SORTED SET Operations (Leaderboard) ===
    public void updateScore(String sessionCode, String participantId, int score) {
        String key = "quiz:session:" + sessionCode + ":leaderboard";
        redisTemplate.opsForZSet().incrementScore(key, participantId, score);
    }
    
    public Set<ZSetOperations.TypedTuple<Object>> getTopPlayers(
            String sessionCode, int limit) {
        String key = "quiz:session:" + sessionCode + ":leaderboard";
        // reverseRangeWithScores: lấy từ cao xuống thấp
        return redisTemplate.opsForZSet()
            .reverseRangeWithScores(key, 0, limit - 1);
    }
    
    // === CHECK KEY EXISTS ===
    public boolean sessionExists(String sessionCode) {
        String key = "quiz:session:" + sessionCode;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    // === DELETE KEY ===
    public void deleteSession(String sessionCode) {
        String pattern = "quiz:session:" + sessionCode + "*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}