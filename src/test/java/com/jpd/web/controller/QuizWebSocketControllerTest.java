package com.jpd.web.controller;
import com.jpd.web.dto.*;
import com.jpd.web.exception.QuizCompletedException;
import com.jpd.web.model.*;
import com.jpd.web.service.SessionService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class QuizWebSocketControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private QuizWebSocketController controller;

    @BeforeEach
    void setUp() {
        Mockito.reset(messagingTemplate, sessionService);
    }

    // TC_HP_01 – joinSession: broadcast PARTICIPANT_JOINED
    @Test
    void testJoinSession_happyPath() {
        String sessionCode = "ABC";
        JoinSessionRequest req = new JoinSessionRequest(sessionCode, "Alice");
        ParticipantInfo mockedParticipant = ParticipantInfo.builder()
                .participantId("1").name("Alice").currentScore(0).sessionCode(sessionCode)
                .joinedAt(LocalDateTime.now()).build();
        SessionInfo session = SessionInfo.builder()
                .sessionCode(sessionCode).totalParticipants(5).build();

        when(sessionService.joinSession(req)).thenReturn(mockedParticipant);
        when(sessionService.getSession(sessionCode)).thenReturn(session);

        controller.joinSession(sessionCode, req);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/quiz/" + sessionCode + "/participants"), captor.capture());

        Map<String, Object> payload = captor.getValue();
        assertEquals("PARTICIPANT_JOINED", payload.get("type"));
        assertEquals(mockedParticipant, payload.get("participant"));
        assertEquals(5, payload.get("totalParticipants"));
    }

    // TC_HP_02 – getParticipants: broadcast PARTICIPANTS_LIST
    @Test
    void testGetParticipants_happyPath() {
        String sessionCode = "ABC";
        List<ParticipantInfo> participants = Arrays.asList(
                ParticipantInfo.builder().participantId("1").name("A").currentScore(10).build(),
                ParticipantInfo.builder().participantId("2").name("B").currentScore(20).build(),
                ParticipantInfo.builder().participantId("3").name("C").currentScore(15).build()
        );
        when(sessionService.getParticipants(sessionCode)).thenReturn(participants);

        controller.getParticipants(sessionCode);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/quiz/" + sessionCode + "/participants"), captor.capture());
        var payload = captor.getValue();
        assertEquals("PARTICIPANTS_LIST", payload.get("type"));
        assertEquals(participants, payload.get("participants"));
        assertEquals(participants.size(), payload.get("totalParticipants"));
    }

    // TC_HP_03 – startQuiz: phát QUIZ_STARTED; auto phát QUESTION_STARTED (thread)
    @Test
    void testStartQuiz_happyPath() throws InterruptedException {
        String sessionCode = "ABC";
        SessionInfo session = SessionInfo.builder()
                .sessionCode(sessionCode)
                .status(SessionStatus.WAITING)
                .totalQuestions(10)
                .build();

        // Tạo một instance giả của ModuleContent (ví dụ MultipleChoiceQuestion)
        com.jpd.web.model.ModuleContent mockQuestion = mock(com.jpd.web.model.MultipleChoiceQuestion.class);

        StartQuestionResponse sqr = StartQuestionResponse.builder()
                .questionId(11L)
                .questionNumber(1)
                .totalQuestions(10)
                .question(mockQuestion)
                .timeLimit(30)
                .serverTime(java.time.LocalDateTime.now())
                .build();

        when(sessionService.getSession(sessionCode)).thenReturn(session);
        when(sessionService.startNextQuestion(sessionCode)).thenReturn(sqr);

        controller.startQuiz(sessionCode);

        // Capture both QUIZ_STARTED and QUESTION_STARTED broadcasts
        ArgumentCaptor<Map<String, Object>> broadcastCaptor = ArgumentCaptor.forClass(Map.class);
        // We expect two calls: first QUIZ_STARTED, then QUESTION_STARTED (async).
        verify(messagingTemplate, timeout(2500).times(2))
                .convertAndSend(eq("/topic/quiz/" + sessionCode), broadcastCaptor.capture());
        // The captor will have two payloads: .getAllValues()
        var allPayloads = broadcastCaptor.getAllValues();

        // QUIZ_STARTED broadcast comes first
        Map<String, Object> quizStartedPayload = allPayloads.get(0);
        assertEquals("QUIZ_STARTED", quizStartedPayload.get("type"));

        // QUESTION_STARTED broadcast (async), comes second
        Map<String, Object> questionStartedPayload = allPayloads.get(1);
        assertEquals("QUESTION_STARTED", questionStartedPayload.get("type"));
        assertEquals(11L, questionStartedPayload.get("questionId"));
        assertEquals(1, questionStartedPayload.get("questionNumber"));
        assertEquals(30, questionStartedPayload.get("timeLimit"));
        assertEquals(10, questionStartedPayload.get("totalQuestions"));
        assertEquals(mockQuestion, questionStartedPayload.get("question")); // kiểm tra trường question là ModuleContent

        // VERIFY sessionService.saveSession called (status=ACTIVE, currentQuestionIndex=-1)
        assertEquals(SessionStatus.ACTIVE, session.getStatus());
        assertEquals(-1, session.getCurrentQuestionIndex());
        verify(sessionService).saveSession(sessionCode, session);
    }

