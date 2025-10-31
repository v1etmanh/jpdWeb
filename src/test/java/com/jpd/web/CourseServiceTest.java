package com.jpd.web;


import com.jpd.web.exception.*;
import com.jpd.web.model.*;
import com.jpd.web.repository.*;
import com.jpd.web.service.utils.ValidationResources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationResourcesTest {

    @Mock
    private CreatorRepository creatorRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private ValidationResources validationResources;

    @DisplayName("Data-driven test for validateCourseOwnership")
    @ParameterizedTest(name = "courseId={0}, creatorId={1}, expectedOutcome={2}")
    @CsvSource({
            "1, 10, SUCCESS",
            "2, 99, CREATOR_NOT_FOUND",
            "3, 20, COURSE_NOT_FOUND",
            "4, 40, UNAUTHORIZED"
    })
    void testValidateCourseOwnership(Long courseId, Long creatorId, String expectedOutcome) {
        // Mock Creator and Course objects
        Creator creator = new Creator();
        creator.setCreatorId(creatorId);

        Course course = new Course();
        Creator owner = new Creator();
        owner.setCreatorId(creatorId);
        course.setCreator(owner);

        // Mock behaviors based on scenario
        switch (expectedOutcome) {
            case "SUCCESS":
                when(creatorRepository.findById(creatorId)).thenReturn(Optional.of(creator));
                when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
                assertDoesNotThrow(() -> {
                    Course result = validationResources.validateCourseOwnership(courseId, creatorId);
                    assertEquals(course, result);
                });
                break;

            case "CREATOR_NOT_FOUND":
                when(creatorRepository.findById(creatorId)).thenReturn(Optional.empty());
                assertThrows(CreatorNotFoundException.class,
                        () -> validationResources.validateCourseOwnership(courseId, creatorId));
                break;

            case "COURSE_NOT_FOUND":
                when(creatorRepository.findById(creatorId)).thenReturn(Optional.of(creator));
                when(courseRepository.findById(courseId)).thenReturn(Optional.empty());
                assertThrows(CourseNotFoundException.class,
                        () -> validationResources.validateCourseOwnership(courseId, creatorId));
                break;

            case "UNAUTHORIZED":
                when(creatorRepository.findById(creatorId)).thenReturn(Optional.of(creator));
                Creator anotherOwner = new Creator();
                anotherOwner.setCreatorId(999L); // different ID
                course.setCreator(anotherOwner);
                when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
                assertThrows(UnauthorizedException.class,
                        () -> validationResources.validateCourseOwnership(courseId, creatorId));
                break;
        }
    }
}
