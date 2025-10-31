package com.jpd.web.nguyen.unit.controller;

import com.jpd.web.controller.DictionaryController;
import com.jpd.web.controller.common.GlobalExceptionHandler;
import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.exception.CustomerNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.service.DictionaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Test cho DictionaryController
 *
 * <p><b>Phạm vi test:</b> Controller layer cho personal dictionary management</p>
 *
 * <p><b>Endpoints được test:</b></p>
 * <ul>
 *   <li>GET /api/customer/dictionary - Lấy danh sách từ vựng đã lưu</li>
 *   <li>POST /api/customer/dictionary - Thêm từ mới vào dictionary</li>
 *   <li>PUT /api/customer/dictionary - Cập nhật từ đã có</li>
 *   <li>DELETE /api/customer/dictionary/{rwId} - Xóa từ khỏi dictionary</li>
 * </ul>
 *
 * <p><b>Kịch bản test bao gồm:</b></p>
 * <ul>
 *   <li><b>HAPPY PATHS:</b> Success cases cho CRUD operations</li>
 *   <li><b>EDGE CASES:</b> Empty dictionary, validation</li>
 *   <li><b>ERROR SCENARIOS:</b> Unauthorized access, not found, validation errors</li>
 * </ul>
 *
 * @author QA Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DictionaryController Test Suite")
class DictionaryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DictionaryService dictionaryService;

    @InjectMocks
    private DictionaryController dictionaryController;

    private static final String TEST_EMAIL = "student@example.com";
    private static final long TEST_WORD_ID = 1L;
    private static final String TEST_WORD = "こんにちは";
    private static final String TEST_MEANING = "Hello";
    private static final String TEST_DESCRIPTION = "A common Japanese greeting";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dictionaryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