//    // TC_HP_04 – nextQuestion: broadcast QUESTION_STARTED
//    @Test
//    void testNextQuestion_happyPath() {
//        String sessionCode = "ABC";
//        // Defensive: Provide non-null values for all StartQuestionResponse fields that QuizWebSocketController.nextQuestion expects to access!
//        com.jpd.web.model.ModuleContent mockQuestion = mock(com.jpd.web.model.MultipleChoiceQuestion.class);
//
//        // Set all required fields (question, questionId, questionNumber, timeLimit, totalQuestions)
//        StartQuestionResponse sqr = StartQuestionResponse.builder()
//                .questionId(2L)
//                .questionNumber(2)
//                .timeLimit(25)
//                .question(mockQuestion)
//                .totalQuestions(10)
//                // Defensive: add more fields as needed
//                .build();
//
//        // Defensive: assertions to ensure all required fields are not null
//        assertNotNull(sqr.getQuestion(), "question must not be null");
//        assertNotNull(sqr.getQuestionId(), "questionId must not be null");
//        assertNotNull(sqr.getQuestionNumber(), "questionNumber must not be null");
//        assertNotNull(sqr.getTimeLimit(), "timeLimit must not be null");
//        assertNotNull(sqr.getTotalQuestions(), "totalQuestions must not be null");
//
//        when(sessionService.startNextQuestion(sessionCode)).thenReturn(sqr);
//
//        controller.nextQuestion(sessionCode);
//
//        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
//        verify(messagingTemplate).convertAndSend(eq("/topic/quiz/" + sessionCode), captor.capture());
//
//        Map<String, Object> payload = captor.getValue();
//        assertEquals("QUESTION_STARTED", payload.get("type"));
//        assertEquals(2L, payload.get("questionId"));
//        assertEquals(2, payload.get("questionNumber"));
//        assertEquals(25, payload.get("timeLimit"));
//        assertEquals(10, payload.get("totalQuestions"));
//        assertEquals(mockQuestion, payload.get("question")); // kiểm tra trường question là ModuleContent
//    }

    // TC_HP_05 – submitAnswer: broadcast ANSWER_SUBMITTED
    @Test
    void testSubmitAnswer_happyPath() {
        String sessionCode = "ABC";
        SubmitAnswerRequest req = SubmitAnswerRequest.builder()
                .sessionCode(sessionCode).participantId("x").questionId(5L).answer("10").build();
        SubmitAnswerResponse resp = SubmitAnswerResponse.builder()
                .totalAnswered(2)
                .totalParticipants(5)
                .build();
        when(sessionService.submitAnswer(req)).thenReturn(resp);

        controller.submitAnswer(sessionCode, req);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/quiz/" + sessionCode), captor.capture());

        Map<String, Object> payload = captor.getValue();
        assertEquals("ANSWER_SUBMITTED", payload.get("type"));
        assertEquals(2, payload.get("totalAnswered"));
        assertEquals(5, payload.get("totalParticipants"));
    }

    // TC_HP_06 – endQuestion: broadcast QUESTION_ENDED
    @Test
    void testEndQuestion_happyPath() {
        String sessionCode = "ABC";
        List<QuestionResult> results = Arrays.asList(
                QuestionResult.builder().participantId("p1").correct(true).points(1000).build()
        );
        QuestionResultResponse resultResponse = QuestionResultResponse.builder()
                .questionId(7L)
                .correctAnswer("2")
                .results(results)
                .build();

        when(sessionService.endQuestion(sessionCode)).thenReturn(resultResponse);

        controller.endQuestion(sessionCode);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/quiz/" + sessionCode), captor.capture());
        Map<String, Object> payload = captor.getValue();
        assertEquals("QUESTION_ENDED", payload.get("type"));
        assertEquals(7L, payload.get("questionId"));
        assertEquals("2", payload.get("correctAnswer"));
        assertEquals(results, payload.get("results"));
    }

    // TC_HP_07 – showLeaderboard: sort desc & broadcast LEADERBOARD
    @Test
    void testShowLeaderboard_happyPath() {
        String sessionCode = "ABC";
        ParticipantInfo pa = ParticipantInfo.builder().participantId("A").currentScore(10).build();
        ParticipantInfo pb = ParticipantInfo.builder().participantId("B").currentScore(20).build();
        ParticipantInfo pc = ParticipantInfo.builder().participantId("C").currentScore(15).build();
        List<ParticipantInfo> list = new ArrayList<>(Arrays.asList(pa, pb, pc));
        when(sessionService.getParticipants(sessionCode)).thenReturn(list);

        controller.showLeaderboard(sessionCode);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/quiz/" + sessionCode), captor.capture());
        Map<String, Object> payload = captor.getValue();
        @SuppressWarnings("unchecked")
        List<ParticipantInfo> sorted = (List<ParticipantInfo>) payload.get("participants");
        assertEquals("LEADERBOARD", payload.get("type"));
        assertEquals(pb, sorted.get(0));
        assertEquals(pc, sorted.get(1));
        assertEquals(pa, sorted.get(2));
    }

    // TC_HP_08 – endQuiz: set FINISHED, save, broadcast QUIZ_ENDED
    @Test
    void testEndQuiz_happyPath() {
        String sessionCode = "ABC";
        ParticipantInfo pa = ParticipantInfo.builder().participantId("A").currentScore(10).build();
        // Use a mutable list to avoid UnsupportedOperationException from sort()
        List<ParticipantInfo> participants = new ArrayList<>();
        participants.add(pa);

        SessionInfo session = SessionInfo.builder()
                .sessionCode(sessionCode)
                .status(SessionStatus.ACTIVE)
                .build();
        when(sessionService.getSession(sessionCode)).thenReturn(session);
        when(sessionService.getParticipants(sessionCode)).thenReturn(participants);

        controller.endQuiz(sessionCode);

        assertEquals(SessionStatus.FINISHED, session.getStatus());
        verify(sessionService).saveSession(sessionCode, session);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/quiz/" + sessionCode), captor.capture());
        Map<String, Object> data = captor.getValue();
        assertEquals("QUIZ_ENDED", data.get("type"));
        assertEquals(participants, data.get("finalLeaderboard"));
    }

    // TC_HP_09 – ping: broadcast PONG
    @Test
    void testPing_happyPath() {
        controller.ping();
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/quiz/test"), captor.capture());
        Map<String, Object> payload = captor.getValue();
        assertEquals("PONG", payload.get("message"));
    }

    // TC_EC_01 – joinSession: service throws Exception => no broadcast
    @Test
    void testJoinSession_serviceThrowsException() {
        String sessionCode = "ABC";
        JoinSessionRequest req = new JoinSessionRequest(sessionCode, "A");
        when(sessionService.joinSession(req)).thenThrow(new RuntimeException("fail"));

        controller.joinSession(sessionCode, req);

        // Avoid ambiguous method with generics by explicitly casting the arguments
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    // TC_EC_02 – getParticipants: empty list
    @Test
    void testGetParticipants_emptyList() {
        String sessionCode = "ABC";
        when(sessionService.getParticipants(sessionCode)).thenReturn(Collections.emptyList());

        controller.getParticipants(sessionCode);
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/quiz/" + sessionCode + "/participants"), captor.capture());
        Map<String, Object> payload = captor.getValue();
        assertEquals("PARTICIPANTS_LIST", payload.get("type"));
        assertEquals(0, payload.get("totalParticipants"));
        assertTrue(((List<?>) payload.get("participants")).isEmpty());
    }

    // TC_EC_03 – startQuiz: getSession null => no broadcast, no saveSession
    @Test
    void testStartQuiz_sessionNotFound_noBroadcast_noSave() {
        String sessionCode = "DEF";
        when(sessionService.getSession(sessionCode)).thenReturn(null);

        // Gọi method: không throw vì đã bị catch
        controller.startQuiz(sessionCode);

        // Không được lưu session
        verify(sessionService, never()).saveSession(any(String.class), any());
        // Không broadcast QUIZ_STARTED (fix ambiguous method)
        verify(messagingTemplate, never()).<String, Object>convertAndSend(eq("/topic/quiz/" + sessionCode), any(Object.class));
        // Không auto-start next question (không gọi startNextQuestion)
        verify(sessionService, never()).startNextQuestion(any(String.class));
    }

//    // TC_EC_04 – nextQuestion: timeLimit = 0 (abnormal)
//    @Test
//    void testNextQuestion_timeLimitZero() {
//        String sessionCode = "ABC";
//        // Simulate a StartQuestionResponse with timeLimit = 0 (abnormal)
//        com.jpd.web.model.ModuleContent mockQuestion = mock(com.jpd.web.model.MultipleChoiceQuestion.class);
//
//        // In order to avoid NPE during Map.of() due to a null in the question,
//        // ensure all non-primitive fields are non-null (esp. .question)
//        StartQuestionResponse response = StartQuestionResponse.builder()
//                .questionId(42L)
//                .questionNumber(1)
//                .totalQuestions(10)
//                .question(mockQuestion) // mockQuestion is NOT null
//                .timeLimit(0)
//                .build();
//        when(sessionService.startNextQuestion(sessionCode)).thenReturn(response);
//
//        // Defensive: verify that none of the expected fields are null, else the controller will throw NPE
//        assertNotNull(response.getQuestion(), "question must not be null to avoid Map.of NPE");
//        assertNotNull(response.getQuestionId(), "questionId must not be null to avoid Map.of NPE");
//        assertNotNull(response.getQuestionNumber(), "questionNumber must not be null to avoid Map.of NPE");
//
//        controller.nextQuestion(sessionCode);
//
//        // Should still broadcast QUESTION_STARTED with timeLimit=0
//        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
//        verify(messagingTemplate).convertAndSend(eq("/topic/quiz/" + sessionCode), captor.capture());
//
//        Map<String, Object> payload = captor.getValue();
//        assertEquals("QUESTION_STARTED", payload.get("type"));
//        assertEquals(42L, payload.get("questionId"));
//        assertEquals(1, payload.get("questionNumber"));
//        assertEquals(0, payload.get("timeLimit"));
//        assertEquals(10, payload.get("totalQuestions"));
//        assertEquals(mockQuestion, payload.get("question"));
//    }

    // TC_EC_05 – submitAnswer: totalAnswered > totalParticipants
    @Test
    void testSubmitAnswer_CountInconsistency() {
        String sessionCode = "ABC";
        SubmitAnswerRequest req = SubmitAnswerRequest.builder()
                .sessionCode(sessionCode).participantId("id").questionId(1L).answer("A").build();
        SubmitAnswerResponse resp = SubmitAnswerResponse.builder()
                .totalAnswered(6)
                .totalParticipants(5)
                .build();
        when(sessionService.submitAnswer(req)).thenReturn(resp);

        controller.submitAnswer(sessionCode, req);
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/quiz/" + sessionCode), captor.capture());
        Map<String, Object> payload = captor.getValue();
        assertEquals(6, payload.get("totalAnswered"));
        assertEquals(5, payload.get("totalParticipants"));
    }

    // TC_EC_06 – endQuestion: results empty
    @Test
    void testEndQuestion_resultsEmpty() {
        String sessionCode = "ABC";
        QuestionResultResponse resp = QuestionResultResponse.builder()
                .questionId(1L)
                .correctAnswer("X")
                .results(Collections.emptyList())
                .build();
        when(sessionService.endQuestion(sessionCode)).thenReturn(resp);

        controller.endQuestion(sessionCode);
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/quiz/" + sessionCode), captor.capture());
        Map<String, Object> payload = captor.getValue();
        assertEquals("QUESTION_ENDED", payload.get("type"));
        assertTrue(((List<?>) payload.get("results")).isEmpty());
    }

    // TC_EC_07 – showLeaderboard: bằng điểm
    @Test
    void testShowLeaderboard_SameScore() {
        String sessionCode = "DEF";
        ParticipantInfo p1 = ParticipantInfo.builder().participantId("A").currentScore(10).build();
        ParticipantInfo p2 = ParticipantInfo.builder().participantId("B").currentScore(10).build();
        when(sessionService.getParticipants(sessionCode)).thenReturn(Arrays.asList(p1, p2));
        controller.showLeaderboard(sessionCode);
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/quiz/" + sessionCode), captor.capture());
        Map<String, Object> payload = captor.getValue();
        @SuppressWarnings("unchecked")
        List<ParticipantInfo> sorted = (List<ParticipantInfo>) payload.get("participants");
        assertEquals(2, sorted.size());
        assertEquals(10, sorted.get(0).getCurrentScore());
        assertEquals(10, sorted.get(1).getCurrentScore());
    }

    // TC_EC_08 – endQuiz: finalLeaderboard rỗng
    @Test
    void testEndQuiz_emptyParticipantList() {
        String sessionCode = "ZZZ";
        SessionInfo session = SessionInfo.builder().sessionCode(sessionCode).status(SessionStatus.ACTIVE).build();
        when(sessionService.getSession(sessionCode)).thenReturn(session);
        when(sessionService.getParticipants(sessionCode)).thenReturn(Collections.emptyList());

        controller.endQuiz(sessionCode);

        // Use ArgumentCaptor to avoid method ambiguity and ensure Map access
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/quiz/" + sessionCode), captor.capture());
        Map<String, Object> payload = captor.getValue();
        assertEquals("QUIZ_ENDED", payload.get("type"));
        assertTrue(((List<?>) payload.get("finalLeaderboard")).isEmpty());
    }

    // TC_EC_09 – startQuiz: thread auto-start fail
    @Test
    void testStartQuiz_ThreadAutoStartFail() throws Exception {
        String sessionCode = "AUTOFAIL";
        SessionInfo session = SessionInfo.builder()
                .sessionCode(sessionCode)
                .status(SessionStatus.WAITING)
                .totalQuestions(8)
                .build();

        when(sessionService.getSession(sessionCode)).thenReturn(session);

        // Do NOT stub sessionService.startNextQuestion; it will throw if invoked (unnecessary stubbing avoided)

        controller.startQuiz(sessionCode);

        // Should only fire QUIZ_STARTED, not QUESTION_STARTED (since async fails)
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/quiz/" + sessionCode), captor.capture());

        Map<String, Object> payload = captor.getValue();
        assertEquals("QUIZ_STARTED", payload.get("type"));
        assertEquals(8, payload.get("totalQuestions"));

        // Verify that NO other convertAndSend to this destination (no async/question)
        verifyNoMoreInteractions(messagingTemplate);
    }

    // TC_ES_01 – nextQuestion: QuizCompletedException => broadcast QUIZ_ENDED
    @Test
    void testNextQuestion_QuizCompletedException() {
        String sessionCode = "ENDQ";
        List<ParticipantInfo> leaderboard = Arrays.asList(
                ParticipantInfo.builder().participantId("A").currentScore(99).build()
        );
        QuizCompletedException ex = new QuizCompletedException("done!", leaderboard);
        when(sessionService.startNextQuestion(sessionCode)).thenThrow(ex);

        controller.nextQuestion(sessionCode);

        // Use ArgumentCaptor<Map<String,Object>> for type safety and to fix ambiguous/undefined get()
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/quiz/" + sessionCode), captor.capture());
        Map<String, Object> payload = captor.getValue();
        assertEquals("QUIZ_ENDED", payload.get("type"));
        assertEquals(leaderboard, payload.get("finalLeaderboard"));
    }

    // TC_ES_02 – nextQuestion: Exception bất kỳ => broadcast ERROR
    @Test
    void testNextQuestion_GenericError() {
        String sessionCode = "ERR";
        when(sessionService.startNextQuestion(sessionCode)).thenThrow(new RuntimeException("DB down"));

        controller.nextQuestion(sessionCode);

        // Fix ambiguous method and payload .get - use ArgumentCaptor<Map<String, Object>> for type-safety
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/quiz/" + sessionCode), captor.capture());
        Map<String, Object> payload = captor.getValue();
        assertEquals("ERROR", payload.get("type"));
        assertEquals("DB down", payload.get("message"));
    }

    // TC_ES_03 – submitAnswer: Exception => không broadcast
    @Test
    void testSubmitAnswer_Exception() {
        String sessionCode = "A";
        SubmitAnswerRequest req = SubmitAnswerRequest.builder().sessionCode(sessionCode).build();
        when(sessionService.submitAnswer(req)).thenThrow(new RuntimeException("err"));
        controller.submitAnswer(sessionCode, req);

        // Fix ambiguous method: explicitly cast arguments to resolve ambiguity
        verify(messagingTemplate, never()).<String, Object>convertAndSend(anyString(), any(Object.class));
    }

    // TC_ES_04 – endQuestion: Exception => không broadcast
    @Test
    void testEndQuestion_Exception() {
        String sessionCode = "A";
        when(sessionService.endQuestion(sessionCode)).thenThrow(new RuntimeException("fail"));
        controller.endQuestion(sessionCode);
        // Fix ambiguous method: specify type parameters explicitly to resolve ambiguity warning
        verify(messagingTemplate, never()).<String, Object>convertAndSend(anyString(), any(Object.class));
    }

    // TC_ES_05 – endQuiz: getSession throws Exception => không broadcast
    @Test
    void testEndQuiz_SessionThrows() {
        String sessionCode = "ERR";
        when(sessionService.getSession(sessionCode)).thenThrow(new RuntimeException("bad"));
        controller.endQuiz(sessionCode);
        // Fix ambiguous method: explicitly cast arguments to resolve ambiguity
        verify(messagingTemplate, never()).<String, Object>convertAndSend(anyString(), any(Object.class));
    }

    // TC_ES_06 – joinSession: getSession throws Exception khi đọc totalParticipants => không broadcast
    @Test
    void testJoinSession_TotalParticipantsThrowsException() {
        String sessionCode = "ABC";
        JoinSessionRequest req = new JoinSessionRequest(sessionCode, "test");
        ParticipantInfo mockedParticipant = ParticipantInfo.builder().participantId("1").name("test").build();

        when(sessionService.joinSession(req)).thenReturn(mockedParticipant);
        when(sessionService.getSession(sessionCode)).thenThrow(new RuntimeException("fail getSession"));

        controller.joinSession(sessionCode, req);

        // Fix ambiguous method: specify type parameters explicitly to resolve ambiguity warning
        verify(messagingTemplate, never()).<String, Object>convertAndSend(anyString(), any(Object.class));
    }
}
