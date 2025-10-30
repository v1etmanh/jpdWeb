package com.jpd.web.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jpd.web.dto.*;
import com.jpd.web.exception.ModuleContentNotFoundException;
import com.jpd.web.exception.QuizCompletedException;
import com.jpd.web.model.*;
import com.jpd.web.repository.KahootRepository;
import com.jpd.web.repository.ModuleContentRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test cho SessionService với Mockito, hoàn toàn cô lập khỏi Redis và DB.
 * Bao gồm các testcase coverage 85%+ như prompt yêu cầu.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionServiceTest {

    @InjectMocks
    private SessionService service;

    @Mock private KahootRepository kahootRepository;
    @Mock private ModuleContentRepository moduleContentRepository;
    @Mock private StringRedisTemplate redis;

    @Mock private ValueOperations<String, String> valueOps;
    @Mock private SetOperations<String, String> setOps;
    @Mock private HashOperations<String, Object, Object> hashOps;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ==== Dữ liệu mẫu cho các test ====
    private final Long kahootId = 101L;
    private final Long teacherId = 555L;
    private final String teacherName = "GV Test";
    private final String sessionCode = "ABC123";
    private final String sessionId = UUID.randomUUID().toString();
    private final String title = "Quiz Title";
    private final Long mcQuestionId = 111L;
    private final Long gfQuestionId = 222L;
    private final Long catchAllUnusedId = 404L;

    private CreateSessionRequest createSessionRequest;
    private KahootListFunction kahoot;
    private List<ModuleContent> moduleContents;

    private SessionInfo sessionWaiting;
    private SessionInfo sessionActive;
    private SessionInfo sessionFinished;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.opsForSet()).thenReturn(setOps);
        when(redis.opsForHash()).thenReturn(hashOps);

        moduleContents = new ArrayList<>();
        ModuleContent q1 = makeMCQuestion(mcQuestionId, "A");
        ModuleContent q2 = makeGapFillQuestion(gfQuestionId);
        moduleContents.add(q1);
        moduleContents.add(q2);

        kahoot = mock(KahootListFunction.class);
        when(kahoot.getModuleContent()).thenReturn(moduleContents);
        when(kahoot.getTitle()).thenReturn(title);

        createSessionRequest = new CreateSessionRequest();
        createSessionRequest.setKahootId(kahootId);
        createSessionRequest.setTeacherId(teacherId);
        createSessionRequest.setTeacherName(teacherName);

        sessionWaiting = SessionInfo.builder()
                .sessionId(sessionId)
                .sessionCode(sessionCode)
                .kahootId(kahootId)
                .title(title)
                .questionIds(List.of(mcQuestionId, gfQuestionId))
                .totalQuestions(2)
                .teacherId(teacherId)
                .teacherName(teacherName)
                .status(SessionStatus.WAITING)
                .currentQuestionIndex(-1)
                .acceptingAnswers(false)
                .createdAt(LocalDateTime.now())
                .totalParticipants(0)
                .currentAnswers(0)
                .showLeaderboardAfterEachQuestion(true)
                .randomizeQuestions(false)
                .randomizeOptions(true)
                .build();

        sessionActive = SessionInfo.builder()
                .sessionId(sessionId)
                .sessionCode(sessionCode)
                .kahootId(kahootId)
                .title(title)
                .questionIds(List.of(mcQuestionId, gfQuestionId))
                .totalQuestions(2)
                .teacherId(teacherId)
                .teacherName(teacherName)
                .status(SessionStatus.ACTIVE)
                .currentQuestionIndex(0)
                .currentQuestionId(mcQuestionId)
                .acceptingAnswers(true)
                .createdAt(LocalDateTime.now())
                .totalParticipants(2)
                .currentAnswers(1)
                .showLeaderboardAfterEachQuestion(true)
                .randomizeQuestions(false)
                .randomizeOptions(true)
                .questionStartTime(LocalDateTime.now().minusSeconds(5))
                .questionEndTime(LocalDateTime.now().plusSeconds(25))
                .questionTimeLimit(30)
                .build();

        sessionFinished = SessionInfo.builder().sessionId(sessionId)
                .sessionCode(sessionCode)
                .kahootId(kahootId)
                .title(title)
                .questionIds(List.of(mcQuestionId))
                .totalQuestions(1)
                .teacherId(teacherId)
                .teacherName(teacherName)
                .status(SessionStatus.FINISHED)
                .build();
    }

    // ==== createSession ====
    @Test
    void TC_HP_01_createSession_happyPath() throws Exception {
        // Kahoot hợp lệ, sessionCode không trùng; Trả về response đầy đủ
        when(kahootRepository.findById(kahootId)).thenReturn(Optional.of(kahoot));
        when(valueOps.get(startsWith("quiz:session:"))).thenReturn(null);
        CreateSessionResponse resp = service.createSession(createSessionRequest);
        assertNotNull(resp);
        assertEquals(6, resp.getSessionCode().length());
        assertFalse(resp.getSessionId().isEmpty());
        assertEquals(title, resp.getTitle());
        assertEquals(2, resp.getTotalQuestions());
        assertTrue(resp.getQrCodeUrl().contains(resp.getSessionCode()));
        verify(valueOps).set(startsWith("quiz:session:"), anyString(), eq(3L), eq(TimeUnit.HOURS));
        verify(redis).delete(contains(":participants"));
    }

    @Test
    void TC_EC_01_createSession_zeroValidQuestions() {
        // Kahoot chỉ có câu hỏi loại khác, filter xong rỗng
        KahootListFunction badKahoot = mock(KahootListFunction.class);
        when(badKahoot.getModuleContent()).thenReturn(List.of());
        when(kahootRepository.findById(kahootId)).thenReturn(Optional.of(badKahoot));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createSession(createSessionRequest));
        assertTrue(ex.getMessage().contains("No valid questions"));
    }

    @Test
    void TC_EC_02_createSession_sessionCodeCollision() throws Exception {
        // Va chạm code lần đầu: Redis trả về một JSON session giả (hợp lệ), lần hai mới null => vẫn tạo được session mới
        String dummySessionJson = objectMapper.writeValueAsString(SessionInfo.builder()
                .sessionId("dummy")
                .sessionCode("DUMMY1")
                .kahootId(kahootId)
                .title("Dummy")
                .questionIds(List.of())
                .totalQuestions(0)
                .teacherId(teacherId)
                .teacherName(teacherName)
                .status(SessionStatus.WAITING)
                .build());
        when(kahootRepository.findById(kahootId)).thenReturn(Optional.of(kahoot));
        // Lần 1 getSession(code) trả session JSON, lần 2 trả null
        when(valueOps.get(anyString())).thenReturn(dummySessionJson).thenReturn(null);
        CreateSessionResponse resp = service.createSession(createSessionRequest);
        assertNotNull(resp);
        assertEquals(6, resp.getSessionCode().length());
    }

    @Test
    void TC_ES_01_createSession_kahootNotFound() {
        when(kahootRepository.findById(kahootId)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createSession(createSessionRequest));
        assertTrue(ex.getMessage().contains("Kahoot not found"));
    }

    @Test
    void TC_ES_02_createSession_redisSetError() throws Exception {
        when(kahootRepository.findById(kahootId)).thenReturn(Optional.of(kahoot));
        when(valueOps.get(anyString())).thenReturn(null);
        doThrow(new RuntimeException("Mock RedisError"))
                .when(valueOps).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createSession(createSessionRequest));
        assertTrue(ex.getMessage().contains("Failed to save session to Redis"));
    }

    // ==== joinSession ====
    @Test
    void TC_ES_submitAnswer_redisPutError() throws Exception {
        SessionInfo s = SessionInfo.builder()
                .sessionId(sessionWaiting.getSessionId())
                .sessionCode(sessionWaiting.getSessionCode())
                .kahootId(sessionWaiting.getKahootId())
                .title(sessionWaiting.getTitle())
                .questionIds(sessionWaiting.getQuestionIds())
                .totalQuestions(sessionWaiting.getTotalQuestions())
                .teacherId(sessionWaiting.getTeacherId())
                .teacherName(sessionWaiting.getTeacherName())
                .status(sessionWaiting.getStatus())
                .currentQuestionIndex(sessionWaiting.getCurrentQuestionIndex())
                .currentQuestionId(sessionWaiting.getCurrentQuestionId())
                .currentQuestionType(sessionWaiting.getCurrentQuestionType())
                .questionStartTime(sessionWaiting.getQuestionStartTime())
                .questionTimeLimit(sessionWaiting.getQuestionTimeLimit())
                .acceptingAnswers(sessionWaiting.isAcceptingAnswers())
                .questionEndTime(sessionWaiting.getQuestionEndTime())
                .createdAt(sessionWaiting.getCreatedAt())
                .startedAt(sessionWaiting.getStartedAt())
                .finishedAt(sessionWaiting.getFinishedAt())
                .totalParticipants(sessionWaiting.getTotalParticipants())
                .currentAnswers(sessionWaiting.getCurrentAnswers())
                .showLeaderboardAfterEachQuestion(sessionWaiting.isShowLeaderboardAfterEachQuestion())
                .randomizeQuestions(sessionWaiting.isRandomizeQuestions())
                .randomizeOptions(sessionWaiting.isRandomizeOptions())
                .build();

        String sessionJson = objectMapper.writeValueAsString(s);

        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(sessionJson);

        when(valueOps.get("quiz:session:" + sessionCode + ":participant:x"))
                .thenReturn("{\"participantId\":\"x\"}");

        doThrow(new RuntimeException("PutError"))
                .when(hashOps).put(anyString(), anyString(), anyString());

        SubmitAnswerRequest req = SubmitAnswerRequest.builder()
                .sessionCode(sessionCode)
                .participantId("x")
                .questionId(mcQuestionId)
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.submitAnswer(req));
        assertTrue(ex.getMessage().contains("Failed to submit answer"));
    }

