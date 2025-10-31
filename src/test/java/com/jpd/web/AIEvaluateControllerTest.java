package com.jpd.web;


import com.jpd.web.controller.customer.AIEvaluateController;
import com.jpd.web.model.SemanticResult;

import com.jpd.web.service.AiEvaluateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIEvaluateControllerTest {

    @Mock
    private AiEvaluateService aiEvaluateService;

    @Mock
    private MultipartFile mockFile;

    @Mock
    private Jwt mockJwt;

    @InjectMocks
    private AIEvaluateController aiEvaluateController;

    @Test
    void evaluateAnswer_shouldNormalizeLanguageAndReturnOk() throws Exception {
        // Arrange
        when(mockJwt.getClaimAsString("email")).thenReturn("test@example.com");
        SemanticResult mockResult = new SemanticResult();
        when(aiEvaluateService.evaluateSpeaking(mockFile, "Hello world", "en")).thenReturn(mockResult);

        // Act
        ResponseEntity<SemanticResult> response = aiEvaluateController.evaluateAnswer(
                mockFile, "Hello world", "en-US", mockJwt);

        // Assert
        verify(aiEvaluateService, times(1))
                .evaluateSpeaking(mockFile, "Hello world", "en");
        assertEquals(ResponseEntity.ok(mockResult), response);
    }

    @Test
    void evaluateAnswer_shouldReturnBadRequestWhenServiceThrowsException() throws Exception {
        // Arrange
        when(mockJwt.getClaimAsString("email")).thenReturn("test@example.com");
        when(aiEvaluateService.evaluateSpeaking(any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Mocked error"));

        // Act
        ResponseEntity<SemanticResult> response = aiEvaluateController.evaluateAnswer(
                mockFile, "Hello world", "en-US", mockJwt);

        // Assert
        assertEquals(400, response.getStatusCodeValue());
    }
}
