package com.jpd.web;

import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Language;
import com.jpd.web.model.Module;
import com.jpd.web.service.CourseService;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.CourseTransForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the getCourseById method in CourseService.
 * This test suite focuses on testing course retrieval functionality with various scenarios
 * including error cases and successful retrievals with different course structures.
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceGetCourseByIdTest {

    @Mock
    private ValidationResources validationResources;

    @InjectMocks
    private CourseService courseService;

    /**
     * Test verifies that CourseNotFoundException is thrown when attempting to retrieve
     * a course that doesn't exist. This validates proper error handling for invalid course IDs.
     */
    @Test
    void getCourseById_WhenCourseNotFound_ShouldThrowCourseNotFoundException() {
        // Arrange
        long courseId = 999L;
        long creatorId = 1L;

        when(validationResources.validateCourseOwnership(courseId, creatorId))
                .thenThrow(new CourseNotFoundException(courseId));

        // Act & Assert
        assertThrows(CourseNotFoundException.class, () -> {
            courseService.getCourseById(courseId, creatorId);
        }, "Should throw CourseNotFoundException when course doesn't exist");

        verify(validationResources, times(1)).validateCourseOwnership(courseId, creatorId);
    }

    /**
     * Test verifies that UnauthorizedException is thrown when a creator attempts to access
     * a course they don't own. This validates proper authorization checks.
     */
    @Test
    void getCourseById_WhenCreatorDoesNotOwnCourse_ShouldThrowUnauthorizedException() {
        // Arrange
        long courseId = 1L;
        long creatorId = 1L;

        when(validationResources.validateCourseOwnership(courseId, creatorId))
                .thenThrow(new UnauthorizedException("You don't have permission to modify this course"));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            courseService.getCourseById(courseId, creatorId);
        }, "Should throw UnauthorizedException when creator doesn't own the course");

        verify(validationResources, times(1)).validateCourseOwnership(courseId, creatorId);
    }

    /**
     * Test verifies successful retrieval of course content when the course exists,
     * creator has ownership, but the course has no chapters. Validates basic course
     * information is correctly transformed.
     */
    @Test
    void getCourseById_WhenCourseExistsWithNoChapters_ShouldReturnCourseContent() {
        // Arrange
        long courseId = 1L;
        long creatorId = 1L;

        Course course = Course.builder()
                .courseId(courseId)
                .name("Test Course")
                .language(Language.ENGLISH)
                .teachingLanguage(Language.ENGLISH)
                .isPublic(true)
                .chapters(new ArrayList<>())
                .build();

        CourseContentDto expectedDto = new CourseContentDto();
        expectedDto.setName("Test Course");
        expectedDto.setPublic(true);
        expectedDto.setLanguage(Language.ENGLISH);
        expectedDto.setTeachingLanguage(Language.ENGLISH);
        expectedDto.setChapters(new ArrayList<>());

        when(validationResources.validateCourseOwnership(courseId, creatorId)).thenReturn(course);

        try (MockedStatic<CourseTransForm> mockedTransform = mockStatic(CourseTransForm.class)) {
            mockedTransform.when(() -> CourseTransForm.transformToCourseContentDto(course))
                    .thenReturn(expectedDto);

            // Act
            CourseContentDto result = courseService.getCourseById(courseId, creatorId);

            // Assert
            assertNotNull(result, "Result should not be null");
            assertEquals("Test Course", result.getName(), "Course name should match");
            assertEquals(Language.ENGLISH, result.getLanguage(), "Language should match");
            assertEquals(Language.ENGLISH, result.getTeachingLanguage(), "Teaching language should match");
            assertTrue(result.isPublic(), "Course should be public");
            assertNotNull(result.getChapters(), "Chapters list should not be null");
            assertTrue(result.getChapters().isEmpty(), "Chapters list should be empty");

            verify(validationResources, times(1)).validateCourseOwnership(courseId, creatorId);
            mockedTransform.verify(() -> CourseTransForm.transformToCourseContentDto(course), times(1));
        }
    }

    /**
     * Test verifies successful retrieval of complete course content including chapters and modules.
     * This validates that the lazy-loaded relationships are properly accessed and the full
     * course structure is correctly transformed to DTO.
     */
    @Test
    void getCourseById_WhenCourseExistsWithChaptersAndModules_ShouldReturnCompleteCourseContent() {
        // Arrange
        long courseId = 1L;
        long creatorId = 1L;

        // Create modules
        Module module1 = Module.builder()
                .moduleId(1L)
                .titleOfModule("Module 1")
                .build();

        Module module2 = Module.builder()
                .moduleId(2L)
                .titleOfModule("Module 2")
                .build();

        // Create chapters with modules
        Chapter chapter1 = Chapter.builder()
                .chapterId(1L)
                .ChapterName("Chapter 1")
                .orderInCourse(1)
                .modules(List.of(module1, module2))
                .build();

        Chapter chapter2 = Chapter.builder()
                .chapterId(2L)
                .ChapterName("Chapter 2")
                .orderInCourse(2)
                .modules(new ArrayList<>())
                .build();

        // Create course with chapters
        Course course = Course.builder()
                .courseId(courseId)
                .name("Complete Course")
                .language(Language.ENGLISH)
                .teachingLanguage(Language.VIETNAMESE)
                .isPublic(true)
                .chapters(List.of(chapter1, chapter2))
                .build();

        CourseContentDto expectedDto = new CourseContentDto();
        expectedDto.setName("Complete Course");
        expectedDto.setPublic(true);
        expectedDto.setLanguage(Language.ENGLISH);
        expectedDto.setTeachingLanguage(Language.VIETNAMESE);
        expectedDto.setChapters(List.of(chapter1, chapter2));

        when(validationResources.validateCourseOwnership(courseId, creatorId)).thenReturn(course);

        try (MockedStatic<CourseTransForm> mockedTransform = mockStatic(CourseTransForm.class)) {
            mockedTransform.when(() -> CourseTransForm.transformToCourseContentDto(course))
                    .thenReturn(expectedDto);

            // Act
            CourseContentDto result = courseService.getCourseById(courseId, creatorId);

            // Assert
            assertNotNull(result, "Result should not be null");
            assertEquals("Complete Course", result.getName(), "Course name should match");
            assertEquals(Language.ENGLISH, result.getLanguage(), "Language should match");
            assertEquals(Language.VIETNAMESE, result.getTeachingLanguage(), "Teaching language should match");
            assertTrue(result.isPublic(), "Course should be public");

            assertNotNull(result.getChapters(), "Chapters list should not be null");
            assertEquals(2, result.getChapters().size(), "Should have 2 chapters");

            // Verify first chapter
            assertEquals("Chapter 1", result.getChapters().get(0).getChapterName(), "First chapter name should match");
            assertEquals(2, result.getChapters().get(0).getModules().size(), "First chapter should have 2 modules");

            // Verify second chapter
            assertEquals("Chapter 2", result.getChapters().get(1).getChapterName(), "Second chapter name should match");
            assertTrue(result.getChapters().get(1).getModules().isEmpty(), "Second chapter should have no modules");

            verify(validationResources, times(1)).validateCourseOwnership(courseId, creatorId);
            mockedTransform.verify(() -> CourseTransForm.transformToCourseContentDto(course), times(1));
        }
    }
}