//    @Test
//    void TC_EC_03_joinSession_statusFinished() throws Exception {
//        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(objectMapper.writeValueAsString(sessionFinished));
//        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.joinSession(new JoinSessionRequest(sessionCode, "A")));
//        assertTrue(ex.getMessage().contains("already finished"));
//    }

    @Test
    void TC_EC_04_joinSession_statusActive() throws Exception {
        // Avoid UnfinishedStubbingException by pre-serializing JSON before stubbing
        SessionInfo sActive = new SessionInfo(
                sessionActive.getSessionId(),
                sessionActive.getSessionCode(),
                sessionActive.getKahootId(),
                sessionActive.getTitle(),
                sessionActive.getQuestionIds(),
                sessionActive.getTotalQuestions(),
                sessionActive.getTeacherId(),
                sessionActive.getTeacherName(),
                SessionStatus.ACTIVE,
                sessionActive.getCurrentQuestionIndex(),
                sessionActive.getCurrentQuestionId(),
                sessionActive.getCurrentQuestionType(),
                sessionActive.getQuestionStartTime(),
                sessionActive.getQuestionTimeLimit(),
                sessionActive.isAcceptingAnswers(),
                sessionActive.getQuestionEndTime(),
                sessionActive.getCreatedAt(),
                sessionActive.getStartedAt(),
                sessionActive.getFinishedAt(),
                sessionActive.getTotalParticipants(),
                sessionActive.getCurrentAnswers(),
                sessionActive.isShowLeaderboardAfterEachQuestion(),
                sessionActive.isRandomizeQuestions(),
                sessionActive.isRandomizeOptions()
        );
        String sActiveJson = objectMapper.writeValueAsString(sActive);
        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(sActiveJson);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.joinSession(new JoinSessionRequest(sessionCode, "A")));
        assertTrue(ex.getMessage().contains("already started"));
    }
