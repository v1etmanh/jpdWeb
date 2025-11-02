package com.jpd.web.IntegrationTest;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.Collections;

import com.jpd.web.controller.creator.CreatorController;
import com.jpd.web.dto.CreatorDashboardDTO;
import com.jpd.web.dto.CreatorDto;
import com.jpd.web.dto.PopularCourseDTO;
import com.jpd.web.model.Status;
import com.jpd.web.model.Withdraw;
import com.jpd.web.service.CreatorService;
import com.jpd.web.service.utils.RequestAttributeExtractor;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CreatorController.class)
@ContextConfiguration(classes = {CreatorController.class})
class CreatorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreatorService creatorService;


    @Test
    @DisplayName("GET /getAccount - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void getAccount_Success() throws Exception {
        CreatorDto dto = new CreatorDto("Nguyen Van A", "0123456789", "Bio", "imgUrl",
                Collections.emptyList(), "paypal@example.com", Status.SUCCESS);

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(10L);

            when(creatorService.getAccount(10L)).thenReturn(dto);

            mockMvc.perform(get("/api/creator/getAccount"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("Nguyen Van A"))
                    .andExpect(jsonPath("$.phone").value("0123456789"))
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }
    }

    @Test
    @DisplayName("POST /upload/paypalEmail - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void uploadPaypalEmail_Success() throws Exception {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(10L);

            doNothing().when(creatorService).sendMoneyToVerify(eq(10L), anyString());

            mockMvc.perform(post("/api/creator/upload/paypalEmail")
                            .param("pEmail", "paypal@example.com")
                            .with(csrf()))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @DisplayName("POST /createWithdraw - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void createWithdraw_Success() throws Exception {
        Withdraw w = Withdraw.builder().withdrawId(1L).amount(100).status(Status.PENDING).build();

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(10L);

            when(creatorService.createWithdraw(10L, 100)).thenReturn(w);

            mockMvc.perform(post("/api/creator/createWithdraw")
                            .param("amount", "100")
                            .with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.withdrawId").value(1))
                    .andExpect(jsonPath("$.amount").value(100));
        }
    }

    @Test
    @DisplayName("GET /getStatisticInfor - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void getStatisticInfor_Success() throws Exception {
        CreatorDashboardDTO dto = CreatorDashboardDTO.builder()
                .totalRevenue(500.0)
                .totalStudents(10)
                .totalCourses(2)
                .avgRating(4.5)
                .ppc(Collections.singletonList(new PopularCourseDTO()))
                .build();

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(10L);

            when(creatorService.retrieveStatictisInfo(10L)).thenReturn(dto);

            mockMvc.perform(get("/api/creator/getStatisticInfor"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRevenue").value(500.0))
                    .andExpect(jsonPath("$.totalStudents").value(10));
        }
    }

    @Test
    @DisplayName("POST /upade_certificate - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void updateCertificate_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("certificateFile", "cert.pdf", "application/pdf", "dummy".getBytes());

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(10L);

            doNothing().when(creatorService).upLoadCertificate(eq(10L), any());

            mockMvc.perform(multipart("/api/creator/upade_certificate")
                            .file(file)
                            .with(csrf()))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @DisplayName("GET /history_transaction - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void historyTransaction_Success() throws Exception {
        Withdraw w = Withdraw.builder().withdrawId(1L).amount(100).status(Status.PENDING).build();

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(10L);

            when(creatorService.historyTransaction(10L)).thenReturn(Collections.singletonList(w));

            mockMvc.perform(get("/api/creator/history_transaction"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].withdrawId").value(1));
        }
    }

    @Test
    @DisplayName("GET /getBalance - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void getBalance_Success() throws Exception {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(10L);

            when(creatorService.getblance(10L)).thenReturn(1000.0);

            mockMvc.perform(get("/api/creator/getBalance"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1000.0"));
        }
    }
}
