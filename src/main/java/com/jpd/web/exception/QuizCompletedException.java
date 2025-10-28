package com.jpd.web.exception;

import com.jpd.web.model.ParticipantInfo;
import java.util.List;

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