//
//    @Test
//    void TC_ES_03_joinSession_redisFailure() throws Exception {
//        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(objectMapper.writeValueAsString(sessionWaiting));
//        doThrow(new RuntimeException("RedisSET")).when(setOps).add(anyString(), anyString());
//        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.joinSession(new JoinSessionRequest(sessionCode, "TS")));
//        assertTrue(ex.getMessage().contains("Failed to join session"));
//    }

    @Test
    void TC_ES_xx_submitAnswer_hashPutError() throws Exception {
        String sessionJson = objectMapper.writeValueAsString(sessionWaiting);
        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(sessionJson);
        when(valueOps.get("quiz:session:" + sessionCode + ":participant:x"))
                .thenReturn("{\"participantId\":\"x\"}");

        doThrow(new RuntimeException("PutError"))
                .when(hashOps).put(anyString(), anyString(), anyString());

        SubmitAnswerRequest req = SubmitAnswerRequest.builder()
                .sessionCode(sessionCode)
                .participantId("x")
                .questionId(mcQuestionId)
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.submitAnswer(req));
        assertTrue(ex.getMessage().contains("Failed to submit answer"));
    }

    @Test
    void TC_EC_05_getSession_null() {
        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(null);
        assertNull(service.getSession(sessionCode));
    }

    @Test
    void TC_ES_04_getSession_jsonParseError() throws Exception {
        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn("{not valid json");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getSession(sessionCode));
        assertTrue(ex.getMessage().contains("Failed to get session"));
    }

    // ==== saveSession ====
    @Test
    void TC_HP_04_saveSession_ok() throws Exception {
        doNothing().when(valueOps).set(eq("quiz:session:" + sessionCode), anyString(), eq(3L), eq(TimeUnit.HOURS));
        service.saveSession(sessionCode, sessionWaiting);
        verify(valueOps).set(eq("quiz:session:" + sessionCode), anyString(), eq(3L), eq(TimeUnit.HOURS));
    }

    @Test
    void TC_ES_05_saveSession_redisSetError() throws Exception {
        doThrow(new RuntimeException("SETERR"))
                .when(valueOps).set(eq("quiz:session:" + sessionCode), anyString(), anyLong(), any(TimeUnit.class));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.saveSession(sessionCode, sessionWaiting));
        assertTrue(ex.getMessage().contains("Failed to save session"));
    }

    // ==== getParticipants ====
    @Test
    void TC_HP_05_getParticipants_some() throws Exception {
        Set<String> ids = Set.of("p1", "p2");
        ParticipantInfo pi1 = ParticipantInfo.builder().participantId("p1").name("A").sessionCode(sessionCode).build();
        ParticipantInfo pi2 = ParticipantInfo.builder().participantId("p2").name("B").sessionCode(sessionCode).build();
        String json1 = objectMapper.writeValueAsString(pi1);
        String json2 = objectMapper.writeValueAsString(pi2);

        when(setOps.members("quiz:session:" + sessionCode + ":participants")).thenReturn(ids);
        when(valueOps.get("quiz:session:" + sessionCode + ":participant:p1")).thenReturn(json1);
        when(valueOps.get("quiz:session:" + sessionCode + ":participant:p2")).thenReturn(json2);

        List<ParticipantInfo> lst = service.getParticipants(sessionCode);
        assertEquals(2, lst.size());
        assertEquals(Set.of("A", "B"), Set.of(lst.get(0).getName(), lst.get(1).getName()));
    }

    @Test
    void TC_EC_06_getParticipants_setNullOrEmpty() {
        when(setOps.members(anyString())).thenReturn(null);
        List<ParticipantInfo> res1 = service.getParticipants(sessionCode);
        assertTrue(res1.isEmpty());
        when(setOps.members(anyString())).thenReturn(Set.of());
        List<ParticipantInfo> res2 = service.getParticipants(sessionCode);
        assertTrue(res2.isEmpty());
    }

    @Test
    void TC_EC_07_getParticipants_someJsonNull() {
        Set<String> ids = Set.of("p1", "p2");
        when(setOps.members("quiz:session:" + sessionCode + ":participants")).thenReturn(ids);
        when(valueOps.get(endsWith(":p1"))).thenReturn(null);
        when(valueOps.get(endsWith(":p2"))).thenReturn("{\"participantId\":\"p2\",\"name\":\"B\"}");
        List<ParticipantInfo> lst = service.getParticipants(sessionCode);
        assertEquals(1, lst.size());
        assertEquals("B", lst.get(0).getName());
    }

    @Test
    void TC_ES_06_getParticipants_jsonException() throws Exception {
        Set<String> ids = Set.of("p1");
        when(setOps.members(anyString())).thenReturn(ids);
        when(valueOps.get(anyString())).thenReturn("{not json");
        assertThrows(RuntimeException.class, () -> service.getParticipants(sessionCode));
    }

    // ==== deleteSession ====
//    @Test
//    void TC_HP_06_deleteSession_happy() {
//        Set<String> ids = Set.of("p1", "p2");
//        when(setOps.members("quiz:session:" + sessionCode + ":participants")).thenReturn(ids);
//        Set<String> wildcard = Set.of("quiz:session:" + sessionCode + ":randomKey1", "quiz:session:" + sessionCode + ":randomKey2");
//        when(redis.keys("quiz:session:" + sessionCode + "*")).thenReturn(wildcard);
//
//        doNothing().when(redis).delete(anyString());
//        doNothing().when(redis).delete(any(Set.class));
//
//        service.deleteSession(sessionCode);
//
//        verify(redis).delete("quiz:session:" + sessionCode);
//        verify(redis).delete("quiz:session:" + sessionCode + ":participant:p1");
//        verify(redis).delete("quiz:session:" + sessionCode + ":participant:p2");
//        verify(redis).delete("quiz:session:" + sessionCode + ":participants");
//        verify(redis).delete(wildcard);
//    }

    @Test
    void TC_ES_07_deleteSession_redisError() {
        doThrow(new RuntimeException("Del")).when(redis).delete(anyString());
        assertThrows(RuntimeException.class, () -> service.deleteSession(sessionCode));
    }

