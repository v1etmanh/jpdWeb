package com.jpd.web.service;
import com.jpd.web.model.*;
import com.jpd.web.repository.KahootRepository;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.dto.*;
import com.jpd.web.exception.ModuleContentNotFoundException;
import com.jpd.web.exception.QuizCompletedException;
import com.jpd.web.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SessionService {
    
    @Autowired
    private KahootRepository kahootRepository;
    @Autowired
    private ValidationResources validationResources;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ModuleContentRepository moduleContentRepository;
    /**
     * Tạo session mới
     */
    public CreateSessionResponse createSession(CreateSessionRequest request, long creatorId) {
        // 1. Lấy Kahoot từ database,long 
        KahootListFunction kahoot = kahootRepository.findById(request.getKahootId())
            .orElseThrow(() -> new RuntimeException("Kahoot not found with id: " + request.getKahootId()));
        Creator c=this.validationResources.validateCreatorExists(creatorId);
        if(kahoot.getCreator().getCreatorId()!=creatorId)
        	throw new UnauthorizedException("tài nguyên không thuộc về mày");
        // 2. Lọc chỉ lấy câu hỏi Multiple Choice và GapFill
        List<Long> questionIds = kahoot.getModuleContent().stream()
            .filter(mc -> mc.getTypeOfContent() == TypeOfContent.MULTIPLE_CHOICE 
                       || mc.getTypeOfContent() == TypeOfContent.GAPFILL)
            .map(ModuleContent::getMcId)
            .collect(Collectors.toList());
        
        if (questionIds.isEmpty()) {
            throw new RuntimeException("No valid questions (Multiple Choice or GapFill) found in this Kahoot");
        }
        
        // 3. Generate session code và ID
        String sessionCode = generateUniqueSessionCode();
        String sessionId = UUID.randomUUID().toString();
        
        // 4. Tạo SessionInfo
        SessionInfo session = SessionInfo.builder()
            .sessionId(sessionId)
            .sessionCode(sessionCode)
            .kahootId(request.getKahootId())
            .title(kahoot.getTitle())
            .questionIds(questionIds)
            .totalQuestions(questionIds.size())
            .teacherId(creatorId)
            .teacherName(request.getTeacherName())
            .status(SessionStatus.WAITING)
            .currentQuestionIndex(-1)
            .currentQuestionId(null)
            .currentQuestionType(null)
            .questionStartTime(null)
            .acceptingAnswers(false)
            .createdAt(LocalDateTime.now())
            .totalParticipants(0)
            .currentAnswers(0)
            .showLeaderboardAfterEachQuestion(true)
            .randomizeQuestions(false)
            .randomizeOptions(true)
            .build();
        
        // 5. Lưu vào Redis (TTL 3 giờ)
        try {
            String sessionKey = "quiz:session:" + sessionCode;
            String sessionJson = objectMapper.writeValueAsString(session);
            stringRedisTemplate.opsForValue().set(sessionKey, sessionJson, 3, TimeUnit.HOURS);
            
            // Tạo Set để tracking participants
            String participantsKey = "quiz:session:" + sessionCode + ":participants";
            stringRedisTemplate.delete(participantsKey); // Clear nếu có
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to save session to Redis: " + e.getMessage(), e);
        }
        
        // 6. Generate QR code URL (dùng API public)
        String joinUrl = "http://localhost:3000/creator/class/kahoot/studentJoin/" + sessionCode; // Thay bằng domain thật
        String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" + joinUrl;
        
        // 7. Return response
        return CreateSessionResponse.builder()
            .sessionCode(sessionCode)
            .sessionId(sessionId)
            .qrCodeUrl(qrCodeUrl)
            .joinUrl(joinUrl)
            .title(kahoot.getTitle())
            .totalQuestions(questionIds.size())
            .build();
    }
    
    /**
     * Join session
     */
    public ParticipantInfo joinSession(JoinSessionRequest request) {
        String sessionCode = request.getSessionCode();
        String participantName = request.getParticipantName();
        
        // 1. Kiểm tra session có tồn tại không
        SessionInfo session = getSession(sessionCode);
        if (session == null) {
            throw new RuntimeException("Session not found with code: " + sessionCode);
        }
        
        // 2. Kiểm tra session status
        if (session.getStatus() == SessionStatus.FINISHED) {
            throw new RuntimeException("This quiz has already finished");
        }
        
        if (session.getStatus() == SessionStatus.ACTIVE) {
            throw new RuntimeException("Quiz already started. Cannot join now.");
        }
        
        // 3. Generate participant ID
        String participantId = UUID.randomUUID().toString();
        
        // 4. Tạo ParticipantInfo
        ParticipantInfo participant = ParticipantInfo.builder()
            .participantId(participantId)
            .name(participantName)
            .sessionCode(sessionCode)
            .joinedAt(LocalDateTime.now())
            .currentScore(0)
            .build();
        
        // 5. Lưu participant vào Redis
        try {
            // Add vào Set participants
            String participantsKey = "quiz:session:" + sessionCode + ":participants";
            stringRedisTemplate.opsForSet().add(participantsKey, participantId);
            
            // Lưu participant info
            String participantKey = "quiz:session:" + sessionCode + ":participant:" + participantId;
            String participantJson = objectMapper.writeValueAsString(participant);
            stringRedisTemplate.opsForValue().set(participantKey, participantJson, 3, TimeUnit.HOURS);
            
            // Update total participants count
            session.setTotalParticipants(session.getTotalParticipants() + 1);
            saveSession(sessionCode, session);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to join session: " + e.getMessage(), e);
        }
        
        return participant;
    }
    
    /**
     * Get session by code
     */
    public SessionInfo getSession(String sessionCode) {
        try {
            String sessionKey = "quiz:session:" + sessionCode;
            String sessionJson = stringRedisTemplate.opsForValue().get(sessionKey);
            
            if (sessionJson == null) {
                return null;
            }
            
            return objectMapper.readValue(sessionJson, SessionInfo.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get session: " + e.getMessage(), e);
        }
    }
    
    /**
     * Save session to Redis
     */
    public void saveSession(String sessionCode, SessionInfo session) {
        try {
            String sessionKey = "quiz:session:" + sessionCode;
            String sessionJson = objectMapper.writeValueAsString(session);
            stringRedisTemplate.opsForValue().set(sessionKey, sessionJson, 3, TimeUnit.HOURS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save session: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all participants in session
     */
    public List<ParticipantInfo> getParticipants(String sessionCode) {
        try {
            String participantsKey = "quiz:session:" + sessionCode + ":participants";
            Set<String> participantIds = stringRedisTemplate.opsForSet().members(participantsKey);
            
            if (participantIds == null || participantIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<ParticipantInfo> participants = new ArrayList<>();
            for (String participantId : participantIds) {
                String participantKey = "quiz:session:" + sessionCode + ":participant:" + participantId;
                String participantJson = stringRedisTemplate.opsForValue().get(participantKey);
                
                if (participantJson != null) {
                    ParticipantInfo participant = objectMapper.readValue(participantJson, ParticipantInfo.class);
                    participants.add(participant);
                }
            }
            
            return participants;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get participants: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate unique 6-character session code
     */
    private String generateUniqueSessionCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        String code;
        
        // Retry nếu code đã tồn tại
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            code = sb.toString();
        } while (getSession(code) != null);
        
        return code;
    }
    
    /**
     * Delete session (cleanup)
     */
    public void deleteSession(String sessionCode) {
        try {
            // Delete session
            stringRedisTemplate.delete("quiz:session:" + sessionCode);
            
            // Delete participants
            Set<String> participantIds = stringRedisTemplate.opsForSet().members("quiz:session:" + sessionCode + ":participants");
            if (participantIds != null) {
                for (String participantId : participantIds) {
                    stringRedisTemplate.delete("quiz:session:" + sessionCode + ":participant:" + participantId);
                }
            }
            stringRedisTemplate.delete("quiz:session:" + sessionCode + ":participants");
            
            // Delete other related keys
            Set<String> keys = stringRedisTemplate.keys("quiz:session:" + sessionCode + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete session: " + e.getMessage(), e);
        }
    }
    public SubmitAnswerResponse submitAnswer(SubmitAnswerRequest request) {
        try {
            String sessionCode = request.getSessionCode();
            String participantId = request.getParticipantId();
            Long questionId = request.getQuestionId();

            // 1. Lấy session
            SessionInfo session = getSession(sessionCode);
            if (session == null) {
                throw new RuntimeException("Session not found");
            }

            // 2. Kiểm tra có đang nhận đáp án không
            if (!session.isAcceptingAnswers()) {
                throw new RuntimeException("Not accepting answers at this time");
            }

            // 3. Kiểm tra thời gian (nếu đã hết giờ thì reject)
            if (session.getQuestionEndTime() != null && 
                LocalDateTime.now().isAfter(session.getQuestionEndTime())) {
                throw new RuntimeException("Time's up! Cannot submit answer");
            }

            // 4. Lấy thông tin participant
            String participantKey = "quiz:session:" + sessionCode + ":participant:" + participantId;
            String participantJson = stringRedisTemplate.opsForValue().get(participantKey);
            if (participantJson == null) {
                throw new RuntimeException("Participant not found");
            }
            ParticipantInfo participant = objectMapper.readValue(participantJson, ParticipantInfo.class);

            // 5. Tạo TempAnswer
            TempAnswer tempAnswer = TempAnswer.builder()
                .participantId(participantId)
                .participantName(participant.getName())
                .questionId(questionId)
                .answer(request.getAnswer())
                .submittedAt(LocalDateTime.now())
                .build();

            // 6. Lưu vào Redis (dùng Hash để dễ quản lý)
            String answersKey = "quiz:session:" + sessionCode + ":question:" + questionId + ":answers";
            String tempAnswerJson = objectMapper.writeValueAsString(tempAnswer);
            stringRedisTemplate.opsForHash().put(answersKey, participantId, tempAnswerJson);
            
            // Set TTL cho key này
            stringRedisTemplate.expire(answersKey, 3, TimeUnit.HOURS);

            // 7. Tăng số người đã trả lời
            session.setCurrentAnswers(session.getCurrentAnswers() + 1);
            saveSession(sessionCode, session);

            // 8. Trả về response (chưa có điểm)
            return SubmitAnswerResponse.builder()
                .success(true)
                .message("Answer submitted successfully")
                .totalAnswered(session.getCurrentAnswers())
                .totalParticipants(session.getTotalParticipants())
                .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to submit answer: " + e.getMessage(), e);
        }
    }
    public ModuleContent getQuestionById(Long questionId) {
        ModuleContent q = moduleContentRepository.findById(questionId)
            .orElseThrow(() -> new ModuleContentNotFoundException(questionId));
        ModuleContent t=null;
        if(q.getTypeOfContent()==TypeOfContent.MULTIPLE_CHOICE) {
        	MultipleChoiceQuestion q1=(MultipleChoiceQuestion)q;
        	q1.getOptions().forEach(op->{
        		if(op.isCorrect()==true)op.setCorrect(false);
        	});
        	return q1;
        }
        else if(q.getTypeOfContent()==TypeOfContent.GAPFILL) {
         GapFillQuestion q2=(GapFillQuestion)q;
         q2.setAnswers(null);
         return q2;
        }
        else {
        	throw new ModuleContentNotFoundException(questionId);
        }
       
    }
    public QuestionResultResponse endQuestion(String sessionCode) {
        try {
            // 1. Lấy session
            SessionInfo session = getSession(sessionCode);
            if (session == null) {
                throw new RuntimeException("Session not found");
            }

            Long currentQuestionId = session.getCurrentQuestionId();
            if (currentQuestionId == null) {
                throw new RuntimeException("No active question");
            }

            // 2. Dừng nhận đáp án
            session.setAcceptingAnswers(false);
            saveSession(sessionCode, session);

            // 3. Lấy tất cả đáp án tạm từ Redis
            String answersKey = "quiz:session:" + sessionCode + ":question:" + currentQuestionId + ":answers";
            Map<Object, Object> tempAnswersMap = stringRedisTemplate.opsForHash().entries(answersKey);

            // 4. Lấy đáp án đúng từ DB
            ModuleContent question = moduleContentRepository.findById(currentQuestionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

            List<QuestionResult> results = new ArrayList<>();

            // 5. Chấm điểm cho từng participant
            for (Map.Entry<Object, Object> entry : tempAnswersMap.entrySet()) {
                String participantId = (String) entry.getKey();
                String tempAnswerJson = (String) entry.getValue();
                TempAnswer tempAnswer = objectMapper.readValue(tempAnswerJson, TempAnswer.class);

                // Kiểm tra đáp án
                boolean correct = checkAnswer(question, tempAnswer.getAnswer());
                
                // Tính điểm dựa trên thời gian trả lời
                int points = calculatePoints(correct, session, tempAnswer.getSubmittedAt());

                // Cập nhật điểm cho participant
                String participantKey = "quiz:session:" + sessionCode + ":participant:" + participantId;
                String participantJson = stringRedisTemplate.opsForValue().get(participantKey);
                ParticipantInfo participant = objectMapper.readValue(participantJson, ParticipantInfo.class);
                
                participant.setCurrentScore(participant.getCurrentScore() + points);
                
                String updatedJson = objectMapper.writeValueAsString(participant);
                stringRedisTemplate.opsForValue().set(participantKey, updatedJson, 3, TimeUnit.HOURS);

                // Thêm vào results
                results.add(QuestionResult.builder()
                    .participantId(participantId)
                    .participantName(tempAnswer.getParticipantName())
                    .answer(tempAnswer.getAnswer())
                    .correct(correct)
                    .points(points)
                    .totalScore(participant.getCurrentScore())
                    .build());
            }

            // 6. Xóa đáp án tạm
            stringRedisTemplate.delete(answersKey);

            // 7. Reset currentAnswers
            session.setCurrentAnswers(0);
            saveSession(sessionCode, session);

            // 8. Trả về kết quả
            return QuestionResultResponse.builder()
                .questionId(currentQuestionId)
                .results(results)
                .correctAnswer(getCorrectAnswer(question))
                .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to end question: " + e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra đáp án đúng/sai
     */
    private boolean checkAnswer(ModuleContent question, String answer) {
        if (question.getTypeOfContent() == TypeOfContent.MULTIPLE_CHOICE) {
            MultipleChoiceQuestion mcq = (MultipleChoiceQuestion) question;
            for (MultipleChoiceOption option : mcq.getOptions()) {
                if (option.isCorrect() && Long.parseLong(answer) == option.getMcoId()) {
                    return true;
                }
            }
            return false;
            
        } else if (question.getTypeOfContent() == TypeOfContent.GAPFILL) {
            GapFillQuestion gfq = (GapFillQuestion) question;
            List<String> userAnswers = Arrays.asList(answer.split(","));
            
            if (userAnswers.size() != gfq.getAnswers().size()) {
                return false;
            }
            
            for (int i = 0; i < userAnswers.size(); i++) {
                if (!userAnswers.get(i).trim().equalsIgnoreCase(
                        gfq.getAnswers().get(i).getAnswer().trim())) {
                    return false;
                }
            }
            return true;
        }
        
        return false;
    }

    /**
     * Tính điểm dựa trên thời gian (trả lời nhanh = nhiều điểm hơn)
     */
    private int calculatePoints(boolean correct, SessionInfo session, LocalDateTime submittedAt) {
        if (!correct) {
            return 0;
        }

        // Tính thời gian từ lúc bắt đầu câu hỏi đến lúc submit
        long secondsTaken = java.time.Duration.between( session.getQuestionStartTime(), submittedAt ).getSeconds();
        Integer timeLimit = session.getQuestionTimeLimit();
        if (timeLimit == null || timeLimit <= 0) {
            return 1000; // Điểm mặc định
        }

        // Điểm từ 500 -> 1000 dựa trên tốc độ
        // Trả lời ngay lập tức = 1000 điểm
        // Trả lời ở giây cuối = 500 điểm
        double ratio = 1.0 - ((double) secondsTaken / timeLimit);
        ratio = Math.max(0, Math.min(1, ratio)); // Clamp [0, 1]
        
        return (int) (500 + (ratio * 500));
    }

    /**
     * Lấy đáp án đúng để hiển thị
     */
    private String getCorrectAnswer(ModuleContent question) {
        if (question.getTypeOfContent() == TypeOfContent.MULTIPLE_CHOICE) {
            MultipleChoiceQuestion mcq = (MultipleChoiceQuestion) question;
            for (MultipleChoiceOption option : mcq.getOptions()) {
                if (option.isCorrect()) {
                    return option.getOptionText();
                }
            }
        } else if (question.getTypeOfContent() == TypeOfContent.GAPFILL) {
            GapFillQuestion gfq = (GapFillQuestion) question;
            return gfq.getAnswers().stream()
                .map(GapFillAnswer::getAnswer)
                .collect(Collectors.joining(", "));
        }
        return null;
    }
    
        
        // ... existing code
        
        /**
         * Bắt đầu câu hỏi tiếp theo
         */
        public StartQuestionResponse startNextQuestion(String sessionCode) {
            SessionInfo session = getSession(sessionCode);
            
            if (session == null) {
                throw new RuntimeException("Session not found");
            }
            
            // Tăng index
            int nextIndex = session.getCurrentQuestionIndex() + 1;
            
            // Kiểm tra còn câu hỏi không
            if (nextIndex >= session.getQuestionIds().size()) {
            	session.setStatus(SessionStatus.FINISHED);
                saveSession(sessionCode, session);
                
                // Lấy final leaderboard
                List<ParticipantInfo> participants = getParticipants(sessionCode);
                participants.sort((a, b) -> Integer.compare(b.getCurrentScore(), a.getCurrentScore()));
                
                // Broadcast QUIZ_ENDED (cần inject SimpMessagingTemplate vào SessionService)
                // Hoặc throw một custom exception để controller xử lý
                throw new QuizCompletedException("Quiz completed", participants);
               
            }
            
            Long questionId = session.getQuestionIds().get(nextIndex);
            
            // Lấy question từ DB
            ModuleContent question = moduleContentRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
            
            Integer timeLimit = 30;
            LocalDateTime now = LocalDateTime.now();
            
            // Update session
            session.setCurrentQuestionIndex(nextIndex);
            session.setCurrentQuestionId(questionId);
            session.setCurrentQuestionType(question.getTypeOfContent());
            session.setQuestionStartTime(now);
            session.setQuestionEndTime(now.plusSeconds(timeLimit));
            session.setAcceptingAnswers(true);
            session.setCurrentAnswers(0);
            session.setQuestionTimeLimit(timeLimit);
            
            saveSession(sessionCode, session);
            
            return StartQuestionResponse.builder()
                .questionId(questionId)
                .questionNumber(nextIndex + 1)
                .totalQuestions(session.getTotalQuestions())
                .question(getQuestionById(questionId))
                .timeLimit(timeLimit)
                .serverTime(now)
                .build();
        }
    
    
}
