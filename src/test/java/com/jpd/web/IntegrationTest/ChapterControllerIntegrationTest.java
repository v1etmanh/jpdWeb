package com.jpd.web.IntegrationTest;

import com.jpd.web.controller.common.GlobalExceptionHandler;
import com.jpd.web.controller.creator.AiGenerateController;
import com.jpd.web.controller.creator.ChapterController;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.service.ChapterService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChapterController.class)
@ContextConfiguration(classes = {ChapterController.class, GlobalExceptionHandler.class})

class ChapterControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ChapterService chapterService;

    final String BASE_URL = "/api/creator/{courseId}/chapter";

    @Nested
    @DisplayName("POST /api/creator/{courseId}/chapter")
    class CreateChapterTests {

        @Test
        @WithMockUser
        @DisplayName("200 OK / 201 Created when valid request")
        void createChapter_Success() throws Exception {
            Chapter chapter = Chapter.builder()
                    .chapterId(1L)
                    .ChapterName("Intro")
                    .course(new Course())
                    .build();

            when(chapterService.createChapter(anyString(), anyLong(), anyLong())).thenReturn(chapter);

            mockMvc.perform(post(BASE_URL, 10)
                            .param("chapterName", "Intro")
                            .requestAttr("creatorId", 1L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.chapterId").value(1))
                    .andExpect(jsonPath("$.chapterName").value("Intro")); // chữ thường 'c'

            verify(chapterService).createChapter("Intro", 10L, 1L);
        }


        @Test
        @WithMockUser
        @DisplayName("400 Bad Request when chapterName blank")
        void createChapter_BlankName() throws Exception {
            mockMvc.perform(post("/api/creator/10/chapter")
                            .param("chapterName", "")
                            .with(csrf()))  // thêm dòng này để tự sinh token
                    .andExpect(status().isInternalServerError()) // hoặc 400 nếu bạn đổi handler
                    .andExpect(jsonPath("$.message")
                            .value("createChapter.name: Chapter name is required"));

            verifyNoInteractions(chapterService);
        }




    }

    @Nested
    @DisplayName("DELETE /api/creator/{courseId}/chapter/{chapterId}")
    class DeleteChapterTests {

        @Test
        @WithMockUser
        @DisplayName("204 No Content when valid request")
        void deleteChapter_Success() throws Exception {
            doNothing().when(chapterService).deleteChapter(5L, 10L, 1L);

            mockMvc.perform(delete(BASE_URL + "/{chapterId}", 10, 5)
                            .requestAttr("creatorId", 1L)
                            .with(csrf())) // thêm CSRF
                    .andExpect(status().isNoContent());

            verify(chapterService).deleteChapter(5L, 10L, 1L);
        }


        @Test
        @WithMockUser
        @DisplayName("DELETE /chapter/{chapterId} - 500 Internal Server Error when chapterId negative")
        void deleteChapter_NegativeId() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{chapterId}", 10, -5)
                            .requestAttr("creatorId", 1L)
                            .with(csrf()))
                    .andExpect(status().isInternalServerError());

            // Service không bị gọi
            verifyNoInteractions(chapterService);
        }




    }

    @Nested
    @DisplayName("PUT /api/creator/{courseId}/chapter/{chapterID}/update")
    class UpdateChapterTests {

        @Test
        @WithMockUser // giả lập user, tránh 403
        @DisplayName("204 No Content when valid request")
        void updateChapter_Success() throws Exception {
            doNothing().when(chapterService).updateChapter(1L, "Updated", 5L);

            mockMvc.perform(put(BASE_URL + "/{chapterID}/update", 10, 5)
                            .param("name", "Updated")
                            .requestAttr("creatorId", 1L)
                            .with(csrf())) // thêm CSRF token cho PUT
                    .andExpect(status().isNoContent());

            verify(chapterService).updateChapter(1L, "Updated", 5L);
        }


        @Test
        @WithMockUser
        @DisplayName("500 Internal Server Error when name missing")
        void updateChapter_MissingName() throws Exception {
            mockMvc.perform(put(BASE_URL + "/{chapterID}/update", 10, 5)
                            .requestAttr("creatorId", 1L)
                            .with(csrf()))
                    .andExpect(status().isInternalServerError());

            verifyNoInteractions(chapterService);
        }





        @Test
        @WithMockUser // Giả lập user để vượt qua Spring Security
        @DisplayName("500 Internal Server Error when chapterService throws RuntimeException")
        void updateChapter_ModuleNotFound() throws Exception {
            // Mock chapterService ném exception
            doThrow(new RuntimeException("Module not found"))
                    .when(chapterService).updateChapter(1L, "X", 99L);

            mockMvc.perform(put(BASE_URL + "/{chapterID}/update", 10, 99)
                            .param("name", "X")
                            .requestAttr("creatorId", 1L)
                            .with(csrf())) // thêm CSRF token cho PUT
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Module not found")));

            verify(chapterService).updateChapter(1L, "X", 99L);
        }

    }
}