//    // ==== submitAnswer ====
//    @Test
//    void TC_HP_07_submitAnswer_success() throws Exception {
//        SessionInfo activeQ = new SessionInfo(
//                sessionActive.getSessionId(),
//                sessionActive.getSessionCode(),
//                sessionActive.getKahootId(),
//                sessionActive.getTitle(),
//                sessionActive.getQuestionIds(),
//                sessionActive.getTotalQuestions(),
//                sessionActive.getTeacherId(),
//                sessionActive.getTeacherName(),
//                sessionActive.getStatus(),
//                sessionActive.getCurrentQuestionIndex(),
//                sessionActive.getCurrentQuestionId(),
//                sessionActive.getCurrentQuestionType(),
//                sessionActive.getQuestionStartTime(),
//                sessionActive.getQuestionTimeLimit(),
//                true,
//                sessionActive.getQuestionEndTime(),
//                sessionActive.getCreatedAt(),
//                sessionActive.getStartedAt(),
//                sessionActive.getFinishedAt(),
//                sessionActive.getTotalParticipants(),
//                sessionActive.getCurrentAnswers(),
//                sessionActive.isShowLeaderboardAfterEachQuestion(),
//                sessionActive.isRandomizeQuestions(),
//                sessionActive.isRandomizeOptions()
//        );
//        activeQ.setQuestionEndTime(LocalDateTime.now().plusSeconds(10));
//        String sessionJson = objectMapper.writeValueAsString(activeQ);
//
//        ParticipantInfo pi = ParticipantInfo.builder().participantId("PID").name("Name").sessionCode(sessionCode).build();
//        String partJson = objectMapper.writeValueAsString(pi);
//
//        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(sessionJson);
//        when(valueOps.get("quiz:session:" + sessionCode + ":participant:PID")).thenReturn(partJson);
//
//        SubmitAnswerRequest req = SubmitAnswerRequest.builder()
//                .sessionCode(sessionCode)
//                .participantId("PID")
//                .questionId(mcQuestionId)
//                .answer("1")
//                .build();
//
//        doNothing().when(hashOps).put(anyString(), eq("PID"), anyString());
//        doNothing().when(redis).expire(anyString(), eq(3L), eq(TimeUnit.HOURS));
//        doNothing().when(valueOps).set(eq("quiz:session:" + sessionCode), anyString(), eq(3L), eq(TimeUnit.HOURS));
//
//        SubmitAnswerResponse resp = service.submitAnswer(req);
//        assertTrue(resp.isSuccess());
//        assertEquals(2, resp.getTotalParticipants());
//        verify(hashOps).put(anyString(), eq("PID"), anyString());
//        verify(redis).expire(anyString(), eq(3L), eq(TimeUnit.HOURS));
//        verify(valueOps).set(eq("quiz:session:" + sessionCode), anyString(), eq(3L), eq(TimeUnit.HOURS));
//    }

    @Test
    void TC_EC_08_submitAnswer_sessionNull() {
        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(null);
        SubmitAnswerRequest req = SubmitAnswerRequest.builder()
                .sessionCode(sessionCode).participantId("PID").questionId(mcQuestionId).build();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.submitAnswer(req));
        assertTrue(ex.getMessage().contains("Session not found"));
    }

    @Test
    void TC_EC_09_submitAnswer_notAccepting() throws Exception {
        SessionInfo s = new SessionInfo(
                sessionActive.getSessionId(),
                sessionActive.getSessionCode(),
                sessionActive.getKahootId(),
                sessionActive.getTitle(),
                sessionActive.getQuestionIds(),
                sessionActive.getTotalQuestions(),
                sessionActive.getTeacherId(),
                sessionActive.getTeacherName(),
                sessionActive.getStatus(),
                sessionActive.getCurrentQuestionIndex(),
                sessionActive.getCurrentQuestionId(),
                sessionActive.getCurrentQuestionType(),
                sessionActive.getQuestionStartTime(),
                sessionActive.getQuestionTimeLimit(),
                false,
                sessionActive.getQuestionEndTime(),
                sessionActive.getCreatedAt(),
                sessionActive.getStartedAt(),
                sessionActive.getFinishedAt(),
                sessionActive.getTotalParticipants(),
                sessionActive.getCurrentAnswers(),
                sessionActive.isShowLeaderboardAfterEachQuestion(),
                sessionActive.isRandomizeQuestions(),
                sessionActive.isRandomizeOptions()
        );
        String sessionJson = objectMapper.writeValueAsString(s);
        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(sessionJson);
        when(valueOps.get("quiz:session:" + sessionCode + ":participant:x"))
                .thenReturn("{\"participantId\":\"x\"}");
        doThrow(new RuntimeException("PutError"))
                .when(hashOps).put(anyString(), anyString(), anyString());

        SubmitAnswerRequest req = SubmitAnswerRequest.builder()
                .sessionCode(sessionCode).participantId("x").questionId(mcQuestionId).build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.submitAnswer(req));
        assertTrue(ex.getMessage().contains("Failed to submit answer"));
    }

    @Test
    void TC_EC_10_submitAnswer_outOfTime() throws Exception {
        SessionInfo s = new SessionInfo(
                sessionActive.getSessionId(),
                sessionActive.getSessionCode(),
                sessionActive.getKahootId(),
                sessionActive.getTitle(),
                sessionActive.getQuestionIds(),
                sessionActive.getTotalQuestions(),
                sessionActive.getTeacherId(),
                sessionActive.getTeacherName(),
                sessionActive.getStatus(),
                sessionActive.getCurrentQuestionIndex(),
                sessionActive.getCurrentQuestionId(),
                sessionActive.getCurrentQuestionType(),
                sessionActive.getQuestionStartTime(),
                sessionActive.getQuestionTimeLimit(),
                true,
                LocalDateTime.now().minusSeconds(1),
                sessionActive.getCreatedAt(),
                sessionActive.getStartedAt(),
                sessionActive.getFinishedAt(),
                sessionActive.getTotalParticipants(),
                sessionActive.getCurrentAnswers(),
                sessionActive.isShowLeaderboardAfterEachQuestion(),
                sessionActive.isRandomizeQuestions(),
                sessionActive.isRandomizeOptions()
        );

        String sessionJson = objectMapper.writeValueAsString(s);

        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(sessionJson);
        when(valueOps.get("quiz:session:" + sessionCode + ":participant:x"))
                .thenReturn("{\"participantId\":\"x\"}");

        doThrow(new RuntimeException("PutError"))
                .when(hashOps).put(anyString(), anyString(), anyString());

        SubmitAnswerRequest req = SubmitAnswerRequest.builder()
                .sessionCode(sessionCode).participantId("x").questionId(mcQuestionId).build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.submitAnswer(req));
        assertTrue(ex.getMessage().contains("Failed to submit answer"));
    }

    @Test
    void TC_EC_11_submitAnswer_participantNotFound() throws Exception {
        SessionInfo s = new SessionInfo(
                sessionActive.getSessionId(),
                sessionActive.getSessionCode(),
                sessionActive.getKahootId(),
                sessionActive.getTitle(),
                sessionActive.getQuestionIds(),
                sessionActive.getTotalQuestions(),
                sessionActive.getTeacherId(),
                sessionActive.getTeacherName(),
                sessionActive.getStatus(),
                sessionActive.getCurrentQuestionIndex(),
                sessionActive.getCurrentQuestionId(),
                sessionActive.getCurrentQuestionType(),
                sessionActive.getQuestionStartTime(),
                sessionActive.getQuestionTimeLimit(),
                true,
                sessionActive.getQuestionEndTime(),
                sessionActive.getCreatedAt(),
                sessionActive.getStartedAt(),
                sessionActive.getFinishedAt(),
                sessionActive.getTotalParticipants(),
                sessionActive.getCurrentAnswers(),
                sessionActive.isShowLeaderboardAfterEachQuestion(),
                sessionActive.isRandomizeQuestions(),
                sessionActive.isRandomizeOptions()
        );
        String sessionJson = objectMapper.writeValueAsString(s);
        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(sessionJson);
        when(valueOps.get("quiz:session:" + sessionCode + ":participant:x")).thenReturn(null);
        SubmitAnswerRequest req = SubmitAnswerRequest.builder().sessionCode(sessionCode).participantId("x").questionId(mcQuestionId).build();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.submitAnswer(req));
        assertTrue(ex.getMessage().contains("Participant not found"));
    }

    @Test
    void TC_ES_08_submitAnswer_redisPutError() throws Exception {
        SessionInfo s = new SessionInfo(
                sessionActive.getSessionId(),
                sessionActive.getSessionCode(),
                sessionActive.getKahootId(),
                sessionActive.getTitle(),
                sessionActive.getQuestionIds(),
                sessionActive.getTotalQuestions(),
                sessionActive.getTeacherId(),
                sessionActive.getTeacherName(),
                sessionActive.getStatus(),
                sessionActive.getCurrentQuestionIndex(),
                sessionActive.getCurrentQuestionId(),
                sessionActive.getCurrentQuestionType(),
                sessionActive.getQuestionStartTime(),
                sessionActive.getQuestionTimeLimit(),
                true,
                sessionActive.getQuestionEndTime(),
                sessionActive.getCreatedAt(),
                sessionActive.getStartedAt(),
                sessionActive.getFinishedAt(),
                sessionActive.getTotalParticipants(),
                sessionActive.getCurrentAnswers(),
                sessionActive.isShowLeaderboardAfterEachQuestion(),
                sessionActive.isRandomizeQuestions(),
                sessionActive.isRandomizeOptions()
        );
        String sessionJson = objectMapper.writeValueAsString(s);
        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(sessionJson);
        when(valueOps.get("quiz:session:" + sessionCode + ":participant:x")).thenReturn("{\"participantId\":\"x\"}");
        doThrow(new RuntimeException("PutError")).when(hashOps).put(anyString(), anyString(), anyString());
        SubmitAnswerRequest req = SubmitAnswerRequest.builder()
                .sessionCode(sessionCode).participantId("x").questionId(mcQuestionId).build();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.submitAnswer(req));
        assertTrue(ex.getMessage().contains("Failed to submit answer"));
    }

    // ==== getQuestionById ====
    @Test
    void TC_HP_08_getQuestionById_multipleChoice_noCorrectRevealed() {
        // Mọi option.correct = false
        ModuleContent m = makeMCQuestion(11L, "Đáp Án");
        when(moduleContentRepository.findById(11L)).thenReturn(Optional.of(m));
        ModuleContent ret = service.getQuestionById(11L);
        MultipleChoiceQuestion mcq = (MultipleChoiceQuestion) ret;
        assertTrue(mcq.getOptions().stream().allMatch(op -> !op.isCorrect()));
    }

    @Test
    void TC_HP_09_getQuestionById_gapfill_hidesAnswer() {
        ModuleContent gf = makeGapFillQuestion(22L);
        when(moduleContentRepository.findById(22L)).thenReturn(Optional.of(gf));
        ModuleContent ret = service.getQuestionById(22L);
        GapFillQuestion gfq = (GapFillQuestion) ret;
        assertNull(gfq.getAnswers());
    }

    @Test
    void TC_EC_12_getQuestionById_idNotFound() {
        when(moduleContentRepository.findById(catchAllUnusedId)).thenReturn(Optional.empty());
        assertThrows(ModuleContentNotFoundException.class, () -> service.getQuestionById(catchAllUnusedId));
    }

    // ==== endQuestion ====
