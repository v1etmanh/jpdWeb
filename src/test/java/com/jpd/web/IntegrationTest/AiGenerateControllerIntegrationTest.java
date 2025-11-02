package com.jpd.web.IntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpd.web.controller.creator.AiGenerateController;
import com.jpd.web.controller.common.GlobalExceptionHandler;
import com.jpd.web.dto.GenerateFeedbackForm;
import com.jpd.web.model.CreatorRequestNumber;
import com.jpd.web.repository.CreatorRequestNumberRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.service.AIService;
import com.jpd.web.service.FireBaseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AiGenerateController.class)
@ContextConfiguration(classes = {AiGenerateController.class, GlobalExceptionHandler.class})
class AiGenerateControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AIService aiService;

    @MockitoBean
    CreatorRequestNumberRepository creatorRequestNumberRepository;

    @MockitoBean
    FireBaseService fireBaseService;

    @MockitoBean
    CustomerRepository customerRepository;

    @MockitoBean
    JwtDecoder jwtDecoder; // cực kỳ quan trọng để Spring Security không 401

    final String BASE_URL = "/api/creator/AI/generateFeeback";
    final long CREATOR_ID = 1L;

    // JWT mock với email + authority
    private static JwtRequestPostProcessor jwtWithEmail(String email) {
        return jwt().jwt(j -> j.claim("email", email))
                .authorities(new SimpleGrantedAuthority("SCOPE_creator.write"));
    }

    @Nested
    @DisplayName("POST /api/creator/AI/generateFeeback")
    class GenerateFeedback {

        @Test
        @DisplayName("🎯 201 Created when valid request")
        void happyPath() throws Exception {
            // Mock JwtDecoder để Spring Security chấp nhận JWT
            when(jwtDecoder.decode(anyString()))
                    .thenReturn(Jwt.withTokenValue("token")
                            .header("alg", "none")
                            .claim("email", "user@test.com")
                            .build()
                    );

            GenerateFeedbackForm form = new GenerateFeedbackForm();
            form.setQuestion("What is Java?");
            form.setAnswer("Java is a language");
            String email = "user@test.com";

            when(aiService.generateFeedback(form.getQuestion(), form.getAnswer()))
                    .thenReturn("AI Generated Feedback");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form))
                            .with(jwtWithEmail(email))) // JWT mock
                    .andExpect(status().isCreated())
                    .andExpect(content().string("AI Generated Feedback"));

            verify(aiService).generateFeedback(form.getQuestion(), form.getAnswer());
        }

//        @Test
//        @WithMockUser(username = "testuser", roles = {"CREATOR"})
//        void missingQuestion() throws Exception {
//            GenerateFeedbackForm form = new GenerateFeedbackForm();
//            form.setAnswer("answer only");
//            form.setQuestion(null);
//            mockMvc.perform(post("/api/creator/AI/generateFeeback")
//                            .with(csrf())
//                            .with(user("testuser").roles("CREATOR"))
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(form)))
//                    .andExpect(status().isCreated()); // hoặc is5xx nếu muốn 500
//
//
//            verifyNoInteractions(aiService);
//        }


        @Test
        @DisplayName("🚫 Missing answer (controller does not validate)")
        void missingAnswer() throws Exception {
            GenerateFeedbackForm form = new GenerateFeedbackForm();
            form.setQuestion("Question only");

            when(aiService.generateFeedback(anyString(), any()))
                    .thenReturn("AI Generated Feedback");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form))
                            .with(jwtWithEmail("user@test.com")))
                    .andExpect(status().isCreated())  // 201, vì controller luôn trả 201
                    .andExpect(content().string("AI Generated Feedback"));

            verify(aiService).generateFeedback(form.getQuestion(), form.getAnswer());
        }



        @Test
        @DisplayName("✅ 201 Created even when JWT missing email claim (current behavior)")
        void missingEmailClaim() throws Exception {
            GenerateFeedbackForm form = new GenerateFeedbackForm();
            form.setQuestion("Q");
            form.setAnswer("A");

            // JWT giả lập thiếu email (controller không check, nên vẫn 201)
            mockMvc.perform(post("/api/creator/AI/generateFeeback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form))
                            .with(jwt().jwt(jwt -> jwt.claim("email", null))))
                    .andExpect(status().isCreated());  // chấp nhận hành vi hiện tại

            verify(aiService).generateFeedback(form.getQuestion(), form.getAnswer());
        }


        @Test
        @DisplayName("🚫 500 Internal Server Error when service throws RuntimeException")
        void serviceThrowsRuntime() throws Exception {
            String email = "user@test.com";

            when(aiService.generateFeedback(anyString(), anyString()))
                    .thenThrow(new RuntimeException("AI error"));

            GenerateFeedbackForm form = new GenerateFeedbackForm();
            form.setQuestion("Q");
            form.setAnswer("A");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form))
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("AI error"));

            verify(aiService).generateFeedback(form.getQuestion(), form.getAnswer());
        }

        @Test
        @DisplayName("🎯 Logic test: canMakeRequest() returns true and saves record")
        void canMakeRequest_FirstRequest() {
            AiGenerateController controller = new AiGenerateController();
            injectMocks(controller);

            when(creatorRequestNumberRepository.findByCreatorId(CREATOR_ID))
                    .thenReturn(Optional.empty());

            boolean result = controller.canMakeRequest(CREATOR_ID);
            assertThat(result).isTrue();

            ArgumentCaptor<CreatorRequestNumber> captor = ArgumentCaptor.forClass(CreatorRequestNumber.class);
            verify(creatorRequestNumberRepository).save(captor.capture());
            assertThat(captor.getValue().getCreatorId()).isEqualTo(CREATOR_ID);
            assertThat(captor.getValue().getNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("🚫 Logic test: canMakeRequest() returns false if reached limit")
        void canMakeRequest_ExceedLimit() {
            AiGenerateController controller = new AiGenerateController();
            injectMocks(controller);

            CreatorRequestNumber record = CreatorRequestNumber.builder()
                    .creatorId(CREATOR_ID)
                    .number(5)
                    .lastUpdate(LocalDateTime.now())
                    .build();

            when(creatorRequestNumberRepository.findByCreatorId(CREATOR_ID))
                    .thenReturn(Optional.of(record));

            boolean result = controller.canMakeRequest(CREATOR_ID);
            assertThat(result).isFalse();
        }

        private void injectMocks(AiGenerateController controller) {
            try {
                var aiField = AiGenerateController.class.getDeclaredField("aiService");
                aiField.setAccessible(true);
                aiField.set(controller, aiService);

                var repoField = AiGenerateController.class.getDeclaredField("creatorRequestNumberRepository");
                repoField.setAccessible(true);
                repoField.set(controller, creatorRequestNumberRepository);

                var fbField = AiGenerateController.class.getDeclaredField("fireBaseService");
                fbField.setAccessible(true);
                fbField.set(controller, fireBaseService);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
