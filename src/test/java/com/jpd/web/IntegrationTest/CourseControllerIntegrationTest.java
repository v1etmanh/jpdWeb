package com.jpd.web.IntegrationTest;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Collections;

import com.jpd.web.controller.common.GlobalExceptionHandler;
import com.jpd.web.controller.creator.CourseController;
import com.jpd.web.dto.*;
import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Language;
import com.jpd.web.model.Course;
import com.jpd.web.service.CourseService;
import com.jpd.web.service.utils.RequestAttributeExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;
import org.mockito.MockedStatic;

@WebMvcTest(CourseController.class)
@ContextConfiguration(classes = {CourseController.class, GlobalExceptionHandler.class})
class CourseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;


    @MockBean
    private CourseService courseService;

    @Test
    @DisplayName("POST /create - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void createCourse_Success() throws Exception {
        MockMultipartFile imgFile = new MockMultipartFile(
                "imgFile", "test.jpg", "image/jpeg", "dummy".getBytes()
        );

        CourseFormDto formDto = new CourseFormDto(
                "Java Course",
                "Description",
                "Beginners",
                null,
                null,
                Language.ENGLISH,
                Language.ENGLISH,
                0,
                imgFile,
                AccessMode.PUBLIC
        );

        Course savedCourse = new Course();
        savedCourse.setCourseId(1L);
        savedCourse.setName("Java Course");
        savedCourse.setUrlImg("imageUrl");
        savedCourse.setAccessMode(AccessMode.PUBLIC);

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any(HttpServletRequest.class)))
                    .thenReturn(10L);

            when(courseService.createCourse(any(CourseFormDto.class), eq(10L)))
                    .thenReturn(savedCourse);

            mockMvc.perform(multipart("/api/creator/course/create")
                            .file(imgFile)
                            .param("name", formDto.getName())
                            .param("description", formDto.getDescription())
                            .param("targetAudience", formDto.getTargetAudience())
                            .param("language", formDto.getLanguage().name())
                            .param("teachingLanguage", formDto.getTeachingLanguage().name())
                            .param("price", String.valueOf(formDto.getPrice()))
                            .param("accessMode", formDto.getAccessMode().name())
                            .with(csrf())   // <-- Thêm dòng này
                    )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Java Course"));
        }
    }

    @Test
    @DisplayName("GET / - retrieve all courses")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void retrieveAllCourses_Success() throws Exception {
        CourseCardDto dto = new CourseCardDto();
        dto.setId(1);
        dto.setName("Course A");
        dto.setCreatedDate(LocalDate.now());
        dto.setImage("url");
        dto.setType(AccessMode.PUBLIC);

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any(HttpServletRequest.class)))
                    .thenReturn(10L);

            when(courseService.retrieveCourseByemail(10L)).thenReturn(Collections.singletonList(dto));

            mockMvc.perform(get("/api/creator/course"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Course A"));
        }
    }

    @Test
    @DisplayName("GET /{id} - retrieve course by id")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void retrieveCourseById_Success() throws Exception {
        CourseContentDto dto = new CourseContentDto();
        dto.setName("Course A");
        dto.setPublic(true);

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any(HttpServletRequest.class)))
                    .thenReturn(10L);

            when(courseService.getCourseById(1L, 10L)).thenReturn(dto);

            mockMvc.perform(get("/api/creator/course/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Course A"))
                    .andExpect(jsonPath("$.public").value(true));
        }
    }

    @Test
    @DisplayName("GET /retrieve_CommercialCourese - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void retrieveCommercialCourses_Success() throws Exception {
        PopularCourseDTO dto = new PopularCourseDTO();
        dto.setCourseId(1);
        dto.setTitle("Popular Course");
        dto.setStudents(100);

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any(HttpServletRequest.class)))
                    .thenReturn(10L);

            when(courseService.retrieveCCourse(10L)).thenReturn(Collections.singletonList(dto));

            mockMvc.perform(get("/api/creator/course/retrieve_CommercialCourese"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].courseId").value(1))
                    .andExpect(jsonPath("$[0].title").value("Popular Course"));
        }
    }

    @Test
    @DisplayName("GET /{id}/setCourseStatus - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void setCourseStatus_Success() throws Exception {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any(HttpServletRequest.class)))
                    .thenReturn(10L);

            doNothing().when(courseService).changeCourseSatus(1L, 10L);

            mockMvc.perform(get("/api/creator/course/1/setCourseStatus"))
                    .andExpect(status().isOk());
        }
    }
}