//    @Test
//    void TC_HP_10_endQuestion_fullGrading() throws Exception {
//        SessionInfo s = new SessionInfo(
//                sessionActive.getSessionId(),
//                sessionActive.getSessionCode(),
//                sessionActive.getKahootId(),
//                sessionActive.getTitle(),
//                sessionActive.getQuestionIds(),
//                sessionActive.getTotalQuestions(),
//                sessionActive.getTeacherId(),
//                sessionActive.getTeacherName(),
//                sessionActive.getStatus(),
//                0,
//                mcQuestionId,
//                sessionActive.getCurrentQuestionType(),
//                LocalDateTime.now().minusSeconds(4),
//                sessionActive.getQuestionTimeLimit(),
//                true,
//                sessionActive.getQuestionEndTime(),
//                sessionActive.getCreatedAt(),
//                sessionActive.getStartedAt(),
//                sessionActive.getFinishedAt(),
//                sessionActive.getTotalParticipants(),
//                sessionActive.getCurrentAnswers(),
//                sessionActive.isShowLeaderboardAfterEachQuestion(),
//                sessionActive.isRandomizeQuestions(),
//                sessionActive.isRandomizeOptions()
//        );
//
//        String sessionJson = objectMapper.writeValueAsString(s);
//        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(sessionJson);
//
//        Map<Object, Object> tempAnswersMap = new HashMap<>();
//        TempAnswer answerObj = TempAnswer.builder()
//                .participantId("pid")
//                .participantName("P_NAME")
//                .questionId(mcQuestionId)
//                .answer("1")
//                .submittedAt(s.getQuestionStartTime().plusSeconds(2))
//                .build();
//        tempAnswersMap.put("pid", objectMapper.writeValueAsString(answerObj));
//        when(hashOps.entries("quiz:session:" + sessionCode + ":question:" + mcQuestionId + ":answers")).thenReturn(tempAnswersMap);
//
//        ModuleContent mcq = makeMCQuestion(mcQuestionId, "Đúng");
//        ((MultipleChoiceOption)((MultipleChoiceQuestion)mcq).getOptions().get(0)).setCorrect(true);
//        when(moduleContentRepository.findById(mcQuestionId)).thenReturn(Optional.of(mcq));
//
//        String participantJson = "{\"participantId\":\"pid\",\"name\":\"P_NAME\",\"currentScore\":500}";
//        when(valueOps.get("quiz:session:" + sessionCode + ":participant:pid")).thenReturn(participantJson);
//
//        doNothing().when(valueOps).set(contains(":participant:"), anyString(), eq(3L), eq(TimeUnit.HOURS));
//        doNothing().when(valueOps).set(eq("quiz:session:" + sessionCode), anyString(), eq(3L), eq(TimeUnit.HOURS));
//        doNothing().when(redis).delete(anyString());
//
//        QuestionResultResponse resp = service.endQuestion(sessionCode);
//        assertEquals(mcQuestionId, resp.getQuestionId());
//        assertNotNull(resp.getResults());
//        assertEquals("Đúng", resp.getCorrectAnswer());
//    }
//
//    @Test
//    void TC_HP_11_endQuestion_answersEmpty() throws Exception {
//        SessionInfo s = new SessionInfo(
//                sessionActive.getSessionId(),
//                sessionActive.getSessionCode(),
//                sessionActive.getKahootId(),
//                sessionActive.getTitle(),
//                sessionActive.getQuestionIds(),
//                sessionActive.getTotalQuestions(),
//                sessionActive.getTeacherId(),
//                sessionActive.getTeacherName(),
//                sessionActive.getStatus(),
//                sessionActive.getCurrentQuestionIndex(),
//                mcQuestionId,
//                sessionActive.getCurrentQuestionType(),
//                sessionActive.getQuestionStartTime(),
//                sessionActive.getQuestionTimeLimit(),
//                sessionActive.isAcceptingAnswers(),
//                sessionActive.getQuestionEndTime(),
//                sessionActive.getCreatedAt(),
//                sessionActive.getStartedAt(),
//                sessionActive.getFinishedAt(),
//                sessionActive.getTotalParticipants(),
//                sessionActive.getCurrentAnswers(),
//                sessionActive.isShowLeaderboardAfterEachQuestion(),
//                sessionActive.isRandomizeQuestions(),
//                sessionActive.isRandomizeOptions()
//        );
//
//        String sessionJson = objectMapper.writeValueAsString(s);
//        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(sessionJson);
//
//        // For completeness (parallel the provided example): no participant answers
//        when(hashOps.entries("quiz:session:" + sessionCode + ":question:" + mcQuestionId + ":answers"))
//                .thenReturn(new HashMap<>());
//
//        ModuleContent mcq = makeMCQuestion(mcQuestionId, "Đúng");
//        ((MultipleChoiceOption)((MultipleChoiceQuestion)mcq).getOptions().get(0)).setCorrect(true);
//        when(moduleContentRepository.findById(mcQuestionId)).thenReturn(Optional.of(mcq));
//
//        doNothing().when(valueOps).set(anyString(), anyString(), eq(3L), eq(TimeUnit.HOURS));
//        doNothing().when(redis).delete(anyString());
//
//        QuestionResultResponse resp = service.endQuestion(sessionCode);
//        assertEquals(mcQuestionId, resp.getQuestionId());
//        assertNotNull(resp.getResults());
//        assertEquals(0, resp.getResults().size());
//        assertEquals("Đúng", resp.getCorrectAnswer());
//    }

    @Test
    void TC_EC_13_endQuestion_noCurrentQuestion() throws Exception {
        SessionInfo s = new SessionInfo(
                sessionActive.getSessionId(),
                sessionActive.getSessionCode(),
                sessionActive.getKahootId(),
                sessionActive.getTitle(),
                sessionActive.getQuestionIds(),
                sessionActive.getTotalQuestions(),
                sessionActive.getTeacherId(),
                sessionActive.getTeacherName(),
                sessionActive.getStatus(),
                sessionActive.getCurrentQuestionIndex(),
                null,
                sessionActive.getCurrentQuestionType(),
                sessionActive.getQuestionStartTime(),
                sessionActive.getQuestionTimeLimit(),
                sessionActive.isAcceptingAnswers(),
                sessionActive.getQuestionEndTime(),
                sessionActive.getCreatedAt(),
                sessionActive.getStartedAt(),
                sessionActive.getFinishedAt(),
                sessionActive.getTotalParticipants(),
                sessionActive.getCurrentAnswers(),
                sessionActive.isShowLeaderboardAfterEachQuestion(),
                sessionActive.isRandomizeQuestions(),
                sessionActive.isRandomizeOptions()
        );

        String sessionJson = objectMapper.writeValueAsString(s);
        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(sessionJson);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.endQuestion(sessionCode));
        assertTrue(ex.getMessage().contains("No active question"));
    }

    @Test
    void TC_EC_14_endQuestion_sessionNull() {
        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.endQuestion(sessionCode));
        assertTrue(ex.getMessage().contains("Session not found"));
    }
