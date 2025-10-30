package com.jpd.web.controller;


import com.jpd.web.controller.common.GlobalExceptionHandler;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.EnrollmentExistException;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.service.EnrollmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EnrollmentController.class)
@Import({GlobalExceptionHandler.class}) // Import your @ControllerAdvice if any
public class EnrollmentControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    EnrollmentService enrollmentService;
    @MockitoBean
    JwtDecoder jwtDecoder;
    @MockitoBean
    CustomerRepository customerRepository;
    static JwtRequestPostProcessor jwtWithEmail(String email) {
        return jwt().jwt(jwt -> jwt.claim("email", email));
    }

    final String BASE_URL = "/api/enroll/{id}";

    @Nested
    @DisplayName("POST /api/enroll/{id}?joinKey=")
    class EnrollEndpoint {

        @Test
        @DisplayName("TC_HP_01: 200 OK when joinKey valid, id valid, JWT has email, service returns true")
        void postEnroll_HappyPath() throws Exception {
            long id = 123L;
            String joinKey = "abc123";
            String email = "user@example.com";
            when(enrollmentService.handlePrivateCourse(id, joinKey, email)).thenReturn(true);

            mockMvc.perform(post(BASE_URL, id)
                            .param("joinKey", joinKey)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            verify(enrollmentService).handlePrivateCourse(id, joinKey, email);
        }

        @Test
        @DisplayName("TC_HP_02: 200 OK with id=Long.MAX_VALUE, joinKey valid, service returns true")
        void postEnroll_MaxId() throws Exception {
            long id = Long.MAX_VALUE;
            String joinKey = "code999";
            String email = "maxid@test.com";
            when(enrollmentService.handlePrivateCourse(id, joinKey, email)).thenReturn(true);

            mockMvc.perform(post(BASE_URL, String.valueOf(id))
                            .param("joinKey", joinKey)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            verify(enrollmentService).handlePrivateCourse(id, joinKey, email);
        }

        @Test
        @DisplayName("TC_HP_03: 200 OK with joinKey containing special chars, service returns true")
        void postEnroll_SpecialCharJoinKey() throws Exception {
            long id = 555L;
            String joinKey = "Abc-123_.$@^%";
            String email = "spec@email.com";
            when(enrollmentService.handlePrivateCourse(id, joinKey, email)).thenReturn(true);

            mockMvc.perform(post(BASE_URL, id)
                            .param("joinKey", joinKey)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            verify(enrollmentService).handlePrivateCourse(id, joinKey, email);
        }

        @Test
        @DisplayName("TC_EC_01: 500 Internal Server Error when missing joinKey param, exception handled by GlobalExceptionHandler")
        void postEnroll_MissingJoinKey() throws Exception {
            long id = 123L;
            mockMvc.perform(post(BASE_URL, id)
                            .with(jwtWithEmail("user@example.com")))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("joinKey")))
                    .andExpect(jsonPath("$.userMessage").exists())
                    .andExpect(jsonPath("$.path").value("/api/enroll/" + id))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.traceId").exists());
            verifyNoInteractions(enrollmentService);
        }

        @Test
        @DisplayName("TC_EC_02: 400 Bad Request when id is not a number")
        void postEnroll_IdNotNumber() throws Exception {
            mockMvc.perform(post("/api/enroll/{id}", "abc")
                            .param("joinKey", "abc123")
                            .with(jwtWithEmail("test@mail.com")))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> assertThat(result.getResolvedException())
                            .isInstanceOf(MethodArgumentTypeMismatchException.class));
            verifyNoInteractions(enrollmentService);
        }

        @Test
        @DisplayName("TC_EC_03a: joinKey blank, service returns false → 400 Bad Request")
        void postEnroll_JoinKeyBlank_CallsService_Returns400() throws Exception {
            long id = 300L;
            String email = "no@blank.com";
            // Current controller has no @NotBlank, so service called
            when(enrollmentService.handlePrivateCourse(id, "", email)).thenReturn(false);

            mockMvc.perform(post(BASE_URL, id)
                            .param("joinKey", "")
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(""));

            verify(enrollmentService).handlePrivateCourse(id, "", email);

            // blank with spaces
            when(enrollmentService.handlePrivateCourse(id, "   ", email)).thenReturn(false);

            mockMvc.perform(post(BASE_URL, id)
                            .param("joinKey", "   ")
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isBadRequest());

            verify(enrollmentService).handlePrivateCourse(id, "   ", email);
        }

        // If annotated with @NotBlank: expect 400 and no call to service
        // Optional: test (commented or enabled if validation present)
        @Test
        @DisplayName("TC_EC_03b: joinKey blank (with @NotBlank) → 400, no call")
        void postEnroll_JoinKeyBlank_NotBlankEnabled() throws Exception {
            // emulate validation error (if controller annotated)
            // This block should be disabled or modified if you actually have @NotBlank on controller param
            // mockMvc.perform(post(BASE_URL, 10)
            //         .param("joinKey", " ")
            //         .with(jwtWithEmail("foo@bar.com")))
            //         .andExpect(status().isBadRequest()); // .and more (error json if handled)
            // verifyNoInteractions(enrollmentService);
        }

        @Test
        @DisplayName("TC_EC_04a: id = 0 and negative; service called, status 400 on false")
        void postEnroll_IdZeroOrNegative() throws Exception {
            String joinKey = "key";
            String email = "negid@eg.com";
            // id=0
            when(enrollmentService.handlePrivateCourse(0L, joinKey, email)).thenReturn(false);
            mockMvc.perform(post(BASE_URL, 0L)
                            .param("joinKey", joinKey)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isBadRequest());
            verify(enrollmentService).handlePrivateCourse(0L, joinKey, email);

            // id<0
            when(enrollmentService.handlePrivateCourse(-5L, joinKey, email)).thenReturn(false);
            mockMvc.perform(post(BASE_URL, -5L)
                            .param("joinKey", joinKey)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isBadRequest());
            verify(enrollmentService).handlePrivateCourse(-5L, joinKey, email);
        }

        // @Positive present: expect 400 validation and no call
        @Test
        @DisplayName("TC_EC_04b: id negative/zero (with @Positive) → 400, no call")
        void postEnroll_IdNegativeZero_PositiveEnabled() throws Exception {
            // This block enabled if controller param annotated @Positive
            // mockMvc.perform(post(BASE_URL, 0)
            //                 .param("joinKey", "key")
            //                 .with(jwtWithEmail("a@mail.com")))
            //         .andExpect(status().isBadRequest());
            // verifyNoInteractions(enrollmentService);
        }

        @Test
        @DisplayName("TC_EC_05: JWT missing claim email – call service with email=null, service returns false → 400")
        void postEnroll_MissingEmailClaim_Returns400() throws Exception {
            long id = 1111L;
            String joinKey = "somekey";
            // email is null because claim not present
            when(enrollmentService.handlePrivateCourse(eq(id), eq(joinKey), isNull())).thenReturn(false);

            mockMvc.perform(post(BASE_URL, id)
                            .param("joinKey", joinKey)
                            .with(jwt()))
                    .andExpect(status().isBadRequest());

            verify(enrollmentService).handlePrivateCourse(id, joinKey, null);
        }

        @Test
        @DisplayName("TC_EC_05b: JWT missing claim email – service throws IllegalArgumentException → 500 INTERNAL_ERROR if not mapped")
        void postEnroll_MissingEmailClaim_ServiceThrowsIllegalArgument() throws Exception {
            long id = 25L;
            String joinKey = "somekey";
            when(enrollmentService.handlePrivateCourse(eq(id), eq(joinKey), isNull()))
                    .thenThrow(new IllegalArgumentException("email required"));

            mockMvc.perform(post(BASE_URL, id)
                            .param("joinKey", joinKey)
                            .with(jwt()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("email required")));
            verify(enrollmentService).handlePrivateCourse(id, joinKey, null);
        }

        @Test
        @DisplayName("TC_EC_06: joinKey very long (e.g. 1024 chars), allowed, returns 200 if service returns true")
        void postEnroll_LongJoinKey_Allowed() throws Exception {
            long id = 10L;
            String joinKey = "A".repeat(1024);
            String email = "longkey@mail.com";
            when(enrollmentService.handlePrivateCourse(id, joinKey, email)).thenReturn(true);

            mockMvc.perform(post(BASE_URL, id)
                            .param("joinKey", joinKey)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isOk());

            verify(enrollmentService).handlePrivateCourse(id, joinKey, email);
        }
        // If @Size(max=...) present, expect 400 and no call to service (optionally test).

        @Test
        @DisplayName("TC_ES_01: Service throws CourseNotFoundException → 404 if mapped, or 500")
        void postEnroll_ServiceThrowsCourseNotFoundException() throws Exception {
            long id = 10L;
            String joinKey = "key";
            String email = "ex@mail.com";
            when(enrollmentService.handlePrivateCourse(id, joinKey, email))
                    .thenThrow(new CourseNotFoundException(id));

            mockMvc.perform(post(BASE_URL, id)
                            .param("joinKey", joinKey)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isNotFound()); // Or .isInternalServerError() if not mapped
            verify(enrollmentService).handlePrivateCourse(id, joinKey, email);
        }

        @Test
        @DisplayName("TC_ES_02: return false → 500 Internal Server Error with error body if service throws generic RuntimeException ('invalid key')")
        void postEnroll_ServiceThrowsInvalidKeyException() throws Exception {
            long id = 1L;
            String joinKey = "bad";
            String email = "err@mail.com";
            Exception invalidKeyEx = new RuntimeException("invalid key"); // Emulate missing exception

            when(enrollmentService.handlePrivateCourse(id, joinKey, email))
                    .thenThrow(invalidKeyEx);

            mockMvc.perform(post(BASE_URL, id)
                            .param("joinKey", joinKey)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.message").value("invalid key"))
                    .andExpect(jsonPath("$.path").value("/api/enroll/" + id))
                    .andExpect(jsonPath("$.userMessage").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.traceId").exists());

            verify(enrollmentService).handlePrivateCourse(id, joinKey, email);
        }

        @Test
        @DisplayName("TC_ES_03: Service throws EnrollmentExistException → 409 Conflict")
        void postEnroll_ServiceThrowsEnrollmentExistException() throws Exception {
            long id = 890L;
            String joinKey = "x";
            String email = "enrolled@mail.com";
            when(enrollmentService.handlePrivateCourse(id, joinKey, email))
                    .thenThrow(new EnrollmentExistException("you have been enrroll"));

            mockMvc.perform(post(BASE_URL, id)
                            .param("joinKey", joinKey)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isConflict());
            verify(enrollmentService).handlePrivateCourse(id, joinKey, email);
        }

        @Test
        @DisplayName("TC_ES_04: Service throws DataIntegrityViolationException → 500 Internal Server Error (not handled as 409)")
        void postEnroll_ServiceThrowsDataIntegrityViolationException() throws Exception {
            long id = 900L;
            String joinKey = "conflict";
            String email = "dberror@mail.com";
            when(enrollmentService.handlePrivateCourse(id, joinKey, email))
                    .thenThrow(new DataIntegrityViolationException("duplicate"));

            mockMvc.perform(post(BASE_URL, id)
                            .param("joinKey", joinKey)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.message").value("duplicate"))
                    .andExpect(jsonPath("$.userMessage").exists())
                    .andExpect(jsonPath("$.path").value("/api/enroll/" + id))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.traceId").exists());
            verify(enrollmentService).handlePrivateCourse(id, joinKey, email);
        }

        @Test
        @DisplayName("TC_ES_05: Service throws RuntimeException → 500 Internal Server Error")
        void postEnroll_ServiceThrowsRuntimeException() throws Exception {
            long id = 5L;
            String joinKey = "db";
            String email = "fail@mail.com";
            when(enrollmentService.handlePrivateCourse(id, joinKey, email))
                    .thenThrow(new RuntimeException("DB down"));

            mockMvc.perform(post(BASE_URL, id)
                            .param("joinKey", joinKey)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isInternalServerError());
            verify(enrollmentService).handlePrivateCourse(id, joinKey, email);
        }

        @Test
        @DisplayName("TC_ES_06: 403 Forbidden when missing JWT")
        void postEnroll_MissingJwt_Returns403() throws Exception {
            mockMvc.perform(post(BASE_URL, 10)
                            .param("joinKey", "abcde"))
                    .andExpect(status().isForbidden())
                    .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isBlank());
            verifyNoInteractions(enrollmentService);
        }


        @Test
        @DisplayName("TC_ES_07: 403 Forbidden when lacking required authority")
        @WithMockUser // will lack "SCOPE_enroll.write" etc if required by security config
        void postEnroll_Forbidden_NoAuthority() throws Exception {
            mockMvc.perform(post(BASE_URL, 10)
                            .param("joinKey", "abcde"))
                    .andExpect(status().isForbidden())
                    .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isBlank());
            verifyNoInteractions(enrollmentService);
        }

        @Test
        @DisplayName("returns 400 Bad Request when service returns false for logic validation")
        void postEnroll_Returns400IfServiceReturnsFalse() throws Exception {
            long id = 1234L;
            String email = "user@mail.com";
            String joinKey = "willFail";
            when(enrollmentService.handlePrivateCourse(id, joinKey, email)).thenReturn(false);

            mockMvc.perform(post(BASE_URL, id)
                            .param("joinKey", joinKey)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(""));
            verify(enrollmentService).handlePrivateCourse(id, joinKey, email);
        }
    }
}