//
//    // ============================================================
//    // ENDPOINT 1: GET /api/customer/dictionary
//    // ============================================================
//
//    /**
//     * TC_HP_01: shouldReturn200AndWordList_whenUserHasSavedWords
//     *
//     * <p><b>Given:</b> dictionaryService.getDictionary() trả về List<RememberWordDto>
//     * chứa các từ vựng user đã lưu</p>
//     *
//     * <p><b>When:</b> gọi GET /api/customer/dictionary với JWT hợp lệ</p>
//     *
//     * <p><b>Then:</b></p>
//     * <ul>
//     *   <li>HTTP status = 200 OK</li>
//     *   <li>Body = JSON array chứa các RememberWordDto</li>
//     *   <li>Mỗi word có đầy đủ: rwId, word, meaning, description</li>
//     * </ul>
//     */
//    @Test
//    @DisplayName("TC_HP_01: Trả về 200 và danh sách từ vựng khi user có từ đã lưu")
//    void shouldReturn200AndWordList_whenUserHasSavedWords() throws Exception {
//        // Given
//        List<RememberWordDto> wordList = new ArrayList<>();
//
//        RememberWordDto word1 = RememberWordDto.builder()
//                .rwId(1L)
//                .word("こんにちは")
//                .meaning("Hello")
//                .description("A common Japanese greeting")
//                .build();
//
//        RememberWordDto word2 = RememberWordDto.builder()
//                .rwId(2L)
//                .word("ありがとう")
//                .meaning("Thank you")
//                .description("Expression of gratitude")
//                .build();
//
//        wordList.add(word1);
//        wordList.add(word2);
//
//        when(dictionaryService.getDictionary(TEST_EMAIL)).thenReturn(wordList);
//
//        // When / Then
//        mockMvc.perform(get("/api/customer/dictionary")
//                        .with(jwt().jwt(jwt -> jwt.claim("email", TEST_EMAIL))))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$", hasSize(2)))
//                .andExpect(jsonPath("$[0].rwId").value(1L))
//                .andExpect(jsonPath("$[0].word").value("こんにちは"))
//                .andExpect(jsonPath("$[0].meaning").value("Hello"))
//                .andExpect(jsonPath("$[0].description").value("A common Japanese greeting"))
//                .andExpect(jsonPath("$[1].rwId").value(2L))
//                .andExpect(jsonPath("$[1].word").value("ありがとう"))
//                .andExpect(jsonPath("$[1].meaning").value("Thank you"));
//
//        verify(dictionaryService, times(1)).getDictionary(TEST_EMAIL);
//    }
//
//    /**
//     * TC_EC_01: shouldReturn200AndEmptyList_whenUserHasNoSavedWords
//     *
//     * <p><b>Given:</b> Service trả về empty list (user chưa lưu từ nào)</p>
//     *
//     * <p><b>When:</b> gọi GET /api/customer/dictionary</p>
//     *
//     * <p><b>Then:</b></p>
//     * <ul>
//     *   <li>HTTP status = 200 OK</li>
//     *   <li>Body = [] (empty array)</li>
//     * </ul>
//     */
//    @Test
//    @DisplayName("TC_EC_01: Trả về 200 và empty list khi user chưa có từ nào")
//    void shouldReturn200AndEmptyList_whenUserHasNoSavedWords() throws Exception {
//        // Given
//        when(dictionaryService.getDictionary(TEST_EMAIL)).thenReturn(Collections.emptyList());
//
//        // When / Then
//        mockMvc.perform(get("/api/customer/dictionary")
//                        .with(jwt().jwt(jwt -> jwt.claim("email", TEST_EMAIL))))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$", hasSize(0)))
//                .andExpect(jsonPath("$", empty()));
//    }
//
//    /**
//     * TC_ES_01: shouldReturn404_whenCustomerNotFound
//     *
//     * <p><b>Given:</b> Service ném CustomerNotFoundException (customer không tồn tại)</p>
//     *
//     * <p><b>When:</b> gọi GET /api/customer/dictionary</p>
//     *
//     * <p><b>Then:</b></p>
//     * <ul>
//     *   <li>HTTP status = 404 Not Found</li>
//     *   <li>ErrorResponse với message phù hợp</li>
//     * </ul>
//     */
//    @Test
//    @DisplayName("TC_ES_01: Trả về 404 khi customer không tồn tại")
//    void shouldReturn404_whenCustomerNotFound() throws Exception {
//        // Given
//        when(dictionaryService.getDictionary(TEST_EMAIL))
//                .thenThrow(new CustomerNotFoundException("Customer not found"));
//
//        // When / Then
//        mockMvc.perform(get("/api/customer/dictionary")
//                        .with(jwt().jwt(jwt -> jwt.claim("email", TEST_EMAIL))))
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.userMessage").value("Tài nguyên không được tìm thấy"));
//    }
//
//    // ============================================================
//    // ENDPOINT 2: POST /api/customer/dictionary
//    // ============================================================
//
//    /**
//     * TC_HP_02: shouldReturn201AndCreatedWord_whenValidWordData
//     *
//     * <p><b>Given:</b> Request body chứa RememberWordDto hợp lệ (word, meaning, description)</p>
//     *
//     * <p><b>When:</b> gọi POST /api/customer/dictionary</p>
//     *
//     * <p><b>Then:</b></p>
//     * <ul>
//     *   <li>HTTP status = 201 CREATED</li>
//     *   <li>Body = RememberWordDto đã được tạo</li>
//     *   <li>Service.addRememberWord được gọi</li>
//     * </ul>
//     */
//    @Test
//    @DisplayName("TC_HP_02: Trả về 201 và word đã tạo khi data hợp lệ")
//    void shouldReturn201AndCreatedWord_whenValidWordData() throws Exception {
//        // Given
//        RememberWordDto createdDto = RememberWordDto.builder()
//                .rwId(TEST_WORD_ID)
//                .word(TEST_WORD)
//                .meaning(TEST_MEANING)
//                .description(TEST_DESCRIPTION)
//                .build();
//
//        when(dictionaryService.addRememberWord(eq(TEST_EMAIL), org.mockito.ArgumentMatchers.any(RememberWordDto.class)))
//                .thenReturn(createdDto);
//
//        // When / Then
//        mockMvc.perform(post("/api/customer/dictionary")
//                        .with(jwt().jwt(jwt -> jwt.claim("email", TEST_EMAIL)))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"word\":\"" + TEST_WORD + "\",\"meaning\":\"" + TEST_MEANING + "\",\"description\":\"" + TEST_DESCRIPTION + "\"}"))
//                .andExpect(status().isCreated())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.rwId").value(TEST_WORD_ID))
//                .andExpect(jsonPath("$.word").value(TEST_WORD))
//                .andExpect(jsonPath("$.meaning").value(TEST_MEANING))
//                .andExpect(jsonPath("$.description").value(TEST_DESCRIPTION));
//
//        verify(dictionaryService, times(1))
//                .addRememberWord(eq(TEST_EMAIL), org.mockito.ArgumentMatchers.any(RememberWordDto.class));
//    }
//
//    /**
//     * TC_ES_02: shouldReturn400_whenMissingRequiredFields
//     *
//     * <p><b>Given:</b> Request body thiếu field bắt buộc (@NotBlank word, meaning, description)</p>
//     *
//     * <p><b>When:</b> gọi POST /api/customer/dictionary</p>
//     *
//     * <p><b>Then:</b></p>
//     * <ul>
//     *   <li>HTTP status = 400 Bad Request</li>
//     *   <li>Validation error message</li>
//     * </ul>
//     */
//    @Test
//    @DisplayName("TC_ES_02: Trả về 400 khi thiếu field bắt buộc")
//    void shouldReturn400_whenMissingRequiredFields() throws Exception {
//        // When / Then - Missing word field
//        mockMvc.perform(post("/api/customer/dictionary")
//                        .with(jwt().jwt(jwt -> jwt.claim("email", TEST_EMAIL)))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"meaning\":\"Hello\",\"description\":\"A greeting\"}"))
//                .andExpect(status().isBadRequest());
//    }
//
//    /**
//     * TC_ES_03: shouldReturn400_whenBlankFields
//     *
//     * <p><b>Given:</b> Request body có fields blank/empty (vi phạm @NotBlank)</p>
//     *
//     * <p><b>When:</b> gọi POST /api/customer/dictionary</p>
//     *
//     * <p><b>Then:</b></p>
//     * <ul>
//     *   <li>HTTP status = 400 Bad Request</li>
//     * </ul>
//     */
//    @Test
//    @DisplayName("TC_ES_03: Trả về 400 khi fields là blank/empty")
//    void shouldReturn400_whenBlankFields() throws Exception {
//        // When / Then - Blank word
//        mockMvc.perform(post("/api/customer/dictionary")
//                        .with(jwt().jwt(jwt -> jwt.claim("email", TEST_EMAIL)))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"word\":\"\",\"meaning\":\"Hello\",\"description\":\"A greeting\"}"))
//                .andExpect(status().isBadRequest());
//    }
//
//    /**
//     * TC_ES_04: shouldReturn404_whenCustomerNotFoundOnAdd
//     *
//     * <p><b>Given:</b> Service ném CustomerNotFoundException khi add word</p>
//     *
//     * <p><b>When:</b> gọi POST /api/customer/dictionary</p>
//     *
//     * <p><b>Then:</b></p>
//     * <ul>
//     *   <li>HTTP status = 404 Not Found</li>
//     * </ul>
//     */
//    @Test
//    @DisplayName("TC_ES_04: Trả về 404 khi customer không tồn tại khi add word")
//    void shouldReturn404_whenCustomerNotFoundOnAdd() throws Exception {
//        // Given
//        when(dictionaryService.addRememberWord(eq(TEST_EMAIL), org.mockito.ArgumentMatchers.any(RememberWordDto.class)))
//                .thenThrow(new CustomerNotFoundException("Customer not found"));
//
//        // When / Then
//        mockMvc.perform(post("/api/customer/dictionary")
//                        .with(jwt().jwt(jwt -> jwt.claim("email", TEST_EMAIL)))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"word\":\"" + TEST_WORD + "\",\"meaning\":\"" + TEST_MEANING + "\",\"description\":\"" + TEST_DESCRIPTION + "\"}"))
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.userMessage").value("Tài nguyên không được tìm thấy"));
//    }
//
//    // ============================================================
//    // ENDPOINT 3: PUT /api/customer/dictionary
//    // ============================================================
//
//    /**
//     * TC_HP_03: shouldReturn200AndUpdatedWord_whenValidUpdateData
//     *
//     * <p><b>Given:</b> Request body chứa RememberWordDto với rwId và data cập nhật</p>
//     *
//     * <p><b>When:</b> gọi PUT /api/customer/dictionary</p>
//     *
//     * <p><b>Then:</b></p>
//     * <ul>
//     *   <li>HTTP status = 200 OK</li>
//     *   <li>Body = RememberWordDto đã được cập nhật</li>
//     *   <li>Service.updateRememberWord được gọi</li>
//     * </ul>
//     */
//    @Test
//    @DisplayName("TC_HP_03: Trả về 200 và word đã update khi data hợp lệ")
//    void shouldReturn200AndUpdatedWord_whenValidUpdateData() throws Exception {
//        // Given
//        RememberWordDto updateDto = RememberWordDto.builder()
//                .rwId(TEST_WORD_ID)
//                .word(TEST_WORD)
//                .meaning("Hi (informal)")
//                .description("Updated description")
//                .build();
//
//        when(dictionaryService.updateRememberWord(eq(TEST_EMAIL), org.mockito.ArgumentMatchers.any(RememberWordDto.class)))
//                .thenReturn(updateDto);
//
//        // When / Then
//        mockMvc.perform(put("/api/customer/dictionary")
//                        .with(jwt().jwt(jwt -> jwt.claim("email", TEST_EMAIL)))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"rwId\":" + TEST_WORD_ID + ",\"word\":\"" + TEST_WORD + "\",\"meaning\":\"Hi (informal)\",\"description\":\"Updated description\"}"))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.rwId").value(TEST_WORD_ID))
//                .andExpect(jsonPath("$.word").value(TEST_WORD))
//                .andExpect(jsonPath("$.meaning").value("Hi (informal)"))
//                .andExpect(jsonPath("$.description").value("Updated description"));
//
//        verify(dictionaryService, times(1))
//                .updateRememberWord(eq(TEST_EMAIL), org.mockito.ArgumentMatchers.any(RememberWordDto.class));
//    }
//
//    /**
//     * TC_ES_05: shouldReturn401_whenUnauthorizedToUpdateWord
//     *
//     * <p><b>Given:</b> Service ném UnauthorizedException (user không sở hữu từ này)</p>
//     *
//     * <p><b>When:</b> gọi PUT /api/customer/dictionary</p>
//     *
//     * <p><b>Then:</b></p>
//     * <ul>
//     *   <li>HTTP status = 401 Unauthorized</li>
//     *   <li>ErrorResponse: "you do not own this word"</li>
//     * </ul>
//     */
//    @Test
//    @DisplayName("TC_ES_05: Trả về 401 khi user không có quyền update từ này")
//    void shouldReturn401_whenUnauthorizedToUpdateWord() throws Exception {
//        // Given
//        when(dictionaryService.updateRememberWord(eq(TEST_EMAIL), org.mockito.ArgumentMatchers.any(RememberWordDto.class)))
//                .thenThrow(new UnauthorizedException("you do not own this word"));
//
//        // When / Then
//        mockMvc.perform(put("/api/customer/dictionary")
//                        .with(jwt().jwt(jwt -> jwt.claim("email", TEST_EMAIL)))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"rwId\":" + TEST_WORD_ID + ",\"word\":\"test\",\"meaning\":\"test\"," +
//                                "\"description\":\"test\"}"))
//                .andExpect(status().isUnauthorized())
//                .andExpect(jsonPath("$.userMessage").value("Bạn không có quyền truy cập"));
//    }
//
//    /**
//     * TC_ES_06: shouldReturn500_whenWordNotFoundOnUpdate
//     *
//     * <p><b>Given:</b> Service ném RuntimeException("Remember word not found")</p>
//     *
//     * <p><b>When:</b> gọi PUT /api/customer/dictionary</p>
//     *
//     * <p><b>Then:</b></p>
//     * <ul>
//     *   <li>HTTP status = 500 Internal Server Error</li>
//     * </ul>
//     */
//    @Test
//    @DisplayName("TC_ES_06: Trả về 500 khi word không tồn tại khi update")
//    void shouldReturn500_whenWordNotFoundOnUpdate() throws Exception {
//        // Given
//        when(dictionaryService.updateRememberWord(eq(TEST_EMAIL), org.mockito.ArgumentMatchers.any(RememberWordDto.class)))
//                .thenThrow(new RuntimeException("Remember word not found"));
//
//        // When / Then
//        mockMvc.perform(put("/api/customer/dictionary")
//                        .with(jwt().jwt(jwt -> jwt.claim("email", TEST_EMAIL)))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"rwId\":999,\"word\":\"test\",\"meaning\":\"test\",\"description\":\"test\"}"))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
//                .andExpect(jsonPath("$.message").value("Remember word not found"));
//    }
//
//    // ============================================================
//    // ENDPOINT 4: DELETE /api/customer/dictionary/{rwId}
//    // ============================================================
//
//    /**
//     * TC_HP_04: shouldReturn204_whenSuccessfullyDeleteWord
//     *
//     * <p><b>Given:</b> Service xử lý delete thành công (void method không ném exception)</p>
//     *
//     * <p><b>When:</b> gọi DELETE /api/customer/dictionary/{rwId}</p>
//     *
//     * <p><b>Then:</b></p>
//     * <ul>
//     *   <li>HTTP status = 204 No Content</li>
//     *   <li>Không có response body</li>
//     *   <li>Service.deleteRememberWord được gọi</li>
//     * </ul>
//     */
//    @Test
//    @DisplayName("TC_HP_04: Trả về 204 khi xóa từ thành công")
//    void shouldReturn204_whenSuccessfullyDeleteWord() throws Exception {
//        // Given
//        doNothing().when(dictionaryService).deleteRememberWord(TEST_EMAIL, TEST_WORD_ID);
//
//        // When / Then
//        mockMvc.perform(delete("/api/customer/dictionary/{rwId}", TEST_WORD_ID)
//                        .with(jwt().jwt(jwt -> jwt.claim("email", TEST_EMAIL))))
//                .andExpect(status().isNoContent())
//                .andExpect(content().string(""));
//
//        verify(dictionaryService, times(1)).deleteRememberWord(TEST_EMAIL, TEST_WORD_ID);
//    }
//
//    /**
//     * TC_ES_07: shouldReturn401_whenUnauthorizedToDeleteWord
//     *
//     * <p><b>Given:</b> Service ném UnauthorizedException (user không sở hữu từ này)</p>
//     *
//     * <p><b>When:</b> gọi DELETE /api/customer/dictionary/{rwId}</p>
//     *
//     * <p><b>Then:</b></p>
//     * <ul>
//     *   <li>HTTP status = 401 Unauthorized</li>
//     *   <li>ErrorResponse: "you do not own this word"</li>
//     * </ul>
//     */
//    @Test
//    @DisplayName("TC_ES_07: Trả về 401 khi user không có quyền xóa từ này")
//    void shouldReturn401_whenUnauthorizedToDeleteWord() throws Exception {
//        // Given
//        doThrow(new UnauthorizedException("you do not own this word"))
//                .when(dictionaryService).deleteRememberWord(TEST_EMAIL, TEST_WORD_ID);
//
//        // When / Then
//        mockMvc.perform(delete("/api/customer/dictionary/{rwId}", TEST_WORD_ID)
//                        .with(jwt().jwt(jwt -> jwt.claim("email", TEST_EMAIL))))
//                .andExpect(status().isUnauthorized())
//                .andExpect(jsonPath("$.userMessage").value("Bạn không có quyền truy cập"));
//    }
//
//    /**
//     * TC_ES_08: shouldReturn500_whenWordNotFoundOnDelete
//     *
//     * <p><b>Given:</b> Service ném RuntimeException("this id is not exist")</p>
//     *
//     * <p><b>When:</b> gọi DELETE /api/customer/dictionary/{rwId}</p>
//     *
//     * <p><b>Then:</b></p>
//     * <ul>
//     *   <li>HTTP status = 500 Internal Server Error</li>
//     * </ul>
//     */
//    @Test
//    @DisplayName("TC_ES_08: Trả về 500 khi word không tồn tại khi delete")
//    void shouldReturn500_whenWordNotFoundOnDelete() throws Exception {
//        // Given
//        doThrow(new RuntimeException("this id is not exist"))
//                .when(dictionaryService).deleteRememberWord(TEST_EMAIL, 999L);
//
//        // When / Then
//        mockMvc.perform(delete("/api/customer/dictionary/{rwId}", 999L)
//                        .with(jwt().jwt(jwt -> jwt.claim("email", TEST_EMAIL))))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
//                .andExpect(jsonPath("$.message").value("this id is not exist"));
//    }
//
//    /**
//     * TC_ES_09: shouldReturn404_whenCustomerNotFoundOnDelete
//     *
//     * <p><b>Given:</b> Service ném CustomerNotFoundException</p>
//     *
//     * <p><b>When:</b> gọi DELETE /api/customer/dictionary/{rwId}</p>
//     *
//     * <p><b>Then:</b></p>
//     * <ul>
//     *   <li>HTTP status = 404 Not Found</li>
//     * </ul>
//     */
//    @Test
//    @DisplayName("TC_ES_09: Trả về 404 khi customer không tồn tại khi delete")
//    void shouldReturn404_whenCustomerNotFoundOnDelete() throws Exception {
//        // Given
//        doThrow(new CustomerNotFoundException("Customer not found"))
//                .when(dictionaryService).deleteRememberWord(TEST_EMAIL, TEST_WORD_ID);
//
//        // When / Then
//        mockMvc.perform(delete("/api/customer/dictionary/{rwId}", TEST_WORD_ID)
//                        .with(jwt().jwt(jwt -> jwt.claim("email", TEST_EMAIL))))
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.userMessage").value("Tài nguyên không được tìm thấy"));
//    }
//
//    /**
//     * TC_ES_10: shouldReturn500_whenServiceThrowsRuntimeException
//     *
//     * <p><b>Given:</b> Service ném generic RuntimeException</p>
//     *
//     * <p><b>When:</b> gọi bất kỳ endpoint nào</p>
//     *
//     * <p><b>Then:</b></p>
//     * <ul>
//     *   <li>HTTP status = 500 Internal Server Error</li>
//     *   <li>ErrorResponse với code "INTERNAL_ERROR"</li>
//     * </ul>
//     */
//    @Test
//    @DisplayName("TC_ES_10: Trả về 500 khi service ném RuntimeException")
//    void shouldReturn500_whenServiceThrowsRuntimeException() throws Exception {
//        // Given
//        when(dictionaryService.getDictionary(TEST_EMAIL))
//                .thenThrow(new RuntimeException("Unexpected database error"));
//
//        // When / Then
//        mockMvc.perform(get("/api/customer/dictionary")
//                        .with(jwt().jwt(jwt -> jwt.claim("email", TEST_EMAIL))))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
//                .andExpect(jsonPath("$.message").value("Unexpected database error"))
//                .andExpect(jsonPath("$.userMessage").value("Có lỗi xảy ra trên server, vui lòng thử lại sau"));
//    }
}
