package com.jpd.web.nguyen.unit.controller;

import com.jpd.web.controller.common.CourseInfController;
import com.jpd.web.controller.common.GlobalExceptionHandler;
import com.jpd.web.service.CourseInfService;
import com.jpd.web.dto.CourseInfDto;
import com.jpd.web.model.Language;
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

import java.util.List;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Test cho CourseInfController - endpoint GET /api/course/recommend_courses
 *
 * <p><b>Phạm vi test:</b> Controller layer - kiểm tra request mapping, response status và JSON serialization</p>
 *
 * <p><b>Kịch bản test bao gồm:</b></p>
 * <ul>
 *   <li><b>HAPPY PATHS:</b>
 *     <ul>
 *       <li>TC_HP_01: Service trả về danh sách nhiều courses</li>
 *       <li>TC_HP_02: Service trả về danh sách có 1 course</li>
 *     </ul>
 *   </li>
 *   <li><b>EDGE CASES:</b>
 *     <ul>
 *       <li>TC_EC_01: Service trả về danh sách rỗng</li>
 *       <li>TC_EC_02: Courses có một số field null hoặc giá trị đặc biệt</li>
 *     </ul>
 *   </li>
 *   <li><b>ERROR SCENARIOS:</b>
 *     <ul>
 *       <li>TC_ES_01: Service ném RuntimeException</li>
 *       <li>TC_ES_02: Service ném NullPointerException</li>
 *       <li>TC_ES_03: Service ném IllegalStateException</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p><b>Dependencies được mock:</b> CourseInfService</p>
 *
 * @author QA Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CourseInfController - GET /api/course/recommend_courses Test Suite")
class CourseInfControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CourseInfService courseInfService;

    @InjectMocks
    private CourseInfController courseInfController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(courseInfController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ============================================================
    // HAPPY PATHS
    // ============================================================

    /**
     * TC_HP_01: shouldReturn200AndListOfCourses_whenServiceReturnsMultipleCourses
     *
     * <p><b>Given:</b> courseInfService.getRecommendCourses() trả về List chứa 3 CourseInfDto
     * với đầy đủ các field (id, name, img, numberStudent, rating, instructor, price, language)</p>
     *
     * <p><b>When:</b> gọi GET /api/course/recommend_courses</p>
     *
     * <p><b>Then:</b></p>
     * <ul>
     *   <li>HTTP status = 200 OK</li>
     *   <li>Content-Type = application/json</li>
     *   <li>Body = JSON array size = 3</li>
     *   <li>Các field của từng course được serialize đúng</li>
     * </ul>
     */
    @Test
    @DisplayName("TC_HP_01: Trả về 200 và danh sách courses khi service trả về nhiều courses")
    void shouldReturn200AndListOfCourses_whenServiceReturnsMultipleCourses() throws Exception {
        // =====================
        // Given
        // =====================
        CourseInfDto dto1 = CourseInfDto.builder()
                .id(101L)
                .name("Japanese Conversation Mastery")
                .img("https://example.com/japanese.jpg")
                .numberStudent(4500)
                .rating(4.8)
                .instructor("Akiko Suzuki")
                .price(699000)
                .language(Language.JAPANESE)
                .build();

        CourseInfDto dto2 = CourseInfDto.builder()
                .id(202L)
                .name("English for Business")
                .img("https://example.com/english.jpg")
                .numberStudent(3200)
                .rating(4.6)
                .instructor("John Smith")
                .price(599000)
                .language(Language.ENGLISH)
                .build();

        CourseInfDto dto3 = CourseInfDto.builder()
                .id(303L)
                .name("French Pronunciation")
                .img("https://example.com/french.jpg")
                .numberStudent(1500)
                .rating(4.5)
                .instructor("Marie Dubois")
                .price(550000)
                .language(Language.FRENCH)
                .build();

        List<CourseInfDto> mockedList = List.of(dto1, dto2, dto3);

        when(courseInfService.getRecommendCourses()).thenReturn(mockedList);

        // =====================
        // When / Then
        // =====================
        mockMvc.perform(get("/api/course/recommend_courses"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                
                // Validate course thứ nhất
                .andExpect(jsonPath("$[0].id").value(101L))
                .andExpect(jsonPath("$[0].name").value("Japanese Conversation Mastery"))
                .andExpect(jsonPath("$[0].img").value("https://example.com/japanese.jpg"))
                .andExpect(jsonPath("$[0].numberStudent").value(4500))
                .andExpect(jsonPath("$[0].rating").value(4.8))
                .andExpect(jsonPath("$[0].instructor").value("Akiko Suzuki"))
                .andExpect(jsonPath("$[0].price").value(699000))
                .andExpect(jsonPath("$[0].language").value("JAPANESE"))
                
                // Validate course thứ hai
                .andExpect(jsonPath("$[1].id").value(202L))
                .andExpect(jsonPath("$[1].name").value("English for Business"))
                .andExpect(jsonPath("$[1].rating").value(4.6))
                
                // Validate course thứ ba
                .andExpect(jsonPath("$[2].id").value(303L))
                .andExpect(jsonPath("$[2].name").value("French Pronunciation"))
                .andExpect(jsonPath("$[2].language").value("FRENCH"));
    }

    /**
     * TC_HP_02: shouldReturn200AndSingleCourse_whenServiceReturnsOneElement
     *
     * <p><b>Given:</b> courseInfService.getRecommendCourses() trả về List chứa 1 phần tử duy nhất</p>
     *
     * <p><b>When:</b> gọi GET /api/course/recommend_courses</p>
     *
     * <p><b>Then:</b></p>
     * <ul>
     *   <li>HTTP status = 200 OK</li>
     *   <li>Body = JSON array size = 1</li>
     *   <li>Course được serialize đúng các field</li>
     * </ul>
     */
    @Test
    @DisplayName("TC_HP_02: Trả về 200 và array có 1 phần tử khi service trả về 1 course")
    void shouldReturn200AndSingleCourse_whenServiceReturnsOneElement() throws Exception {
        // =====================
        // Given
        // =====================
        CourseInfDto singleCourse = CourseInfDto.builder()
                .id(999L)
                .name("Korean for Beginners")
                .img("https://example.com/korean.jpg")
                .numberStudent(2000)
                .rating(4.7)
                .instructor("Kim Min-ji")
                .price(650000)
                .language(Language.KOREAN)
                .build();

        List<CourseInfDto> singleElementList = List.of(singleCourse);

        when(courseInfService.getRecommendCourses()).thenReturn(singleElementList);

        // =====================
        // When / Then
        // =====================
        mockMvc.perform(get("/api/course/recommend_courses"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(999L))
                .andExpect(jsonPath("$[0].name").value("Korean for Beginners"))
                .andExpect(jsonPath("$[0].numberStudent").value(2000));
    }

    // ============================================================
    // EDGE CASES
    // ============================================================

    /**
     * TC_EC_01: shouldReturn200AndEmptyArray_whenServiceReturnsEmptyList
     *
     * <p><b>Given:</b> courseInfService.getRecommendCourses() trả về Collections.emptyList()</p>
     *
     * <p><b>When:</b> gọi GET /api/course/recommend_courses</p>
     *
     * <p><b>Then:</b></p>
     * <ul>
     *   <li>HTTP status = 200 OK (không ném exception)</li>
     *   <li>Body = [] (JSON array rỗng)</li>
     * </ul>
     */
    @Test
    @DisplayName("TC_EC_01: Trả về 200 và array rỗng khi service trả về empty list")
    void shouldReturn200AndEmptyArray_whenServiceReturnsEmptyList() throws Exception {
        // =====================
        // Given
        // =====================
        when(courseInfService.getRecommendCourses()).thenReturn(Collections.emptyList());

        // =====================
        // When / Then
        // =====================
        mockMvc.perform(get("/api/course/recommend_courses"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(jsonPath("$", empty()));
    }

    /**
     * TC_EC_02: shouldHandleCoursesWithNullOrMissingFields
     *
     * <p><b>Given:</b> courseInfService.getRecommendCourses() trả về List chứa CourseInfDto
     * với một số field có giá trị null, 0, hoặc chuỗi rỗng</p>
     *
     * <p><b>When:</b> gọi GET /api/course/recommend_courses</p>
     *
     * <p><b>Then:</b></p>
     * <ul>
     *   <li>HTTP status = 200 OK</li>
     *   <li>JSON vẫn serialize đúng, field null sẽ là null trong JSON</li>
     *   <li>Field có giá trị 0 sẽ là 0 trong JSON</li>
     * </ul>
     */
    @Test
    @DisplayName("TC_EC_02: Xử lý đúng courses có field null hoặc giá trị đặc biệt")
    void shouldHandleCoursesWithNullOrMissingFields() throws Exception {
        // =====================
        // Given
        // =====================
        CourseInfDto courseWithNullFields = CourseInfDto.builder()
                .id(555L)
                .name("Course with missing data")
                .img(null)  // null image
                .numberStudent(0)  // 0 students
                .rating(0.0)  // 0 rating
                .instructor(null)  // null instructor
                .price(0)  // free course
                .language(Language.ENGLISH)
                .build();

        List<CourseInfDto> listWithNullFields = List.of(courseWithNullFields);

        when(courseInfService.getRecommendCourses()).thenReturn(listWithNullFields);

        // =====================
        // When / Then
        // =====================
        mockMvc.perform(get("/api/course/recommend_courses"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(555L))
                .andExpect(jsonPath("$[0].name").value("Course with missing data"))
                .andExpect(jsonPath("$[0].img").isEmpty())
                .andExpect(jsonPath("$[0].numberStudent").value(0))
                .andExpect(jsonPath("$[0].rating").value(0.0))
                .andExpect(jsonPath("$[0].instructor").isEmpty())
                .andExpect(jsonPath("$[0].price").value(0));
    }

    // ============================================================
    // ERROR SCENARIOS
    // ============================================================

    /**
     * TC_ES_01: shouldReturn500_whenServiceThrowsRuntimeException
     *
     * <p><b>Given:</b> courseInfService.getRecommendCourses() ném RuntimeException("Database connection failed")</p>
     *
     * <p><b>When:</b> gọi GET /api/course/recommend_courses</p>
     *
     * <p><b>Then:</b></p>
     * <ul>
     *   <li>HTTP status = 500 Internal Server Error</li>
     *   <li>GlobalExceptionHandler bắt exception và trả về ErrorResponse</li>
     *   <li>Body chứa code = "INTERNAL_ERROR", message, userMessage, timestamp, traceId</li>
     * </ul>
     */
    @Test
    @DisplayName("TC_ES_01: Trả về 500 khi service ném RuntimeException")
    void shouldReturn500_whenServiceThrowsRuntimeException() throws Exception {
        // =====================
        // Given
        // =====================
        when(courseInfService.getRecommendCourses())
                .thenThrow(new RuntimeException("Database connection failed"));

        // =====================
        // When / Then
        // =====================
        mockMvc.perform(get("/api/course/recommend_courses"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Database connection failed"))
                .andExpect(jsonPath("$.userMessage").value("Có lỗi xảy ra trên server, vui lòng thử lại sau"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.path").value("/api/course/recommend_courses"));
    }

    /**
     * TC_ES_02: shouldReturn500_whenServiceThrowsNullPointerException
     *
     * <p><b>Given:</b> courseInfService.getRecommendCourses() ném NullPointerException</p>
     *
     * <p><b>When:</b> gọi GET /api/course/recommend_courses</p>
     *
     * <p><b>Then:</b></p>
     * <ul>
     *   <li>HTTP status = 500 Internal Server Error</li>
     *   <li>GlobalExceptionHandler bắt bởi @ExceptionHandler(Exception.class)</li>
     *   <li>Body chứa ErrorResponse với code "INTERNAL_ERROR"</li>
     * </ul>
     */
    @Test
    @DisplayName("TC_ES_02: Trả về 500 khi service ném NullPointerException")
    void shouldReturn500_whenServiceThrowsNullPointerException() throws Exception {
        // =====================
        // Given
        // =====================
        when(courseInfService.getRecommendCourses())
                .thenThrow(new NullPointerException("Unexpected null value"));

        // =====================
        // When / Then
        // =====================
        mockMvc.perform(get("/api/course/recommend_courses"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected null value"))
                .andExpect(jsonPath("$.userMessage").value("Có lỗi xảy ra trên server, vui lòng thử lại sau"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    /**
     * TC_ES_03: shouldReturn500_whenServiceThrowsIllegalStateException
     *
     * <p><b>Given:</b> courseInfService.getRecommendCourses() ném IllegalStateException</p>
     *
     * <p><b>When:</b> gọi GET /api/course/recommend_courses</p>
     *
     * <p><b>Then:</b></p>
     * <ul>
     *   <li>HTTP status = 500 Internal Server Error</li>
     *   <li>GlobalExceptionHandler xử lý và trả về ErrorResponse</li>
     * </ul>
     */
    @Test
    @DisplayName("TC_ES_03: Trả về 500 khi service ném IllegalStateException")
    void shouldReturn500_whenServiceThrowsIllegalStateException() throws Exception {
        // =====================
        // Given
        // =====================
        when(courseInfService.getRecommendCourses())
                .thenThrow(new IllegalStateException("Service in invalid state"));

        // =====================
        // When / Then
        // =====================
        mockMvc.perform(get("/api/course/recommend_courses"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Service in invalid state"))
                .andExpect(jsonPath("$.userMessage").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.path").exists());
    }
}
