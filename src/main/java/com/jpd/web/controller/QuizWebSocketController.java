package com.jpd.web.controller;

import com.jpd.web.dto.*;
import com.jpd.web.exception.QuizCompletedException;
import com.jpd.web.model.*;
import com.jpd.web.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.List;
import java.util.Map;

@Controller
public class QuizWebSocketController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private SessionService sessionService;
    
    /**
     * Student join qua WebSocket
     */
    @MessageMapping("/quiz/join/{sessionCode}")
    public void joinSession(
            @DestinationVariable String sessionCode,
            @Payload JoinSessionRequest request) {
        
        try {
            System.out.println("🔵 WebSocket Join: " + request.getParticipantName() + " → " + sessionCode);
            
            // Join session
            ParticipantInfo participant = sessionService.joinSession(request);
            
            // Broadcast đến teacher và tất cả clients
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode + "/participants",
                Map.of(
                    "type", "PARTICIPANT_JOINED",
                    "participant", participant,
                    "totalParticipants", sessionService.getSession(sessionCode).getTotalParticipants()
                )
            );
            
            System.out.println("✅ Broadcasted participant joined: " + participant.getName());
            
        } catch (Exception e) {
            System.err.println("❌ Join error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Teacher request danh sách participants hiện tại
     */
    @MessageMapping("/quiz/{sessionCode}/get-participants")
    public void getParticipants(@DestinationVariable String sessionCode) {
        try {
            List<ParticipantInfo> participants = sessionService.getParticipants(sessionCode);
            
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode + "/participants",
                Map.of(
                    "type", "PARTICIPANTS_LIST",
                    "participants", participants,
                    "totalParticipants", participants.size()
                )
            );
            
            System.out.println("✅ Sent participants list: " + participants.size() + " participants");
            
        } catch (Exception e) {
            System.err.println("❌ Get participants error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== QUIZ CONTROL ====================
    
    /**
     * 🎮 Teacher START QUIZ
     */
    @MessageMapping("/quiz/{sessionCode}/start")
    public void startQuiz(@DestinationVariable String sessionCode) {
        try {
            System.out.println("🎬 Starting quiz: " + sessionCode);
            
            SessionInfo session = sessionService.getSession(sessionCode);
            if (session == null) {
                throw new RuntimeException("Session not found");
            }
            
            // Update status
            session.setStatus(SessionStatus.ACTIVE);
            session.setCurrentQuestionIndex(-1);
            sessionService.saveSession(sessionCode, session);
            
            // Broadcast QUIZ_STARTED
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode,
                Map.of(
                    "type", "QUIZ_STARTED",
                    "message", "Quiz is starting!",
                    "totalQuestions", session.getTotalQuestions()
                )
            );
            
            System.out.println("✅ Quiz started successfully");
            
            // ✨✨✨ THÊM PHẦN NÀY - Tự động start câu hỏi đầu tiên sau 2 giây
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Delay 2 giây
                    System.out.println("📤 Auto-starting first question...");
                    
                    StartQuestionResponse response = sessionService.startNextQuestion(sessionCode);
                    
                    messagingTemplate.convertAndSend(
                        "/topic/quiz/" + sessionCode,
                        Map.of(
                            "type", "QUESTION_STARTED",
                            "questionId", response.getQuestionId(),
                            "questionNumber", response.getQuestionNumber(),
                            "question", response.getQuestion(),
                            "timeLimit", response.getTimeLimit(),
                            "serverTime", response.getServerTime(),
                            "totalQuestions", response.getTotalQuestions()
                        )
                    );
                    
                    System.out.println("✅ First question started automatically");
                    
                } catch (Exception e) {
                    System.err.println("❌ Failed to auto-start first question: " + e.getMessage());
                }
            }).start();
            // ✨✨✨ KẾT THÚC PHẦN THÊM
            
        } catch (Exception e) {
            System.err.println("❌ Start quiz error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 📤 Teacher NEXT QUESTION
     */
    @MessageMapping("/quiz/{sessionCode}/next-question")
    public void nextQuestion(@DestinationVariable String sessionCode) {
        try {
            System.out.println("➡️ Next question for: " + sessionCode);
            
            StartQuestionResponse response = sessionService.startNextQuestion(sessionCode);
            
            // Broadcast câu hỏi MỚI đến TẤT CẢ participants
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode,
                Map.of(
                    "type", "QUESTION_STARTED",
                    "questionId", response.getQuestionId(),
                    "questionNumber", response.getQuestionNumber(),
                    "question", response.getQuestion(),
                    "timeLimit", response.getTimeLimit(),
                    "serverTime", response.getServerTime(),
                    "totalQuestions", response.getTotalQuestions()
                )
            );
            
            System.out.println("✅ Question sent: " + response.getQuestionId());
            
        } catch (QuizCompletedException e) {
            // Hết câu hỏi rồi → Broadcast QUIZ_ENDED
            System.out.println("🎯 Quiz completed, broadcasting final results");
            
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode,
                Map.of(
                    "type", "QUIZ_ENDED",
                    "message", "Quiz has ended! All questions completed.",
                    "finalLeaderboard", e.getFinalLeaderboard()
                )
            );
            
            System.out.println("✅ Quiz ended - all questions completed");
            
        } catch (Exception e) {
            System.err.println("❌ Next question error: " + e.getMessage());
            e.printStackTrace();
            
            // Broadcast error
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode,
                Map.of(
                    "type", "ERROR",
                    "message", e.getMessage()
                )
            );
        }
    }
    
    /**
     * 📥 Student SUBMIT ANSWER
     */
    @MessageMapping("/quiz/{sessionCode}/submit-answer")
    public void submitAnswer(
            @DestinationVariable String sessionCode,
            @Payload SubmitAnswerRequest request) {
        
        try {
            System.out.println("📝 Answer submitted: " + request.getParticipantId());
            System.out.print("answer"+request.getAnswer());
            SubmitAnswerResponse response = sessionService.submitAnswer(request);
            
            // Broadcast CẬP NHẬT SỐ NGƯỜI ĐÃ TRẢ LỜI
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode,
                Map.of(
                    "type", "ANSWER_SUBMITTED",
                    "totalAnswered", response.getTotalAnswered(),
                    "totalParticipants", response.getTotalParticipants()
                )
            );
            
            System.out.println("✅ Answer recorded: " + response.getTotalAnswered() + "/" + response.getTotalParticipants());
            
        } catch (Exception e) {
            System.err.println("❌ Submit answer error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 🏁 Teacher END QUESTION (chấm điểm)
     */
    @MessageMapping("/quiz/{sessionCode}/end-question")
    public void endQuestion(@DestinationVariable String sessionCode) {
        try {
            System.out.println("🏁 Ending question for: " + sessionCode);
            
            QuestionResultResponse result = sessionService.endQuestion(sessionCode);
            
            // Broadcast KẾT QUẢ + ĐÁNH GIÁ đến TẤT CẢ
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode,
                Map.of(
                    "type", "QUESTION_ENDED",
                    "questionId", result.getQuestionId(),
                    "correctAnswer", result.getCorrectAnswer(),
                    "results", result.getResults()
                )
            );
            
            System.out.println("✅ Question ended, results sent");
            
        } catch (Exception e) {
            System.err.println("❌ End question error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 📊 Teacher SHOW LEADERBOARD
     */
    @MessageMapping("/quiz/{sessionCode}/show-leaderboard")
    public void showLeaderboard(@DestinationVariable String sessionCode) {
        try {
            System.out.println("📊 Showing leaderboard: " + sessionCode);
            
            List<ParticipantInfo> participants = sessionService.getParticipants(sessionCode);
            
            // Sort by score (cao → thấp)
            participants.sort((a, b) -> Integer.compare(b.getCurrentScore(), a.getCurrentScore()));
            
            // Broadcast LEADERBOARD
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode,
                Map.of(
                    "type", "LEADERBOARD",
                    "participants", participants
                )
            );
            
            System.out.println("✅ Leaderboard sent");
            
        } catch (Exception e) {
            System.err.println("❌ Leaderboard error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 🎯 Teacher END QUIZ
     */
    @MessageMapping("/quiz/{sessionCode}/end-quiz")
    public void endQuiz(@DestinationVariable String sessionCode) {
        try {
            System.out.println("🎯 Ending quiz: " + sessionCode);
            
            SessionInfo session = sessionService.getSession(sessionCode);
            session.setStatus(SessionStatus.FINISHED);
            sessionService.saveSession(sessionCode, session);
            
            // Get final leaderboard
            List<ParticipantInfo> participants = sessionService.getParticipants(sessionCode);
            participants.sort((a, b) -> Integer.compare(b.getCurrentScore(), a.getCurrentScore()));
            
            // Broadcast QUIZ_ENDED
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode,
                Map.of(
                    "type", "QUIZ_ENDED",
                    "message", "Quiz has ended!",
                    "finalLeaderboard", participants
                )
            );
            
            System.out.println("✅ Quiz ended");
            
        } catch (Exception e) {
            System.err.println("❌ End quiz error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test ping message
     */
    @MessageMapping("/quiz/ping")
    public void ping() {
        messagingTemplate.convertAndSend("/topic/quiz/test", Map.of("message", "PONG"));
        System.out.println("✅ Ping received, sent PONG");
    }
}