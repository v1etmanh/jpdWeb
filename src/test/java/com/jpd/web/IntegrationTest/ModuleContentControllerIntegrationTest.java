package com.jpd.web.IntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpd.web.controller.common.GlobalExceptionHandler;
import com.jpd.web.controller.creator.ModuleContentController;
import com.jpd.web.dto.ModuleContentDto;
import com.jpd.web.model.*;
import com.jpd.web.service.ModuleContentService;
import com.jpd.web.service.utils.RequestAttributeExtractor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModuleContentController.class)
@ContextConfiguration(classes = {ModuleContentController.class, GlobalExceptionHandler.class})
class ModuleContentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ModuleContentService moduleContentService;

    private ModuleContent pdfContent;

    @BeforeEach
    void setUp() {
        pdfContent = new PdfDocument();
        pdfContent.setMcId(1L);
        pdfContent.setTypeOfContent(TypeOfContent.PDF);
        pdfContent.setKahootListFunction(new KahootListFunction());
    }

    @Test
    @DisplayName("GET / - get module contents by type")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void getModuleContentByTypeAndModuleId_Success() throws Exception {
        List<ModuleContent> contents = Collections.singletonList(pdfContent);

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any(HttpServletRequest.class)))
                    .thenReturn(10L);

            when(moduleContentService.getModuleContentsByTypeAndModuleId(
                    any(TypeOfContent.class), anyLong(), anyLong(), anyLong(), anyLong()))
                    .thenReturn(contents);

            mockMvc.perform(get("/api/creator/1/1/1")
                            .param("type", "PDF")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("DELETE /{moduleContentId} - delete module content")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void deleteModuleContent_Success() throws Exception {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any(HttpServletRequest.class)))
                    .thenReturn(10L);

            mockMvc.perform(delete("/api/creator/1/1/1/1")
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    @DisplayName("DELETE /deleteModuleContentByType - delete contents by type")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void deleteModuleContentByType_Success() throws Exception {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any(HttpServletRequest.class)))
                    .thenReturn(10L);

            mockMvc.perform(delete("/api/creator/1/1/1/deleteModuleContentByType")
                            .param("type", "PDF")
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    @DisplayName("POST / - update module contents")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void updateModuleContents_Success() throws Exception {
        ModuleContentDto dto = new ModuleContentDto();
        dto.setCourseId(1L);
        dto.setChapterId(1L);
        dto.setModuleId(1L);
        dto.setModuleContent(Collections.singletonList(pdfContent));

        List<ModuleContent> returned = Collections.singletonList(pdfContent);

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any(HttpServletRequest.class)))
                    .thenReturn(10L);

            when(moduleContentService.updateCourseMaterial(any(ModuleContentDto.class), anyLong()))
                    .thenReturn(returned);

            mockMvc.perform(post("/api/creator/1/1/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }
    }
}
