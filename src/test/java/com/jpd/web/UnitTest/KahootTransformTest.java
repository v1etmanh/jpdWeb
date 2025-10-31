package com.jpd.web.UnitTest;


import com.jpd.web.dto.KahootDto;
import com.jpd.web.model.KahootListFunction;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.transform.KahootTransform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.NullSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KahootTransformTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ParameterizedTest
    @CsvFileSource(resources = "/kahoot-transform-test-data.csv", numLinesToSkip = 1)
    @DisplayName("Should transform KahootListFunction to KahootDto with various data")
    void testTransformToKahootDto(Long kahootId, String title, String createDateStr, int moduleContentSize) {
        // Arrange
        LocalDateTime createDate = LocalDateTime.parse(createDateStr, FORMATTER);

        List<ModuleContent> moduleContentList = null;
        if (moduleContentSize > 0) {
            moduleContentList = new ArrayList<>();
            for (int i = 0; i < moduleContentSize; i++) {
                moduleContentList.add(mock(ModuleContent.class));
            }
        }

        KahootListFunction mockKahoot = mock(KahootListFunction.class);
        when(mockKahoot.getKahootId()).thenReturn(kahootId);
        when(mockKahoot.getTitle()).thenReturn(title);
        when(mockKahoot.getCreateDate()).thenReturn(createDate);
        when(mockKahoot.getModuleContent()).thenReturn(moduleContentList);

        // Act
        KahootDto result = KahootTransform.transformToKahootDto(mockKahoot);

        // Assert
        assertNotNull(result);
        assertEquals(kahootId, result.getId());
        assertEquals(title, result.getTitle());
        assertEquals(createDate, result.getCreateDate());
        assertEquals(moduleContentSize, result.getNumberQuestion());

        verify(mockKahoot, times(1)).getKahootId();
        verify(mockKahoot, times(1)).getTitle();
        verify(mockKahoot, times(1)).getCreateDate();
        verify(mockKahoot, atLeast(1)).getModuleContent();
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw NullPointerException when KahootListFunction is null")
    void testTransformToKahootDto_NullKahoot(KahootListFunction kahoot) {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            KahootTransform.transformToKahootDto(kahoot);
        });
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/kahoot-transform-null-modulecontent-test-data.csv", numLinesToSkip = 1)
    @DisplayName("Should handle null ModuleContent and return numberQuestion as 0")
    void testTransformToKahootDto_NullModuleContent(Long kahootId, String title, String createDateStr) {
        // Arrange
        LocalDateTime createDate = LocalDateTime.parse(createDateStr, FORMATTER);

        KahootListFunction mockKahoot = mock(KahootListFunction.class);
        when(mockKahoot.getKahootId()).thenReturn(kahootId);
        when(mockKahoot.getTitle()).thenReturn(title);
        when(mockKahoot.getCreateDate()).thenReturn(createDate);
        when(mockKahoot.getModuleContent()).thenReturn(null);

        // Act
        KahootDto result = KahootTransform.transformToKahootDto(mockKahoot);

        // Assert
        assertNotNull(result);
        assertEquals(kahootId, result.getId());
        assertEquals(title, result.getTitle());
        assertEquals(createDate, result.getCreateDate());
        assertEquals(0, result.getNumberQuestion());

        verify(mockKahoot, times(1)).getKahootId();
        verify(mockKahoot, times(1)).getTitle();
        verify(mockKahoot, times(1)).getCreateDate();
        verify(mockKahoot, atLeast(1)).getModuleContent();
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/kahoot-transform-edge-cases-test-data.csv", numLinesToSkip = 1)
    @DisplayName("Should handle edge cases with boundary values")
    void testTransformToKahootDto_EdgeCases(Long kahootId, String title, String createDateStr, int moduleContentSize) {
        // Arrange
        LocalDateTime createDate = LocalDateTime.parse(createDateStr, FORMATTER);

        List<ModuleContent> moduleContentList = new ArrayList<>();
        for (int i = 0; i < moduleContentSize; i++) {
            moduleContentList.add(mock(ModuleContent.class));
        }

        KahootListFunction mockKahoot = mock(KahootListFunction.class);
        when(mockKahoot.getKahootId()).thenReturn(kahootId);
        when(mockKahoot.getTitle()).thenReturn(title);
        when(mockKahoot.getCreateDate()).thenReturn(createDate);
        when(mockKahoot.getModuleContent()).thenReturn(moduleContentList);

        // Act
        KahootDto result = KahootTransform.transformToKahootDto(mockKahoot);

        // Assert
        assertNotNull(result);
        assertEquals(kahootId, result.getId());
        assertEquals(title, result.getTitle());
        assertEquals(createDate, result.getCreateDate());
        assertEquals(moduleContentSize, result.getNumberQuestion());
    }
}