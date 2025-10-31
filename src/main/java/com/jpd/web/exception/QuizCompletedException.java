package com.jpd.web.exception;

import java.util.List;

import com.jpd.web.dto.ParticipantInfo;

public class QuizCompletedException extends RuntimeException {
    
    private final List<ParticipantInfo> finalLeaderboard;
    
    public QuizCompletedException(String message, List<ParticipantInfo> finalLeaderboard) {
        super(message);
        this.finalLeaderboard = finalLeaderboard;
    }
    
    public List<ParticipantInfo> getFinalLeaderboard() {
        return finalLeaderboard;
    }
}