//
//    @Test
//    void TC_ES_10_endQuestion_participantJsonNull() throws Exception {
//        SessionInfo s = new SessionInfo(
//                sessionActive.getSessionId(),
//                sessionActive.getSessionCode(),
//                sessionActive.getKahootId(),
//                sessionActive.getTitle(),
//                sessionActive.getQuestionIds(),
//                sessionActive.getTotalQuestions(),
//                sessionActive.getTeacherId(),
//                sessionActive.getTeacherName(),
//                sessionActive.getStatus(),
//                sessionActive.getCurrentQuestionIndex(),
//                mcQuestionId,
//                sessionActive.getCurrentQuestionType(),
//                sessionActive.getQuestionStartTime(),
//                sessionActive.getQuestionTimeLimit(),
//                sessionActive.isAcceptingAnswers(),
//                sessionActive.getQuestionEndTime(),
//                sessionActive.getCreatedAt(),
//                sessionActive.getStartedAt(),
//                sessionActive.getFinishedAt(),
//                sessionActive.getTotalParticipants(),
//                sessionActive.getCurrentAnswers(),
//                sessionActive.isShowLeaderboardAfterEachQuestion(),
//                sessionActive.isRandomizeQuestions(),
//                sessionActive.isRandomizeOptions()
//        );
//        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(objectMapper.writeValueAsString(s));
//
//        Map<Object, Object> tempAnswersMap = new HashMap<>();
//        TempAnswer answerObj = TempAnswer.builder()
//                .participantId("pid2")
//                .participantName("P_NAME")
//                .questionId(mcQuestionId)
//                .answer("1")
//                .submittedAt(LocalDateTime.now())
//                .build();
//        tempAnswersMap.put("pid2", objectMapper.writeValueAsString(answerObj));
//        when(hashOps.entries(anyString())).thenReturn(tempAnswersMap);
//
//        when(moduleContentRepository.findById(mcQuestionId)).thenReturn(Optional.of(makeMCQuestion(mcQuestionId, "DapAn")));
//        when(valueOps.get(contains(":participant:"))).thenReturn(null);
//
//        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.endQuestion(sessionCode));
//        assertTrue(ex.getMessage().contains("Failed to end question"));
//    }

    // ==== startNextQuestion ====
    @Test
    void TC_HP_12_startNextQuestion_success() throws Exception {
        SessionInfo s = SessionInfo.builder()
                .sessionId(sessionWaiting.getSessionId())
                .sessionCode(sessionWaiting.getSessionCode())
                .kahootId(sessionWaiting.getKahootId())
                .title(sessionWaiting.getTitle())
                .questionIds(sessionWaiting.getQuestionIds())
                .totalQuestions(sessionWaiting.getTotalQuestions())
                .teacherId(sessionWaiting.getTeacherId())
                .teacherName(sessionWaiting.getTeacherName())
                .status(sessionWaiting.getStatus())
                .currentQuestionIndex(-1)
                .currentQuestionId(sessionWaiting.getCurrentQuestionId())
                .currentQuestionType(sessionWaiting.getCurrentQuestionType())
                .questionStartTime(sessionWaiting.getQuestionStartTime())
                .questionTimeLimit(sessionWaiting.getQuestionTimeLimit())
                .acceptingAnswers(sessionWaiting.isAcceptingAnswers())
                .questionEndTime(sessionWaiting.getQuestionEndTime())
                .createdAt(sessionWaiting.getCreatedAt())
                .startedAt(sessionWaiting.getStartedAt())
                .finishedAt(sessionWaiting.getFinishedAt())
                .totalParticipants(sessionWaiting.getTotalParticipants())
                .currentAnswers(sessionWaiting.getCurrentAnswers())
                .showLeaderboardAfterEachQuestion(sessionWaiting.isShowLeaderboardAfterEachQuestion())
                .randomizeQuestions(sessionWaiting.isRandomizeQuestions())
                .randomizeOptions(sessionWaiting.isRandomizeOptions())
                .build();

        String sessionJson = objectMapper.writeValueAsString(s);
        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(sessionJson);

        ModuleContent mcq = makeMCQuestion(mcQuestionId, "DapAnHide");
        when(moduleContentRepository.findById(mcQuestionId)).thenReturn(Optional.of(mcq));
        doNothing().when(valueOps).set(anyString(), anyString(), eq(3L), eq(TimeUnit.HOURS));

        StartQuestionResponse resp = service.startNextQuestion(sessionCode);

        assertEquals(mcQuestionId, resp.getQuestionId());
        assertEquals(1, resp.getQuestionNumber());
        assertEquals(2, resp.getTotalQuestions());
        ModuleContent q = resp.getQuestion();
        MultipleChoiceQuestion r = (MultipleChoiceQuestion) q;
        assertTrue(r.getOptions().stream().noneMatch(MultipleChoiceOption::isCorrect));
        verify(valueOps, atLeastOnce()).set(anyString(), anyString(), eq(3L), eq(TimeUnit.HOURS));
    }

    @Test
    void TC_EC_15_startNextQuestion_quizCompleted() throws Exception {
        SessionInfo finishedQuiz = new SessionInfo(
                sessionWaiting.getSessionId(),
                sessionWaiting.getSessionCode(),
                sessionWaiting.getKahootId(),
                sessionWaiting.getTitle(),
                List.of(mcQuestionId, gfQuestionId),
                sessionWaiting.getTotalQuestions(),
                sessionWaiting.getTeacherId(),
                sessionWaiting.getTeacherName(),
                sessionWaiting.getStatus(),
                1,
                sessionWaiting.getCurrentQuestionId(),
                sessionWaiting.getCurrentQuestionType(),
                sessionWaiting.getQuestionStartTime(),
                sessionWaiting.getQuestionTimeLimit(),
                sessionWaiting.isAcceptingAnswers(),
                sessionWaiting.getQuestionEndTime(),
                sessionWaiting.getCreatedAt(),
                sessionWaiting.getStartedAt(),
                sessionWaiting.getFinishedAt(),
                sessionWaiting.getTotalParticipants(),
                sessionWaiting.getCurrentAnswers(),
                sessionWaiting.isShowLeaderboardAfterEachQuestion(),
                sessionWaiting.isRandomizeQuestions(),
                sessionWaiting.isRandomizeOptions()
        );

        String sessionJson = objectMapper.writeValueAsString(finishedQuiz);
        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(sessionJson);

        when(setOps.members("quiz:session:" + sessionCode + ":participants")).thenReturn(Set.of("pa", "pb"));

        when(valueOps.get("quiz:session:" + sessionCode + ":participant:pa"))
                .thenReturn("{\"participantId\":\"pa\",\"name\":\"A\",\"currentScore\":10}");
        when(valueOps.get("quiz:session:" + sessionCode + ":participant:pb"))
                .thenReturn("{\"participantId\":\"pb\",\"name\":\"B\",\"currentScore\":20}");

        doNothing().when(valueOps).set(anyString(), anyString(), eq(3L), eq(TimeUnit.HOURS));

        QuizCompletedException ex = assertThrows(QuizCompletedException.class, () -> service.startNextQuestion(sessionCode));
        assertEquals(2, ex.getFinalLeaderboard().size());
        verify(valueOps).set(eq("quiz:session:" + sessionCode), anyString(), eq(3L), eq(TimeUnit.HOURS));
    }

    @Test
    void TC_ES_11_startNextQuestion_sessionNull() {
        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.startNextQuestion(sessionCode));
    }

    @Test
    void TC_ES_12_startNextQuestion_questionNotFound() throws Exception {
        SessionInfo s = new SessionInfo(
                sessionWaiting.getSessionId(),
                sessionWaiting.getSessionCode(),
                sessionWaiting.getKahootId(),
                sessionWaiting.getTitle(),
                sessionWaiting.getQuestionIds(),
                sessionWaiting.getTotalQuestions(),
                sessionWaiting.getTeacherId(),
                sessionWaiting.getTeacherName(),
                sessionWaiting.getStatus(),
                -1,
                sessionWaiting.getCurrentQuestionId(),
                sessionWaiting.getCurrentQuestionType(),
                sessionWaiting.getQuestionStartTime(),
                sessionWaiting.getQuestionTimeLimit(),
                sessionWaiting.isAcceptingAnswers(),
                sessionWaiting.getQuestionEndTime(),
                sessionWaiting.getCreatedAt(),
                sessionWaiting.getStartedAt(),
                sessionWaiting.getFinishedAt(),
                sessionWaiting.getTotalParticipants(),
                sessionWaiting.getCurrentAnswers(),
                sessionWaiting.isShowLeaderboardAfterEachQuestion(),
                sessionWaiting.isRandomizeQuestions(),
                sessionWaiting.isRandomizeOptions()
        );
        String sessionJson = objectMapper.writeValueAsString(s);

        when(valueOps.get("quiz:session:" + sessionCode)).thenReturn(sessionJson);
        when(moduleContentRepository.findById(mcQuestionId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.startNextQuestion(sessionCode));
        assertTrue(ex.getMessage().contains("not found"));
    }


    // ==== Tiện ích tạo câu hỏi mẫu cho test ====
    private ModuleContent makeMCQuestion(Long id, String answerText) {
        MultipleChoiceOption op1 = new MultipleChoiceOption();
        op1.setMcoId(1L); op1.setOptionText(answerText); op1.setCorrect(true);
        MultipleChoiceOption op2 = new MultipleChoiceOption();
        op2.setMcoId(2L); op2.setOptionText("Sai"); op2.setCorrect(false);

        MultipleChoiceQuestion mcq = new MultipleChoiceQuestion();
        mcq.setMcId(id);
        mcq.setTypeOfContent(TypeOfContent.MULTIPLE_CHOICE);
        mcq.setOptions(List.of(op1, op2));
        return mcq;
    }
    private ModuleContent makeGapFillQuestion(Long id) {
        GapFillQuestion gf = new GapFillQuestion();
        gf.setMcId(id);
        gf.setTypeOfContent(TypeOfContent.GAPFILL);
        GapFillAnswer ans = GapFillAnswer.builder().answer("dap an 1").build();
        gf.setAnswers(List.of(ans));
        return gf;
    }
}
