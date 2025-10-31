package com.jpd.web.UnitTest;

import static org.junit.jupiter.api.Assertions.*;

import com.jpd.web.service.AIService;
import com.jpd.web.service.GeminiAiService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class AIServiceTest {

    @InjectMocks
    private AIService aiService;

    @Mock
    private GeminiAiService geminiAiService;

    @ParameterizedTest
    @CsvFileSource(resources = "/data/ai_feedback.csv", numLinesToSkip = 1)
    void testGenerateFeedback(String question, String answer, String expectedContains) {
        Mockito.when(geminiAiService.generateContent(Mockito.anyString()))
                .thenReturn("Câu trả lời là " + expectedContains);

        String feedback = aiService.generateFeedback(question, answer);
        assertTrue(feedback.contains(expectedContains));
    }
}
