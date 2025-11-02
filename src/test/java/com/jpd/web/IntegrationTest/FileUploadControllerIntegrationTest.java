package com.jpd.web.IntegrationTest;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jpd.web.controller.creator.FileUploadController;
import com.jpd.web.controller.common.GlobalExceptionHandler;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.service.FileUploadService;
import com.jpd.web.service.utils.RequestAttributeExtractor;

import jakarta.servlet.http.HttpServletRequest;

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

@WebMvcTest(FileUploadController.class)
@ContextConfiguration(classes = {FileUploadController.class, GlobalExceptionHandler.class})
class FileUploadControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileUploadService fileUploadService;

    @Test
    @DisplayName("POST /savePdf - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void savePdf_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "pdf", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF content".getBytes()
        );

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any(HttpServletRequest.class)))
                    .thenReturn(10L);

            when(fileUploadService.saveImgIntoFirebase(10L, file, TypeOfFile.PDF))
                    .thenReturn("https://firebase/test.pdf");

            mockMvc.perform(multipart("/api/creator/uploadFile/savePdf")
                            .file(file)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("https://firebase/test.pdf"));
        }
    }

    @Test
    @DisplayName("POST /savePdf - failure (service returns null)")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void savePdf_Failure() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "pdf", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF content".getBytes()
        );

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any(HttpServletRequest.class)))
                    .thenReturn(10L);

            // Service trả null → controller trả 400
            when(fileUploadService.saveImgIntoFirebase(10L, file, TypeOfFile.PDF))
                    .thenReturn(null);

            mockMvc.perform(multipart("/api/creator/uploadFile/savePdf")
                            .file(file)
                            .with(csrf()))
                    .andExpect(status().isBadRequest());  // <-- sửa từ isOk() thành isBadRequest()
        }
    }


    @Test
    @DisplayName("POST /saveImg - success")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void saveImg_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "img", "test.png", MediaType.IMAGE_PNG_VALUE, "IMG content".getBytes()
        );

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any(HttpServletRequest.class)))
                    .thenReturn(10L);

            when(fileUploadService.saveImgIntoFirebase(10L, file, TypeOfFile.IMG))
                    .thenReturn("https://firebase/test.png");

            mockMvc.perform(multipart("/api/creator/uploadFile/saveImg")
                            .file(file)
                            .with(csrf()))  // <-- thêm dòng này
                    .andExpect(status().isOk())
                    .andExpect(content().string("https://firebase/test.png"));
        }
    }

    @Test
    @DisplayName("POST /saveImg - failure (service returns null)")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void saveImg_Failure() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "img", "test.png", MediaType.IMAGE_PNG_VALUE, "IMG content".getBytes()
        );

        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any(HttpServletRequest.class)))
                    .thenReturn(10L);

            when(fileUploadService.saveImgIntoFirebase(10L, file, TypeOfFile.IMG))
                    .thenReturn(null);

            mockMvc.perform(multipart("/api/creator/uploadFile/saveImg")
                            .file(file)
                    .with(csrf()))
                    .andExpect(status().isBadRequest());
        }
    }
}
