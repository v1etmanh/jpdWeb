package com.jpd.web.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpd.web.controller.common.GlobalExceptionHandler;
import com.jpd.web.dto.CustomerSimpleDto;
import com.jpd.web.dto.FeedbackSimpleDto;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.ExceedLimitRequestException;
import com.jpd.web.exception.FeedBackIligalException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.service.FeedbackService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@NestedTestConfiguration(INHERIT)
@WebMvcTest(controllers = FeedbackController.class)
@Import({ GlobalExceptionHandler.class })
@DisplayName("FeedbackControllerTest - Controller Layer")
public class FeedbackControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FeedbackService feedbackService;

    @MockitoBean
    JwtDecoder jwtDecoder;
    @MockitoBean
    CustomerRepository customerRepository;
    @Autowired
    ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/customer/feedback/{courseId}";

    // Helper for JWT with email claim
    static JwtRequestPostProcessor jwtWithEmail(String email) {
        return jwt().jwt(jwt -> jwt.claim("email", email));
    }

    // --- HAPPY PATHS ---

    @Nested
    @DisplayName("POST /api/customer/feedback/{courseId}")
    class AddFeedback {

        @Test
        @DisplayName("TC_HP_01: 204 when valid data, JWT has 'email', service ok")
        void postFeedback_HappyPath() throws Exception {
            long courseId = 1L;
            String email = "a@email.com";
            String detail = "good job";
            int rate = 5;
            willDoNothing().given(feedbackService).addFeedback(email, courseId, detail, rate);

            mockMvc.perform(post(BASE_URL, courseId)
                    .param("rate", String.valueOf(rate))
                    .param("detail", detail)
                    .with(jwtWithEmail(email)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
            verify(feedbackService).addFeedback(email, courseId, detail, rate);
        }

        @Test
        @DisplayName("TC_HP_02: 204 with courseId=Long.MAX_VALUE, valid data")
        void postFeedback_MaxCourseId() throws Exception {
            long courseId = Long.MAX_VALUE;
            String email = "maxid@email.com";
            String detail = "ngon";
            int rate = 3;
            willDoNothing().given(feedbackService).addFeedback(email, courseId, detail, rate);

            mockMvc.perform(post(BASE_URL, courseId)
                    .param("rate", String.valueOf(rate))
                    .param("detail", detail)
                    .with(jwtWithEmail(email)))
                .andExpect(status().isNoContent());
            verify(feedbackService).addFeedback(email, courseId, detail, rate);
        }

        @Test
        @DisplayName("TC_HP_03: 204 with detail contains UTF-8")
        void postFeedback_Utf8Detail() throws Exception {
            long courseId = 2L;
            String email = "utf8@email.com";
            String detail = "👍 tuyệt vời! 🙂";
            int rate = 4;
            willDoNothing().given(feedbackService).addFeedback(email, courseId, detail, rate);

            mockMvc.perform(post(BASE_URL, courseId)
                    .param("rate", String.valueOf(rate))
                    .param("detail", detail)
                    .with(jwtWithEmail(email)))
                .andExpect(status().isNoContent());
            verify(feedbackService).addFeedback(email, courseId, detail, rate);
        }
    }

    @Nested
    @DisplayName("DELETE /api/customer/feedback/{courseId}")
    class DeleteFeedback {

        @Test
        @DisplayName("TC_HP_04: 204 when valid, JWT has 'email', service ok")
        void deleteFeedback_HappyPath() throws Exception {
            long courseId = 9L;
            String email = "del@email.com";
            willDoNothing().given(feedbackService).deleteFeedback(email, courseId);

            mockMvc.perform(delete(BASE_URL, courseId)
                    .with(jwtWithEmail(email)))
                .andExpect(status().isNoContent());
            verify(feedbackService).deleteFeedback(email, courseId);
        }
    }

    @Nested
    @DisplayName("PUT /api/customer/feedback/{courseId}")
    class UpdateFeedback {

        @Test
        @DisplayName("TC_HP_05: 200 with JSON body on success")
        void putFeedback_HappyPath() throws Exception {
            long courseId = 20L;
            String email = "update@email.com";
            String detail = "update feedback";
            int rate = 4;
            FeedbackSimpleDto dto = FeedbackSimpleDto.builder()
                    .feedbackId(1L)
                    .content(detail)
                    .rate(rate)
                    .createDate(LocalDate.now())
                    .customer(CustomerSimpleDto.builder().customerId(12L).fullName("Tester").build())
                    .build();

            when(feedbackService.updateFeedback(email, courseId, detail, rate)).thenReturn(dto);

            // WORKAROUND: send courseId as request param, not path variable (due to controller method signature bug)
            mockMvc.perform(put(BASE_URL, courseId)
                    .param("courseId", String.valueOf(courseId))
                    .param("rate", String.valueOf(rate))
                    .param("detail", detail)
                    .with(jwtWithEmail(email)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.feedbackId", is(1)))
                .andExpect(jsonPath("$.content", is(detail)))
                .andExpect(jsonPath("$.rate", is(rate)));
            verify(feedbackService).updateFeedback(email, courseId, detail, rate);
        }
    }

    // --- EDGE/ERROR CASES ---

    @Nested
    @DisplayName("POST: Edge validation and error cases")
    class AddFeedbackEdgeCases {

        @Test
        @DisplayName("TC_EC_01: 500 INTERNAL_ERROR with MissingServletRequestParameterException when missing rate param")
        void postFeedback_MissingRate() throws Exception {
            long courseId = 10L;
            String detail = "missing rate";
            mockMvc.perform(post(BASE_URL, courseId)
                    .param("detail", detail)
                    .with(jwtWithEmail("m@email.com")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message", containsString("rate")))
                .andExpect(jsonPath("$.userMessage").exists())
                .andExpect(jsonPath("$.path").value("/api/customer/feedback/" + courseId))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
            verifyNoInteractions(feedbackService);
        }

        @Test
        @DisplayName("TC_EC_02: 400 when rate not a number")
        void postFeedback_RateNotNumber() throws Exception {
            long courseId = 7L;
            mockMvc.perform(post(BASE_URL, courseId)
                    .param("rate", "notanumber")
                    .param("detail", "any")
                    .with(jwtWithEmail("rate@email.com")))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException()).isInstanceOf(MethodArgumentTypeMismatchException.class));
            verifyNoInteractions(feedbackService);
        }

        @Test
        @DisplayName("TC_EC_03: 500 INTERNAL_ERROR with MissingServletRequestParameterException when missing detail param")
        void postFeedback_MissingDetail() throws Exception {
            long courseId = 8L;
            mockMvc.perform(post(BASE_URL, courseId)
                    .param("rate", "4")
                    .with(jwtWithEmail("miss@email.com")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message", containsString("detail")))
                .andExpect(jsonPath("$.userMessage").exists())
                .andExpect(jsonPath("$.path").value("/api/customer/feedback/" + courseId))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
            verifyNoInteractions(feedbackService);
        }

        @Test
        @DisplayName("TC_EC_04: blank detail (calls service, if no validation)")
        void postFeedback_BlankDetail() throws Exception {
            // System allows blank detail for now (no @NotBlank), so calls service
            long courseId = 11L;
            String email = "blank@email.com";
            willDoNothing().given(feedbackService).addFeedback(email, courseId, " ", 3);

            mockMvc.perform(post(BASE_URL, courseId)
                    .param("rate", "3")
                    .param("detail", " ")
                    .with(jwtWithEmail(email)))
                .andExpect(status().isNoContent());
            verify(feedbackService).addFeedback(email, courseId, " ", 3);
        }

        @Test
        @DisplayName("TC_EC_05: rate out of range (>5) - still calls service if no validation")
        void postFeedback_RateOutOfRange() throws Exception {
            long courseId = 55L;
            String email = "exceed@email.com";
            willDoNothing().given(feedbackService).addFeedback(email, courseId, "ok", 11);

            mockMvc.perform(post(BASE_URL, courseId)
                    .param("rate", "11")
                    .param("detail", "ok")
                    .with(jwtWithEmail(email)))
                .andExpect(status().isNoContent());
            verify(feedbackService).addFeedback(email, courseId, "ok", 11);
        }

        @Test
        @DisplayName("TC_EC_06: courseId not a number → 400")
        void postFeedback_InvalidCourseIdPath() throws Exception {
            mockMvc.perform(post("/api/customer/feedback/{courseId}", "nnn")
                    .param("rate", "5")
                    .param("detail", "hi")
                    .with(jwtWithEmail("na@email.com")))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(feedbackService);
        }

        @Test
        @DisplayName("TC_EC_07: JWT missing 'email' claim (calls service with null email)")
        void postFeedback_NoEmailClaim() throws Exception {
            long courseId = 999L;
            willDoNothing().given(feedbackService).addFeedback(null, courseId, "none", 2);

            mockMvc.perform(post(BASE_URL, courseId)
                    .param("rate", "2")
                    .param("detail", "none")
                    .with(jwt()))
                .andExpect(status().isNoContent());
            verify(feedbackService).addFeedback(null, courseId, "none", 2);
        }

        @Test
        @DisplayName("TC_EC_07b: JWT missing 'email' - service throws IllegalArgumentException → 500 INTERNAL_ERROR with correct error body")
        void postFeedback_NoEmail_ServiceThrows() throws Exception {
            long courseId = 51L;
            doThrow(new IllegalArgumentException("email required"))
                    .when(feedbackService).addFeedback(isNull(), eq(courseId), anyString(), anyInt());

            mockMvc.perform(post(BASE_URL, courseId)
                    .param("rate", "3")
                    .param("detail", "xx")
                    .with(jwt()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("email required"))
                .andExpect(jsonPath("$.userMessage").exists())
                .andExpect(jsonPath("$.path").value("/api/customer/feedback/" + courseId))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
            verify(feedbackService).addFeedback(null, courseId, "xx", 3);
        }
    }

    @Nested
    @DisplayName("DELETE: edge/error")
    class DeleteEdgeCases {

        @Test
        @DisplayName("TC_EC_08: courseId=0, service ok (no @Positive)")
        void deleteFeedback_ZeroId() throws Exception {
            long courseId = 0L;
            String email = "zero@email.com";
            willDoNothing().given(feedbackService).deleteFeedback(email, courseId);

            mockMvc.perform(delete(BASE_URL, courseId)
                    .with(jwtWithEmail(email)))
                .andExpect(status().isNoContent());
            verify(feedbackService).deleteFeedback(email, courseId);
        }
    }

    @Nested
    @DisplayName("PUT: edge/error")
    class PutEdgeCases {

        @Test
        @DisplayName("TC_EC_09: PUT with param bug → 500 INTERNAL_ERROR due to MissingServletRequestParameterException (controller uses @RequestParam instead of @PathVariable)")
        void putFeedback_BindingBug() throws Exception {
            // Simulate bug: controller declares @RequestParam but endpoint uses /{courseId}, expect 500 + correct error body
            mockMvc.perform(put(BASE_URL, 333L)
                    .param("rate", "2")
                    .param("detail", "update-xxx")
                    .with(jwtWithEmail("bug@email.com")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Required request parameter 'courseId'")))
                .andExpect(jsonPath("$.userMessage").exists())
                .andExpect(jsonPath("$.path").value("/api/customer/feedback/333"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
            verifyNoInteractions(feedbackService);
        }

        @Test
        @DisplayName("TC_EC_10: detail blank/empty - expect 500 INTERNAL_ERROR if controller mixes @RequestParam/@PathVariable bug")
        void putFeedback_BlankDetail() throws Exception {
            long courseId = 29L;
            String email = "blank2@email.com";

            // The bug causes MissingServletRequestParameterException, NOT service call!
            mockMvc.perform(put(BASE_URL, courseId)
                    .param("rate", "3")
                    .param("detail", "")
                    .with(jwtWithEmail(email)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Required request parameter 'courseId'")))
                .andExpect(jsonPath("$.userMessage").exists())
                .andExpect(jsonPath("$.path").value("/api/customer/feedback/" + courseId))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());

            verifyNoInteractions(feedbackService);
        }
    }

    // --- ERROR SCENARIOS (exceptions mapping/handling) ---

    @Nested
    @DisplayName("POST/PUT/DELETE: error - service throws exception")
    class ErrorScenarios {

        @Test
        @DisplayName("TC_ES_01: POST - CourseNotFoundException → 404")
        void postFeedback_CourseNotFound() throws Exception {
            long courseId = 444L;
            String email = "nf@email.com";
            doThrow(new CourseNotFoundException(courseId))
                .when(feedbackService).addFeedback(email, courseId, "abc", 4);

            mockMvc.perform(post(BASE_URL, courseId)
                    .param("rate", "4")
                    .param("detail", "abc")
                    .with(jwtWithEmail(email)))
                .andExpect(status().isNotFound());
            verify(feedbackService).addFeedback(email, courseId, "abc", 4);
        }

        @Test
        @DisplayName("TC_ES_02: POST - DataIntegrityViolationException → 500 (handled as INTERNAL_ERROR) with error body")
        void postFeedback_DataIntegrity() throws Exception {
            long courseId = 1001L;
            String email = "dberr@email.com";
            doThrow(new DataIntegrityViolationException("conflict"))
                .when(feedbackService).addFeedback(email, courseId, "x", 1);

            mockMvc.perform(post(BASE_URL, courseId)
                    .param("rate", "1")
                    .param("detail", "x")
                    .with(jwtWithEmail(email)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("conflict"))
                .andExpect(jsonPath("$.userMessage").value("Có lỗi xảy ra trên server, vui lòng thử lại sau"))
                .andExpect(jsonPath("$.path").value("/api/customer/feedback/" + courseId))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.details").doesNotExist());
            verify(feedbackService).addFeedback(email, courseId, "x", 1);
        }

        @Test
        @DisplayName("TC_ES_03: POST - domain ExceedLimitRequestException → 400 with correct error body")
        void postFeedback_DomainConflict() throws Exception {
            long courseId = 7L;
            String email = "domain@email.com";
            doThrow(new ExceedLimitRequestException("exist"))
                    .when(feedbackService).addFeedback(email, courseId, "any", 3);

            mockMvc.perform(post(BASE_URL, courseId)
                    .param("rate", "3")
                    .param("detail", "any")
                    .with(jwtWithEmail(email)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("EXCEED_LIMIT"))
                .andExpect(jsonPath("$.message").value("exist"))
                .andExpect(jsonPath("$.userMessage").value("Bạn đã vượt quá giới hạn yêu cầu"))
                .andExpect(jsonPath("$.path").value("/api/customer/feedback/" + courseId))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.details").doesNotExist()); // optional, null
            verify(feedbackService).addFeedback(email, courseId, "any", 3);
        }

        @Test
        @DisplayName("TC_ES_04: POST - service RuntimeException → 500")
        void postFeedback_RuntimeError() throws Exception {
            long courseId = 9L;
            String email = "fail@email.com";
            doThrow(new RuntimeException("DB down"))
                    .when(feedbackService).addFeedback(email, courseId, "fail", 2);

            mockMvc.perform(post(BASE_URL, courseId)
                    .param("rate", "2")
                    .param("detail", "fail")
                    .with(jwtWithEmail(email)))
                .andExpect(status().isInternalServerError());
            verify(feedbackService).addFeedback(email, courseId, "fail", 2);
        }

        @Test
        @DisplayName("TC_ES_05: POST - 403 when no JWT")
        void postFeedback_Forbidden_NoJwt() throws Exception {
            mockMvc.perform(post(BASE_URL, 10)
                    .param("rate", "3")
                    .param("detail", "should fail"))
                .andExpect(status().isForbidden())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isBlank());
            verifyNoInteractions(feedbackService);
        }


        @Test
        @DisplayName("TC_ES_06: POST - 403 when lacking authority")
        @WithMockUser
        void postFeedback_Forbidden() throws Exception {
            mockMvc.perform(post(BASE_URL, 9)
                    .param("rate", "2")
                    .param("detail", "x"))
                .andExpect(status().isForbidden())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isBlank());
            verifyNoInteractions(feedbackService);
        }

        @Test
        @DisplayName("TC_ES_07: DELETE - service throws CourseNotFoundException → 404")
        void deleteFeedback_CourseNotFound() throws Exception {
            long courseId = 99L;
            String email = "delnf@email.com";
            doThrow(new CourseNotFoundException(courseId))
                .when(feedbackService).deleteFeedback(email, courseId);

            mockMvc.perform(delete(BASE_URL, courseId)
                    .with(jwtWithEmail(email)))
                .andExpect(status().isNotFound());
            verify(feedbackService).deleteFeedback(email, courseId);
        }

        @Test
        @DisplayName("TC_ES_08: DELETE - service RuntimeException → 500")
        void deleteFeedback_RuntimeError() throws Exception {
            long courseId = 81L;
            String email = "delerr@email.com";
            doThrow(new RuntimeException("error")).when(feedbackService).deleteFeedback(email, courseId);

            mockMvc.perform(delete(BASE_URL, courseId)
                    .with(jwtWithEmail(email)))
                .andExpect(status().isInternalServerError());
            verify(feedbackService).deleteFeedback(email, courseId);
        }




        @Test
        @DisplayName("TC_ES_12: PUT - 403 when unauthorized or missing authority (only covers case with NO JWT, not when JWT present)")
        void putFeedback_Forbidden_NoJwt() throws Exception {
            // When no JWT is present, expect 403 Forbidden (not 401), as per security config
            mockMvc.perform(put(BASE_URL, 20)
                    .param("rate", "2")
                    .param("detail", "abc"))
                .andExpect(status().isForbidden());
            verifyNoInteractions(feedbackService);
        }
}
}
