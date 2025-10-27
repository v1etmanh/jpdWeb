package com.jpd.web;

import com.jpd.web.dto.PopularCourseDTO;
import com.jpd.web.exception.CreatorNotFoundException;
import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.service.CourseService;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.CreatorTransform;
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
 * Unit tests for the retrieveCCourse method in CourseService.
 * This test suite focuses on testing the retrieval of paid courses for a creator
 * and their transformation to PopularCourseDTO objects.
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceRetrieveCCourseTest {

    @Mock
    private ValidationResources validationResources;

    @InjectMocks
    private CourseService courseService;

    /**
     * Test verifies that CreatorNotFoundException is thrown when attempting to retrieve
     * courses for a non-existent creator. This validates proper error handling.
     */
    @Test
    void retrieveCCourse_WhenCreatorNotFound_ShouldThrowCreatorNotFoundException() {
        // Arrange
        long creatorId = 999L;

        when(validationResources.validateCreatorExists(creatorId))
                .thenThrow(new CreatorNotFoundException(creatorId));

        // Act & Assert
        assertThrows(CreatorNotFoundException.class, () -> {
            courseService.retrieveCCourse(creatorId);
        }, "Should throw CreatorNotFoundException when creator doesn't exist");

        verify(validationResources, times(1)).validateCreatorExists(creatorId);
    }

    /**
     * Test verifies that an empty list is returned when a creator exists but has no courses.
     * This validates handling of creators with empty course lists.
     */
    @Test
    void retrieveCCourse_WhenCreatorHasNoCourses_ShouldReturnEmptyList() {
        // Arrange
        long creatorId = 1L;

        Creator creator = Creator.builder()
                .courses(new ArrayList<>())
                .build();

        when(validationResources.validateCreatorExists(creatorId)).thenReturn(creator);

        // Act
        List<PopularCourseDTO> result = courseService.retrieveCCourse(creatorId);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty when creator has no courses");
        assertEquals(0, result.size(), "Result size should be 0");

        verify(validationResources, times(1)).validateCreatorExists(creatorId);
    }

    /**
     * Test verifies that an empty list is returned when a creator has only PUBLIC or PRIVATE courses
     * (no PAID courses). This validates the filtering logic for PAID courses only.
     */
    @Test
    void retrieveCCourse_WhenCreatorHasOnlyNonPaidCourses_ShouldReturnEmptyList() {
        // Arrange
        long creatorId = 1L;

        Course publicCourse = Course.builder()
                .courseId(1L)
                .name("Public Course")
                .accessMode(AccessMode.PUBLIC)
                .build();

        Course privateCourse = Course.builder()
                .courseId(2L)
                .name("Private Course")
                .accessMode(AccessMode.PRIVATE)
                .build();

        Creator creator = Creator.builder()
                .courses(List.of(publicCourse, privateCourse))
                .build();

        when(validationResources.validateCreatorExists(creatorId)).thenReturn(creator);

        // Act
        List<PopularCourseDTO> result = courseService.retrieveCCourse(creatorId);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty when creator has no PAID courses");
        assertEquals(0, result.size(), "Result size should be 0");

        verify(validationResources, times(1)).validateCreatorExists(creatorId);
    }

    /**
     * Test verifies successful retrieval and transformation of a single PAID course.
     * This validates the basic happy path for one paid course.
     */
    @Test
    void retrieveCCourse_WhenCreatorHasOnePaidCourse_ShouldReturnSingleDTO() {
        // Arrange
        long creatorId = 1L;

        Course paidCourse = Course.builder()
                .courseId(100L)
                .name("Paid Course")
                .accessMode(AccessMode.PAID)
                .price(29.99)
                .build();

        Creator creator = Creator.builder()
                .courses(List.of(paidCourse))
                .build();

        PopularCourseDTO expectedDto = PopularCourseDTO.builder()
                .courseId(100L)
                .title("Paid Course")
                .price(29.99)
                .students(0)
                .revenue(0.0)
                .rating(0.0)
                .build();

        when(validationResources.validateCreatorExists(creatorId)).thenReturn(creator);

        try (MockedStatic<CreatorTransform> mockedTransform = mockStatic(CreatorTransform.class)) {
            mockedTransform.when(() -> CreatorTransform.transform(paidCourse))
                    .thenReturn(expectedDto);

            // Act
            List<PopularCourseDTO> result = courseService.retrieveCCourse(creatorId);

            // Assert
            assertNotNull(result, "Result should not be null");
            assertEquals(1, result.size(), "Result should contain exactly one course");
            assertEquals(100L, result.get(0).getCourseId(), "Course ID should match");
            assertEquals("Paid Course", result.get(0).getTitle(), "Course title should match");
            assertEquals(29.99, result.get(0).getPrice(), "Course price should match");

            verify(validationResources, times(1)).validateCreatorExists(creatorId);
            mockedTransform.verify(() -> CreatorTransform.transform(paidCourse), times(1));
        }
    }

    /**
     * Test verifies successful retrieval of multiple PAID courses while filtering out
     * PUBLIC and PRIVATE courses. This validates the complete filtering and transformation logic.
     */
    @Test
    void retrieveCCourse_WhenCreatorHasMultipleCourses_ShouldReturnOnlyPaidCourses() {
        // Arrange
        long creatorId = 1L;

        Course paidCourse1 = Course.builder()
                .courseId(101L)
                .name("Paid Course 1")
                .accessMode(AccessMode.PAID)
                .price(49.99)
                .build();

        Course publicCourse = Course.builder()
                .courseId(102L)
                .name("Public Course")
                .accessMode(AccessMode.PUBLIC)
                .build();

        Course paidCourse2 = Course.builder()
                .courseId(103L)
                .name("Paid Course 2")
                .accessMode(AccessMode.PAID)
                .price(39.99)
                .build();

        Course privateCourse = Course.builder()
                .courseId(104L)
                .name("Private Course")
                .accessMode(AccessMode.PRIVATE)
                .build();

        Creator creator = Creator.builder()
                .courses(List.of(paidCourse1, publicCourse, paidCourse2, privateCourse))
                .build();

        PopularCourseDTO dto1 = PopularCourseDTO.builder()
                .courseId(101L)
                .title("Paid Course 1")
                .price(49.99)
                .students(150)
                .revenue(7498.50)
                .rating(4.5)
                .build();

        PopularCourseDTO dto2 = PopularCourseDTO.builder()
                .courseId(103L)
                .title("Paid Course 2")
                .price(39.99)
                .students(200)
                .revenue(7998.00)
                .rating(4.8)
                .build();

        when(validationResources.validateCreatorExists(creatorId)).thenReturn(creator);

        try (MockedStatic<CreatorTransform> mockedTransform = mockStatic(CreatorTransform.class)) {
            mockedTransform.when(() -> CreatorTransform.transform(paidCourse1))
                    .thenReturn(dto1);
            mockedTransform.when(() -> CreatorTransform.transform(paidCourse2))
                    .thenReturn(dto2);

            // Act
            List<PopularCourseDTO> result = courseService.retrieveCCourse(creatorId);

            // Assert
            assertNotNull(result, "Result should not be null");
            assertEquals(2, result.size(), "Result should contain exactly two PAID courses");

            // Verify first paid course
            assertEquals(101L, result.get(0).getCourseId(), "First course ID should match");
            assertEquals("Paid Course 1", result.get(0).getTitle(), "First course title should match");
            assertEquals(49.99, result.get(0).getPrice(), "First course price should match");
            assertEquals(150, result.get(0).getStudents(), "First course students should match");
            assertEquals(7498.50, result.get(0).getRevenue(), "First course revenue should match");
            assertEquals(4.5, result.get(0).getRating(), "First course rating should match");

            // Verify second paid course
            assertEquals(103L, result.get(1).getCourseId(), "Second course ID should match");
            assertEquals("Paid Course 2", result.get(1).getTitle(), "Second course title should match");
            assertEquals(39.99, result.get(1).getPrice(), "Second course price should match");
            assertEquals(200, result.get(1).getStudents(), "Second course students should match");
            assertEquals(7998.00, result.get(1).getRevenue(), "Second course revenue should match");
            assertEquals(4.8, result.get(1).getRating(), "Second course rating should match");

            verify(validationResources, times(1)).validateCreatorExists(creatorId);
            mockedTransform.verify(() -> CreatorTransform.transform(paidCourse1), times(1));
            mockedTransform.verify(() -> CreatorTransform.transform(paidCourse2), times(1));
            // Verify PUBLIC and PRIVATE courses are not transformed
            mockedTransform.verify(() -> CreatorTransform.transform(publicCourse), never());
            mockedTransform.verify(() -> CreatorTransform.transform(privateCourse), never());
        }
    }

    /**
     * Test verifies that only PAID courses are returned when a creator has all three types
     * of courses (PUBLIC, PAID, PRIVATE). This ensures the filtering logic works correctly
     * with mixed course types.
     */
    @Test
    void retrieveCCourse_WhenCreatorHasAllCourseTypes_ShouldFilterAndReturnOnlyPaid() {
        // Arrange
        long creatorId = 1L;

        Course paidCourse = Course.builder()
                .courseId(201L)
                .name("Premium Java Course")
                .accessMode(AccessMode.PAID)
                .price(99.99)
                .build();

        Course publicCourse1 = Course.builder()
                .courseId(202L)
                .name("Public Intro Course")
                .accessMode(AccessMode.PUBLIC)
                .build();

        Course publicCourse2 = Course.builder()
                .courseId(203L)
                .name("Another Public Course")
                .accessMode(AccessMode.PUBLIC)
                .build();

        Course privateCourse = Course.builder()
                .courseId(204L)
                .name("Company Training")
                .accessMode(AccessMode.PRIVATE)
                .build();

        Creator creator = Creator.builder()
                .courses(List.of(publicCourse1, paidCourse, publicCourse2, privateCourse))
                .build();

        PopularCourseDTO expectedDto = PopularCourseDTO.builder()
                .courseId(201L)
                .title("Premium Java Course")
                .price(99.99)
                .students(500)
                .revenue(49995.00)
                .rating(4.9)
                .urlImg("https://example.com/image.jpg")
                .build();

        when(validationResources.validateCreatorExists(creatorId)).thenReturn(creator);

        try (MockedStatic<CreatorTransform> mockedTransform = mockStatic(CreatorTransform.class)) {
            mockedTransform.when(() -> CreatorTransform.transform(paidCourse))
                    .thenReturn(expectedDto);

            // Act
            List<PopularCourseDTO> result = courseService.retrieveCCourse(creatorId);

            // Assert
            assertNotNull(result, "Result should not be null");
            assertEquals(1, result.size(), "Result should contain only one PAID course");
            assertEquals(201L, result.get(0).getCourseId(), "Course ID should match");
            assertEquals("Premium Java Course", result.get(0).getTitle(), "Course title should match");
            assertEquals(99.99, result.get(0).getPrice(), "Course price should match");
            assertEquals(500, result.get(0).getStudents(), "Students count should match");
            assertEquals(49995.00, result.get(0).getRevenue(), "Revenue should match");
            assertEquals(4.9, result.get(0).getRating(), "Rating should match");
            assertEquals("https://example.com/image.jpg", result.get(0).getUrlImg(), "Image URL should match");

            verify(validationResources, times(1)).validateCreatorExists(creatorId);
            mockedTransform.verify(() -> CreatorTransform.transform(paidCourse), times(1));
            // Verify non-PAID courses are not transformed
            mockedTransform.verify(() -> CreatorTransform.transform(publicCourse1), never());
            mockedTransform.verify(() -> CreatorTransform.transform(publicCourse2), never());
            mockedTransform.verify(() -> CreatorTransform.transform(privateCourse), never());
        }
    }
}

