package com.jpd.web.IntegrationTest;

import com.jpd.web.controller.common.GlobalExceptionHandler;
import com.jpd.web.controller.creator.ModuleController;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Module;
import com.jpd.web.service.ModuleService;
import com.jpd.web.service.utils.RequestAttributeExtractor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModuleController.class)
@ContextConfiguration(classes = {ModuleController.class, GlobalExceptionHandler.class})
class ModuleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ModuleService moduleService;


    @Test
    @DisplayName("DELETE /api/creator/{courseId}/{chapterId}/module/{moduleId} → 204 No Content")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void testDeleteModule_Success() throws Exception {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(1L);

            doNothing().when(moduleService).deleteModule(1L, 10L, 20L, 30L);

            mockMvc.perform(delete("/api/creator/10/20/module/30")
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    @DisplayName("POST /api/creator/{courseId}/{chapterId}/module → 201 Created with response body")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void testCreateModule_Success() throws Exception {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(1L);

            Chapter chapter = new Chapter();
            chapter.setCourse(new Course());
            Module module = Module.builder()
                    .moduleId(100L)
                    .titleOfModule("Test Module")
                    .chapter(chapter)
                    .build();

            when(moduleService.createModule(eq("Test Module"), eq(1L), eq(10L), eq(20L)))
                    .thenReturn(module);

            mockMvc.perform(post("/api/creator/10/20/module")
                            .param("moduleName", "Test Module")
                            .contentType(MediaType.APPLICATION_JSON)
                            .with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.moduleId").value(100L))
                    .andExpect(jsonPath("$.titleOfModule").value("Test Module"));
        }
    }

    @Test
    @DisplayName("PUT /api/creator/{courseId}/{chapterId}/module/{moduleId}/update → 204 No Content")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void testUpdateModuleName_Success() throws Exception {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(1L);

            doNothing().when(moduleService).updateModuleName(1L, 30L, "Updated Name");

            mockMvc.perform(put("/api/creator/10/20/module/30/update")
                            .param("name", "Updated Name")
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }
}
