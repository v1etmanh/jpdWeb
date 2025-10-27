package com.jpd.web;

import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.service.CourseService;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.CourseTransForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest_getAll {

    @Mock
    private CreatorRepository creatorRepository;

    @Mock
    private ValidationResources validationResources;

    @InjectMocks
    private CourseService courseService;


    /**
     * Test verifies that NoSuchElementException is thrown when attempting to retrieve courses
     * for a non-existent creator ID. This validates proper error handling for invalid creator lookups.
     */
    @Test
    void retrieveCourseByemail_WhenCreatorNotFound_ShouldThrowNoSuchElementException() {
        // Arrange
        long nonExistentCreatorId = 999L;
        when(creatorRepository.findById(nonExistentCreatorId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            courseService.retrieveCourseByemail(nonExistentCreatorId);
        }, "Should throw NoSuchElementException when creator is not found");

        verify(creatorRepository, times(1)).findById(nonExistentCreatorId);
    }

    /**
     * Parameterized test that verifies an empty list is returned when a creator exists but has no courses.
     * Test data is loaded from CSV file to test multiple creator IDs.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/test-data/creator-no-courses.csv", numLinesToSkip = 1)
    void retrieveCourseByemail_WhenCreatorExistsWithNoCourses_ShouldReturnEmptyList(long creatorId) {
        // Arrange
        Creator creator = Creator.builder()
                .courses(new ArrayList<>())
                .build();
        when(creatorRepository.findById(creatorId)).thenReturn(Optional.of(creator));

        // Act
        List<CourseCardDto> result = courseService.retrieveCourseByemail(creatorId);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty when creator has no courses");
        assertEquals(0, result.size(), "Result size should be 0");
        verify(creatorRepository, times(1)).findById(creatorId);
    }

    /**
     * Parameterized test that verifies a single course is correctly retrieved and transformed
     * when a creator has exactly one course. Validates course ID and name match expected values.
     * Uses CSV data to test multiple scenarios.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/test-data/creator-one-course.csv", numLinesToSkip = 1)
    void retrieveCourseByemail_WhenCreatorExistsWithOneCourse_ShouldReturnSingleCourse(
            long creatorId, long courseId, String courseName) {
        // Arrange
        Course course = new Course();
        course.setCourseId(courseId);
        course.setName(courseName);

        Creator creator = Creator.builder()
                .courses(List.of(course))
                .build();

        CourseCardDto expectedDto = new CourseCardDto();
        expectedDto.setId(courseId);
        expectedDto.setName(courseName);

        when(creatorRepository.findById(creatorId)).thenReturn(Optional.of(creator));

        try (MockedStatic<CourseTransForm> mockedTransform = mockStatic(CourseTransForm.class)) {
            mockedTransform.when(() -> CourseTransForm.transformToCourseCardDto(course))
                    .thenReturn(expectedDto);

            // Act
            List<CourseCardDto> result = courseService.retrieveCourseByemail(creatorId);

            // Assert
            assertNotNull(result, "Result should not be null");
            assertEquals(1, result.size(), "Result should contain exactly one course");
            assertEquals(courseId, result.get(0).getId(), "Course ID should match");
            assertEquals(courseName, result.get(0).getName(), "Course name should match");

            verify(creatorRepository, times(1)).findById(creatorId);
            mockedTransform.verify(() -> CourseTransForm.transformToCourseCardDto(course), times(1));
        }
    }

    /**
     * Parameterized test that verifies all courses are correctly retrieved and transformed
     * when a creator has multiple courses. Validates that the count, IDs, and names of all
     * courses match expected values. CSV data contains semicolon-separated course lists.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/test-data/creator-multiple-courses.csv", numLinesToSkip = 1)
    void retrieveCourseByemail_WhenCreatorExistsWithMultipleCourses_ShouldReturnAllCourses(
            long creatorId, String courseIds, String courseNames) {
        // Arrange
        String[] ids = courseIds.split(";");
        String[] names = courseNames.split(";");

        List<Course> courses = new ArrayList<>();
        List<CourseCardDto> dtos = new ArrayList<>();

        for (int i = 0; i < ids.length; i++) {
            Course course = new Course();
            course.setCourseId(Long.parseLong(ids[i]));
            course.setName(names[i]);
            courses.add(course);

            CourseCardDto dto = new CourseCardDto();
            dto.setId(Long.parseLong(ids[i]));
            dto.setName(names[i]);
            dtos.add(dto);
        }

        Creator creator = Creator.builder()
                .courses(courses)
                .build();

        when(creatorRepository.findById(creatorId)).thenReturn(Optional.of(creator));

        try (MockedStatic<CourseTransForm> mockedTransform = mockStatic(CourseTransForm.class)) {
            for (int i = 0; i < courses.size(); i++) {
                Course course = courses.get(i);
                CourseCardDto dto = dtos.get(i);
                mockedTransform.when(() -> CourseTransForm.transformToCourseCardDto(course))
                        .thenReturn(dto);
            }

            // Act
            List<CourseCardDto> result = courseService.retrieveCourseByemail(creatorId);

            // Assert
            assertNotNull(result, "Result should not be null");
            assertEquals(courses.size(), result.size(), "Result should contain all courses");

            for (int i = 0; i < result.size(); i++) {
                assertEquals(dtos.get(i).getId(), result.get(i).getId(),
                        "Course ID at index " + i + " should match");
                assertEquals(dtos.get(i).getName(), result.get(i).getName(),
                        "Course name at index " + i + " should match");
            }

            verify(creatorRepository, times(1)).findById(creatorId);
            mockedTransform.verify(() -> CourseTransForm.transformToCourseCardDto(any(Course.class)),
                    times(courses.size()));
        }
    }
}

