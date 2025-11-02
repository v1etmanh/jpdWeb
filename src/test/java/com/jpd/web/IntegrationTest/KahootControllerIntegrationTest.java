package com.jpd.web.IntegrationTest;

import com.jpd.web.controller.common.GlobalExceptionHandler;
import com.jpd.web.controller.creator.FileUploadController;
import com.jpd.web.controller.creator.KahootController;
import com.jpd.web.dto.KahootDto;
import com.jpd.web.model.KahootListFunction;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.service.KahootService;
import com.jpd.web.service.utils.RequestAttributeExtractor;
import static org.mockito.Mockito.mockStatic;

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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KahootController.class)
@ContextConfiguration(classes = {KahootController.class, GlobalExceptionHandler.class})
class KahootControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;


    @MockBean
    private KahootService kahootService;

    @Test
    @DisplayName("GET /retrieveAll - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void retrieveAll_Success() throws Exception {
        KahootDto dto = KahootDto.builder()
                .id(1L)
                .title("Test Kahoot")
                .createDate(LocalDateTime.now())
                .numberQuestion(3)
                .build();

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(10L);

            when(kahootService.retrieveAll(10L)).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/creator/kahoot/retrieveAll"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].title").value("Test Kahoot"));
        }
    }

    @Test
    @DisplayName("GET /{id}/moduleContents - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void getModuleContents_Success() throws Exception {
        ModuleContent mc = new ModuleContent() {
            { this.mcId = 100L; }
        };

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(10L);

            when(kahootService.retrieveData(10L, 1L)).thenReturn(List.of(mc));

            mockMvc.perform(get("/api/creator/kahoot/1/moduleContents"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].mcId").value(100));
        }
    }

    @Test
    @DisplayName("POST /create - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void createKahoot_Success() throws Exception {
        KahootListFunction kh = KahootListFunction.builder()
                .kahootId(1L)
                .title("New Kahoot")
                .build();

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(10L);

            when(kahootService.createKahoot("New Kahoot", 10L)).thenReturn(kh);

            mockMvc.perform(post("/api/creator/kahoot/create")
                            .param("title", "New Kahoot")
                            .with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("New Kahoot"));
        }
    }

    @Test
    @DisplayName("DELETE /{id} - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void deleteKahoot_Success() throws Exception {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(10L);

            mockMvc.perform(delete("/api/creator/kahoot/1")
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    @DisplayName("PUT /{id} - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void updateKahoot_Success() throws Exception {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(10L);

            mockMvc.perform(put("/api/creator/kahoot/1")
                            .param("newTitle", "Updated Kahoot")
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }
}
