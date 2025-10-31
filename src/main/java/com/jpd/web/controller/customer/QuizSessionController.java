package com.jpd.web.controller.customer;
import com.jpd.web.dto.*;
import com.jpd.web.model.JoinSessionRequest;
import com.jpd.web.model.SessionInfo;
import com.jpd.web.service.SessionService;
import com.jpd.web.service.utils.RequestAttributeExtractor;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = "*")
public class QuizSessionController {
    
    @Autowired
    private SessionService sessionService;
    
    /**
     * Tạo session mới
     */
    @PostMapping("/create")
    public ResponseEntity<CreateSessionResponse> createSession(@RequestBody CreateSessionRequest request,
    		HttpServletRequest request2) {
        try {
        	long creatorId= RequestAttributeExtractor.extractCreatorId(request2);
    		
            CreateSessionResponse response = sessionService.createSession(request,creatorId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    /**
     * Join session (REST endpoint - dùng để validate trước khi connect WebSocket)
     */
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> joinSession(@RequestBody JoinSessionRequest request) {
        try {
            // Validate session exists
            SessionInfo session = sessionService.getSession(request.getSessionCode());
            if (session == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Session not found"
                ));
            }
            
            // Validate session status
            if (session.getStatus() == com.jpd.web.model.SessionStatus.FINISHED) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Quiz has already finished"
                ));
            }
            
            if (session.getStatus() == com.jpd.web.model.SessionStatus.ACTIVE) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Quiz already started"
                ));
            }
            
            // Join session
            ParticipantInfo participant = sessionService.joinSession(request);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "participant", participant,
                "session", session
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Get session info
     */
    @GetMapping("/{sessionCode}")
    public ResponseEntity<SessionInfo> getSession(@PathVariable String sessionCode) {
        try {
            SessionInfo session = sessionService.getSession(sessionCode);
            if (session == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    /**
     * Get all participants
     */
    @GetMapping("/{sessionCode}/participants")
    public ResponseEntity<List<ParticipantInfo>> getParticipants(@PathVariable String sessionCode) {
        try {
            List<ParticipantInfo> participants = sessionService.getParticipants(sessionCode);
            return ResponseEntity.ok(participants);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    /**
     * Delete session
     */
    @DeleteMapping("/{sessionCode}")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable String sessionCode) {
        try {
            sessionService.deleteSession(sessionCode);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Session deleted"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    @PostMapping("/submit-answer")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(@RequestBody SubmitAnswerRequest request) {
        return ResponseEntity.ok(sessionService.submitAnswer(request));
    }
    
    @PostMapping("/end-question/{sessionCode}")
    public ResponseEntity<QuestionResultResponse> endQuestion(@PathVariable String sessionCode) {
        return ResponseEntity.ok(sessionService.endQuestion(sessionCode));
    }
}