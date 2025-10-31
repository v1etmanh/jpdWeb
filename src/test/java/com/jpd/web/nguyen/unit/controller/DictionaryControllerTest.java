package com.jpd.web.nguyen.unit.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpd.web.controller.DictionaryController;
import com.jpd.web.controller.common.GlobalExceptionHandler;
import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.RememberWordRepository;
import com.jpd.web.service.DictionaryService;
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
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@NestedTestConfiguration(INHERIT)
@WebMvcTest(controllers = DictionaryController.class)
@Import({ GlobalExceptionHandler.class })
@DisplayName("DictionaryControllerTest - Controller Layer")
class DictionaryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DictionaryService dictionaryService;
    @MockitoBean
    RememberWordRepository rememberWordRepository;
    @MockitoBean
    JwtDecoder jwtDecoder;
@MockitoBean
    CustomerRepository customerRepository;
    private static final String BASE_URL = "/api/customer/dictionary";

    // Helper for JWT with email claim
    static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtWithEmail(String email) {
        return jwt().jwt(jwt -> jwt.claim("email", email));
    }

    // --- HAPPY PATHS ---

    @Nested
    @DisplayName("GET /api/customer/dictionary")
    class GetDictionary {

        @Test
        @DisplayName("TC_HP_01: 200 OK - return non-empty list")
        void getDictionary_ReturnsList() throws Exception {
            var dto1 = new RememberWordDto(1L, "apple", "táo", "fruit");
            var dto2 = new RememberWordDto(2L, "cat", "mèo", "animal");
            given(dictionaryService.getDictionary("admin@email.com"))
                    .willReturn(List.of(dto1, dto2));

            mockMvc.perform(get(BASE_URL)
                            .with(jwtWithEmail("admin@email.com")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].rwId").value(1L))
                    .andExpect(jsonPath("$[1].word").value("cat"));
            verify(dictionaryService).getDictionary("admin@email.com");
        }

        @Test
        @DisplayName("TC_HP_02: 200 OK - return empty list")
        void getDictionary_ReturnsEmpty() throws Exception {
            given(dictionaryService.getDictionary("empty@mail.com"))
                    .willReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL)
                            .with(jwtWithEmail("empty@mail.com")))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
            verify(dictionaryService).getDictionary("empty@mail.com");
        }
    }

    @Nested
    @DisplayName("POST /api/customer/dictionary")
    class AddWord {

        @Test
        @DisplayName("TC_HP_03: 201 CREATED - success case")
        void postRememberWord_HappyPath() throws Exception {
            var dto = new RememberWordDto(3L, "banana", "chuối", "fruit");
            given(dictionaryService.addRememberWord(eq("user@mail.com"), org.mockito.Mockito.<RememberWordDto>any()))
                    .willReturn(dto);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"word":"banana","meaning":"chuối","description":"fruit"}
                                    """)
                            .with(jwtWithEmail("user@mail.com")))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.rwId").value(3))
                    .andExpect(jsonPath("$.word").value("banana"))
                    .andExpect(jsonPath("$.meaning").value("chuối"));
            verify(dictionaryService).addRememberWord(eq("user@mail.com"), org.mockito.Mockito.<RememberWordDto>any());
        }
    }

    @Nested
    @DisplayName("PUT /api/customer/dictionary")
    class UpdateWord {

        @Test
        @DisplayName("TC_HP_04: 200 OK - success")
        void putRememberWord_HappyPath() throws Exception {
            var dto = new RememberWordDto(2L, "do", "làm", "động từ");
            given(dictionaryService.updateRememberWord(eq("updater@mail.com"), org.mockito.Mockito.<RememberWordDto>any()))
                    .willReturn(dto);

            mockMvc.perform(put(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"rwId":2,"word":"do","meaning":"làm","description":"động từ"}
                                    """)
                            .with(jwtWithEmail("updater@mail.com")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.rwId").value(2))
                    .andExpect(jsonPath("$.word").value("do"));
            verify(dictionaryService).updateRememberWord(eq("updater@mail.com"), org.mockito.Mockito.<RememberWordDto>any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/customer/dictionary/{rwId}")
    class DeleteWord {

        @Test
        @DisplayName("TC_HP_05: 204 NO CONTENT - success")
        void deleteRememberWord_HappyPath() throws Exception {
            willDoNothing().given(dictionaryService).deleteRememberWord("user@mail.com", 2L);

            mockMvc.perform(delete(BASE_URL + "/2")
                            .with(jwtWithEmail("user@mail.com")))
                    .andExpect(status().isNoContent());
            verify(dictionaryService).deleteRememberWord("user@mail.com", 2L);
        }
    }

    // --- EDGE/ERROR CASES ---

    @Nested
    @DisplayName("POST: Edge validation and error cases")
    class AddDictionaryEdgeCases {

        @Test
        @DisplayName("TC_EC_01: POST - 201 CREATED nếu thiếu required fields (controller không validate)")
        void postRememberWord_MissingField() throws Exception {
            // The controller does NOT validate required fields, so the service IS called even if fields are empty

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"word":"","meaning":"","description":""}
                                    """)
                            .with(jwtWithEmail("someone@mail.com")))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("X-Content-Type-Options"))
                    .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"));
            // Service is called even with blank fields (see DictionaryController.addDictionary log + test output)
            verify(dictionaryService).addRememberWord(eq("someone@mail.com"), org.mockito.ArgumentMatchers.any());
        }
    }

    @Nested
    @DisplayName("PUT/DELETE: error - service throws exception")
    class ErrorScenarios {

        @Test
        @DisplayName("TC_ES_01: PUT - not found → 500 (INTERNAL_ERROR), API vẫn trả về message")
        void putRememberWord_NotFound() throws Exception {
            given(dictionaryService.updateRememberWord(eq("x@mail.com"), org.mockito.ArgumentMatchers.<RememberWordDto>any()))
                    .willThrow(new RuntimeException("Remember word not found"));
            mockMvc.perform(put(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"rwId":999,"word":"nope","meaning":"không","description":"desc"}
                                    """)
                            .with(jwtWithEmail("x@mail.com")))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Remember word not found"));
            verify(dictionaryService).updateRememberWord(eq("x@mail.com"), org.mockito.ArgumentMatchers.<RememberWordDto>any());
        }

        @Test
        @DisplayName("TC_ES_02: DELETE - service exception → 500 (ẩn lỗi)")
        void deleteRememberWord_ServiceError() throws Exception {
            willThrow(new RuntimeException("database error"))
                    .given(dictionaryService).deleteRememberWord("er@mail.com", 42L);

            mockMvc.perform(delete(BASE_URL + "/42")
                            .with(jwtWithEmail("er@mail.com")))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message", anyOf(
                        is("Internal server error"),
                        containsString("error")
                    )));
            verify(dictionaryService).deleteRememberWord("er@mail.com", 42L);
        }
    }

    @Nested
    @DisplayName("Auth/Validation: forbidden or missing JWT")
    class AuthorizationAndValidation {
        @Test
        @DisplayName("TC_ES_03: POST - 403 when no JWT")
        void postRememberWord_Forbidden_NoJwt() throws Exception {
            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"word":"some","meaning":"gì đó","description":"test"}
                            """))
                .andExpect(status().isForbidden());
            verifyNoInteractions(dictionaryService);
        }

        @Test
        @DisplayName("TC_ES_04: DELETE - 403 when no JWT")
        void deleteRememberWord_Forbidden_NoJwt() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/2"))
                .andExpect(status().isForbidden());
            verifyNoInteractions(dictionaryService);
        }
    }
}
