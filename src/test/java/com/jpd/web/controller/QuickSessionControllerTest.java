package com.jpd.web.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpd.web.dto.QuestionResultResponse;
import com.jpd.web.dto.SubmitAnswerRequest;
import com.jpd.web.dto.SubmitAnswerResponse;
import com.jpd.web.model.*;
import com.jpd.web.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class QuickSessionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private QuizSessionController controller;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ------- createSession -------
    @Test
    void testCreateSession_success() throws Exception {
        CreateSessionRequest req = new CreateSessionRequest(123L, "TeacherName");
        CreateSessionResponse resp = CreateSessionResponse.builder()
                .sessionCode("ABC123")
                .sessionId(UUID.randomUUID().toString())
                .title("Quiz 1")
                .totalQuestions(10)
                .qrCodeUrl("qr.png")
                .joinUrl("url.com").build();
        when(sessionService.createSession(Mockito.any(CreateSessionRequest.class), Mockito.anyLong())).thenReturn(resp);

        // Inject a fake RequestAttributeExtractor via mock, or just set attribute and let it be extracted
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("creatorId", 42L);

        mockMvc.perform(post("/api/quiz/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .requestAttr("creatorId", 42L)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionCode", is("ABC123")))
                .andExpect(jsonPath("$.totalQuestions", is(10)));

        verify(sessionService, times(1)).createSession(Mockito.any(CreateSessionRequest.class), Mockito.eq(42L));
    }

    @Test
    void testCreateSession_serviceThrows_returns400() throws Exception {
        CreateSessionRequest req = new CreateSessionRequest(123L, "t");
        when(sessionService.createSession(any(), anyLong())).thenThrow(new RuntimeException("fail"));
        mockMvc.perform(post("/api/quiz/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .requestAttr("creatorId", 42L)
                )
                .andExpect(status().isBadRequest());
    }

    // ------- joinSession -------

    @Test
    void testJoinSession_success() throws Exception {
        JoinSessionRequest joinReq = new JoinSessionRequest("CODE123", "Alice");
        ParticipantInfo part = ParticipantInfo.builder()
                .participantId("p1")
                .name("Alice")
                .sessionCode("CODE123")
                .currentScore(0)
                .build();
        SessionInfo sessionInfo = SessionInfo.builder()
                .sessionCode("CODE123")
                .status(SessionStatus.WAITING)
                .totalQuestions(5)
                .build();
        when(sessionService.getSession("CODE123")).thenReturn(sessionInfo);
        when(sessionService.joinSession(any())).thenReturn(part);

        mockMvc.perform(
                post("/api/quiz/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinReq))
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.participant.name", is("Alice")))
                .andExpect(jsonPath("$.session.sessionCode", is("CODE123")));
    }

    @Test
    void testJoinSession_sessionNotFound() throws Exception {
        JoinSessionRequest req = new JoinSessionRequest("NOPE", "Jessie");
        when(sessionService.getSession("NOPE")).thenReturn(null);
        mockMvc.perform(
                post("/api/quiz/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
        ).andExpect(status().isBadRequest())
         .andExpect(jsonPath("$.success", is(false)))
         .andExpect(jsonPath("$.message", containsString("Session not found")));
    }

    @Test
    void testJoinSession_statusFinished() throws Exception {
        JoinSessionRequest req = new JoinSessionRequest("CODE", "Tee");
        SessionInfo session = SessionInfo.builder()
                .sessionCode("CODE")
                .status(SessionStatus.FINISHED)
                .build();
        when(sessionService.getSession("CODE")).thenReturn(session);
        mockMvc.perform(
                post("/api/quiz/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
        ).andExpect(status().isBadRequest())
         .andExpect(jsonPath("$.message", containsString("already finished")));
    }

    @Test
    void testJoinSession_statusActive() throws Exception {
        JoinSessionRequest req = new JoinSessionRequest("CODE", "Boo");
        SessionInfo session = SessionInfo.builder()
                .sessionCode("CODE")
                .status(SessionStatus.ACTIVE)
                .build();
        when(sessionService.getSession("CODE")).thenReturn(session);
        mockMvc.perform(
                post("/api/quiz/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
        ).andExpect(status().isBadRequest())
         .andExpect(jsonPath("$.message", containsString("already started")));
    }

    @Test
    void testJoinSession_serviceThrows_errorHandled() throws Exception {
        JoinSessionRequest req = new JoinSessionRequest("X", "Fa");
        SessionInfo sessionInfo = SessionInfo.builder()
                .sessionCode("X")
                .status(SessionStatus.WAITING)
                .build();
        when(sessionService.getSession("X")).thenReturn(sessionInfo);
        when(sessionService.joinSession(any())).thenThrow(new RuntimeException("errrrr"));
        mockMvc.perform(
                post("/api/quiz/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
        ).andExpect(status().isBadRequest())
         .andExpect(jsonPath("$.success", is(false)))
         .andExpect(jsonPath("$.message", containsString("errrrr")));
    }

    // ------- getSession -------
    @Test
    void testGetSession_ok() throws Exception {
        SessionInfo session = SessionInfo.builder()
                .sessionCode("ABBA")
                .teacherName("GVR")
                .totalQuestions(24)
                .build();
        when(sessionService.getSession("ABBA")).thenReturn(session);
        mockMvc.perform(get("/api/quiz/ABBA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionCode", is("ABBA")))
                .andExpect(jsonPath("$.teacherName", is("GVR")))
                .andExpect(jsonPath("$.totalQuestions", is(24)));
    }

    @Test
    void testGetSession_notFound() throws Exception {
        when(sessionService.getSession("YAYA")).thenReturn(null);
        mockMvc.perform(get("/api/quiz/YAYA"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetSession_exception() throws Exception {
        when(sessionService.getSession("ERR")).thenThrow(new RuntimeException("broken"));
        mockMvc.perform(get("/api/quiz/ERR"))
                .andExpect(status().isBadRequest());
    }

    // ------- getParticipants -------
    @Test
    void testGetParticipants_success() throws Exception {
        List<ParticipantInfo> list = Arrays.asList(
                ParticipantInfo.builder().participantId("x").name("X").currentScore(3).build(),
                ParticipantInfo.builder().participantId("y").name("Y").currentScore(9).build()
        );
        when(sessionService.getParticipants("HAPPY")).thenReturn(list);

        mockMvc.perform(get("/api/quiz/HAPPY/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("X")))
                .andExpect(jsonPath("$[1].name", is("Y")));
    }

    @Test
    void testGetParticipants_exception() throws Exception {
        when(sessionService.getParticipants("BAD")).thenThrow(new RuntimeException("Trouble!"));
        mockMvc.perform(get("/api/quiz/BAD/participants"))
                .andExpect(status().isBadRequest());
    }

    // ------- deleteSession -------
    @Test
    void testDeleteSession_ok() throws Exception {
        doNothing().when(sessionService).deleteSession("DEL02");
        mockMvc.perform(delete("/api/quiz/DEL02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Session deleted")));
        verify(sessionService).deleteSession("DEL02");
    }

    @Test
    void testDeleteSession_exception() throws Exception {
        doThrow(new RuntimeException("Oops")).when(sessionService).deleteSession("DEL03");
        mockMvc.perform(delete("/api/quiz/DEL03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ------- submitAnswer -------
    @Test
    void testSubmitAnswer_ok() throws Exception {
        SubmitAnswerRequest req = SubmitAnswerRequest.builder()
                .sessionCode("ABC")
                .participantId("p1")
                .questionId(113L)
                .answer("A").build();
        SubmitAnswerResponse resp = SubmitAnswerResponse.builder()
                .success(true)
                .message("OK")
                .totalAnswered(4)
                .totalParticipants(6)
                .build();
        when(sessionService.submitAnswer(any())).thenReturn(resp);

        mockMvc.perform(post("/api/quiz/submit-answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.totalAnswered", is(4)))
                .andExpect(jsonPath("$.totalParticipants", is(6)))
                .andExpect(jsonPath("$.message", is("OK")));
    }



    // ------- endQuestion -------
    @Test
    void testEndQuestion_ok() throws Exception {
        QuestionResultResponse response = QuestionResultResponse.builder()
                .questionId(1L)
                .correctAnswer("A")
                .build();
        when(sessionService.endQuestion("FIN")).thenReturn(response);

        mockMvc.perform(post("/api/quiz/end-question/FIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId", is(1)))
                .andExpect(jsonPath("$.correctAnswer", is("A")));
    }

}
