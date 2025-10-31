package com.jpd.web.UnitTest;


import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.model.RememberWord;
import com.jpd.web.transform.RememberTransform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.NullSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RememberTransformTest {

    @ParameterizedTest
    @CsvFileSource(resources = "/remember-word-to-dto-test-data.csv", numLinesToSkip = 1)
    @DisplayName("Should transform RememberWord to RememberWordDto with various data")
    void testToRememberWordDto(Long id, String word, String description, String meaning) {
        // Arrange
        RememberWord mockRememberWord = mock(RememberWord.class);
        when(mockRememberWord.getId()).thenReturn(id);
        when(mockRememberWord.getWord()).thenReturn(word);
        when(mockRememberWord.getDescription()).thenReturn(description);
        when(mockRememberWord.getMeaning()).thenReturn(meaning);

        // Act
        RememberWordDto result = RememberTransform.toRememberWordDto(mockRememberWord);

        // Assert
        assertNotNull(result);
        assertEquals(id, result.getRwId());
        assertEquals(word, result.getWord());
        assertEquals(description, result.getDescription());
        assertEquals(meaning, result.getMeaning());

        verify(mockRememberWord, times(1)).getId();
        verify(mockRememberWord, times(1)).getWord();
        verify(mockRememberWord, times(1)).getDescription();
        verify(mockRememberWord, times(1)).getMeaning();
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw NullPointerException when RememberWord is null")
    void testToRememberWordDto_NullRememberWord(RememberWord rememberWord) {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            RememberTransform.toRememberWordDto(rememberWord);
        });
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/remember-word-dto-to-entity-test-data.csv", numLinesToSkip = 1)
    @DisplayName("Should transform RememberWordDto to RememberWord with various data")
    void testToRememberWord(String word, String description, String meaning) {
        // Arrange
        RememberWordDto mockDto = mock(RememberWordDto.class);
        when(mockDto.getWord()).thenReturn(word);
        when(mockDto.getDescription()).thenReturn(description);
        when(mockDto.getMeaning()).thenReturn(meaning);

        // Act
        RememberWord result = RememberTransform.toRememberWord(mockDto);

        // Assert
        assertNotNull(result);
        assertEquals(word, result.getWord());
        assertEquals(description, result.getDescription());
        assertEquals(meaning, result.getMeaning());

        verify(mockDto, times(1)).getWord();
        verify(mockDto, times(1)).getDescription();
        verify(mockDto, times(1)).getMeaning();
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw NullPointerException when RememberWordDto is null")
    void testToRememberWord_NullDto(RememberWordDto dto) {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            RememberTransform.toRememberWord(dto);
        });
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/remember-word-edge-cases-test-data.csv", numLinesToSkip = 1)
    @DisplayName("Should handle edge cases with boundary and special values")
    void testToRememberWordDto_EdgeCases(Long id, String word, String description, String meaning) {
        // Arrange
        RememberWord mockRememberWord = mock(RememberWord.class);
        when(mockRememberWord.getId()).thenReturn(id);
        when(mockRememberWord.getWord()).thenReturn(word);
        when(mockRememberWord.getDescription()).thenReturn(description);
        when(mockRememberWord.getMeaning()).thenReturn(meaning);

        // Act
        RememberWordDto result = RememberTransform.toRememberWordDto(mockRememberWord);

        // Assert
        assertNotNull(result);
        assertEquals(id, result.getRwId());
        assertEquals(word, result.getWord());
        assertEquals(description, result.getDescription());
        assertEquals(meaning, result.getMeaning());
    }
}