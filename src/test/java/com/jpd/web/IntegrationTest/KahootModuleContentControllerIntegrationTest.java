package com.jpd.web.IntegrationTest;

import com.jpd.web.controller.common.GlobalExceptionHandler;
import com.jpd.web.controller.creator.KahootController;
import com.jpd.web.controller.creator.KahootModuleContentController;
import com.jpd.web.model.KahootListFunction;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.PdfDocument;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.service.KahootModuleContentService;
import com.jpd.web.service.utils.RequestAttributeExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(KahootModuleContentController.class)
@ContextConfiguration(classes = {KahootModuleContentController.class, GlobalExceptionHandler.class})

public class KahootModuleContentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KahootModuleContentService kahootModuleContentService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("DELETE /{moduleContentId} - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void deleteModuleContent_Success() throws Exception {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(10L);

            mockMvc.perform(delete("/api/creator/kahootModuleContent/1/100")
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    @DisplayName("POST / - update module contents success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void updateModuleContents_Success() throws Exception {
        // Tạo PDF document với typeOfContent hợp lệ
        PdfDocument doc = new PdfDocument();
        doc.setMcId(null);
        doc.setTypeOfContent(TypeOfContent.PDF); // dùng enum thay vì String
        doc.setKahootListFunction(null); // null cũng được nếu service chấp nhận

        List<ModuleContent> contents = Collections.singletonList(doc);

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(10L);

            when(kahootModuleContentService.updateCourseMaterial(any(), anyLong(), anyLong()))
                    .thenReturn(contents);

            mockMvc.perform(post("/api/creator/kahootModuleContent/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(contents))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }
    }



}
