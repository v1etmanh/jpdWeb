package com.jpd.web.UnitTest;

import com.jpd.web.dto.FeedbackSimpleDto;
import com.jpd.web.model.Feedback;
import com.jpd.web.transform.FeedbackTransform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.NullSource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeedbackTransformTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @ParameterizedTest
    @CsvFileSource(resources = "/feedback-transform-test-data.csv", numLinesToSkip = 1)
    @DisplayName("Should transform Feedback to FeedbackSimpleDto with various data")
    void testToFeedbackDto(Long feedbackId, String content, Integer rate, String updateDateStr) {
        // Arrange
        LocalDate updateDate = LocalDate.parse(updateDateStr, FORMATTER);

        Feedback mockFeedback = mock(Feedback.class);
        when(mockFeedback.getFeedbackId()).thenReturn(feedbackId);
        when(mockFeedback.getContent()).thenReturn(content);
        when(mockFeedback.getRate()).thenReturn(rate);
        when(mockFeedback.getUpdateDate()).thenReturn(updateDate);

        // Act
        FeedbackSimpleDto result = FeedbackTransform.tofeedbackDto(mockFeedback);

        // Assert
        assertNotNull(result);
        assertEquals(feedbackId, result.getFeedbackId());
        assertEquals(content, result.getContent());
        assertEquals(rate, result.getRate());
        assertEquals(updateDate, result.getCreateDate());

        verify(mockFeedback, times(1)).getFeedbackId();
        verify(mockFeedback, times(1)).getContent();
        verify(mockFeedback, times(1)).getRate();
        verify(mockFeedback, times(1)).getUpdateDate();
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw NullPointerException when Feedback is null")
    void testToFeedbackDto_NullFeedback(Feedback feedback) {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            FeedbackTransform.tofeedbackDto(feedback);
        });
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/feedback-transform-edge-cases-test-data.csv", numLinesToSkip = 1)
    @DisplayName("Should handle edge cases with boundary and special values")
    void testToFeedbackDto_EdgeCases(Long feedbackId, String content, Integer rate, String updateDateStr) {
        // Arrange
        LocalDate updateDate = LocalDate.parse(updateDateStr, FORMATTER);

        Feedback mockFeedback = mock(Feedback.class);
        when(mockFeedback.getFeedbackId()).thenReturn(feedbackId);
        when(mockFeedback.getContent()).thenReturn(content);
        when(mockFeedback.getRate()).thenReturn(rate);
        when(mockFeedback.getUpdateDate()).thenReturn(updateDate);

        // Act
        FeedbackSimpleDto result = FeedbackTransform.tofeedbackDto(mockFeedback);

        // Assert
        assertNotNull(result);
        assertEquals(feedbackId, result.getFeedbackId());
        assertEquals(content, result.getContent());
        assertEquals(rate, result.getRate());
        assertEquals(updateDate, result.getCreateDate());
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/feedback-transform-various-ratings-test-data.csv", numLinesToSkip = 1)
    @DisplayName("Should handle various rating values from 1 to 5 stars")
    void testToFeedbackDto_VariousRatings(Long feedbackId, String content, Integer rate, String updateDateStr) {
        // Arrange
        LocalDate updateDate = LocalDate.parse(updateDateStr, FORMATTER);

        Feedback mockFeedback = mock(Feedback.class);
        when(mockFeedback.getFeedbackId()).thenReturn(feedbackId);
        when(mockFeedback.getContent()).thenReturn(content);
        when(mockFeedback.getRate()).thenReturn(rate);
        when(mockFeedback.getUpdateDate()).thenReturn(updateDate);

        // Act
        FeedbackSimpleDto result = FeedbackTransform.tofeedbackDto(mockFeedback);

        // Assert
        assertNotNull(result);
        assertEquals(feedbackId, result.getFeedbackId());
        assertEquals(content, result.getContent());
        assertEquals(rate, result.getRate());
        assertEquals(updateDate, result.getCreateDate());
        assertTrue(rate >= 1 && rate <= 5);
    }
}