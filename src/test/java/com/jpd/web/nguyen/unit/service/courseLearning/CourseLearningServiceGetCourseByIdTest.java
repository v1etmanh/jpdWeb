package com.jpd.web.nguyen.unit.service.courseLearning;

import com.jpd.web.model.Course;
import com.jpd.web.model.Chapter;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.service.CourseLearningService;
import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.CourseTransForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseLearningServiceGetCourseByIdTest {

    @Mock
    private ValidationResources validationResources;

    @Mock
    private ModuleContentRepository moduleContentRepository;

    @Mock
    private com.jpd.web.controller.creator.CourseController courseController;

    @InjectMocks
    private CourseLearningService courseLearningService;

    @BeforeEach
    void init() {
        // Inject các @Autowired dependencies bằng ReflectionTestUtils
        // vì @InjectMocks không handle @Autowired fields
        ReflectionTestUtils.setField(courseLearningService, "validationResources", validationResources);
        ReflectionTestUtils.setField(courseLearningService, "moduleContentRepository", moduleContentRepository);
    }

    @Test
    void shouldThrowException_whenUserDoesNotHaveAccessToCourse() {
        // Given
        long courseId = 999L;
        String email = "hacker@example.com";

        // validationResources sẽ ném lỗi phân quyền
        when(validationResources.validateCustomerWithCourse(email, courseId))
                .thenThrow(new UnauthorizedException("not your course"));

        // When / Then
        assertThrows(
                UnauthorizedException.class,
                () -> courseLearningService.getCourseById(courseId, email),
                "Nếu user không có quyền truy cập, service phải ném UnauthorizedException"
        );

        // verify chúng ta đã gọi đúng validation
        verify(validationResources, times(1))
                .validateCustomerWithCourse(email, courseId);

        // Không đi xa tới mapper/static
        verifyNoInteractions(moduleContentRepository);
    }

    @Test
    void shouldThrowUnauthorizedException_whenCourseIsNotPublic() {
        // Given
        long courseId = 456L;
        String email = "student@example.com";

        Course privateCourse = mock(Course.class);
        when(validationResources.validateCustomerWithCourse(email, courseId))
                .thenReturn(privateCourse);
        when(privateCourse.isPublic()).thenReturn(false);

        // When / Then
        assertThrows(
                UnauthorizedException.class,
                () -> courseLearningService.getCourseById(courseId, email),
                "Nếu course không public, service phải ném UnauthorizedException(\"this course is not exist\")"
        );

        verify(validationResources).validateCustomerWithCourse(email, courseId);
    }

    @Test
    void shouldReturnCourseContentDto_whenUserHasAccessAndCourseIsPublic() {
        // Given
        long courseId = 123L;
        String email = "student@example.com";

        Course courseMock = mock(Course.class);
        Chapter chapterMock = mock(Chapter.class);

        when(validationResources.validateCustomerWithCourse(email, courseId))
                .thenReturn(courseMock);
        when(courseMock.isPublic()).thenReturn(true);

        when(courseMock.getChapters()).thenReturn(List.of(chapterMock));
        when(chapterMock.getModules()).thenReturn(List.of()); // tránh NPE trong forEach

        CourseContentDto dtoMock = new CourseContentDto();

        try (MockedStatic<CourseTransForm> mockedStatic
                     = Mockito.mockStatic(CourseTransForm.class)) {

            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseContentDto(courseMock)
            ).thenReturn(dtoMock);

            // When
            CourseContentDto result = courseLearningService.getCourseById(courseId, email);

            // Then
            assertNotNull(result);
            assertSame(dtoMock, result);

            verify(validationResources).validateCustomerWithCourse(email, courseId);
            mockedStatic.verify(
                    () -> CourseTransForm.transformToCourseContentDto(courseMock),
                    times(1)
            );
        }
    }

    @Test
    void shouldReturnDto_whenCourseHasNoChapters() {
        // Given
        long courseId = 101L;
        String email = "student@example.com";

        Course courseMock = mock(Course.class);

        when(validationResources.validateCustomerWithCourse(email, courseId))
                .thenReturn(courseMock);
        when(courseMock.isPublic()).thenReturn(true);

        // Edge case: không có chapter
        when(courseMock.getChapters()).thenReturn(Collections.emptyList());

        CourseContentDto dtoMock = new CourseContentDto();

        try (MockedStatic<CourseTransForm> mockedStatic
                     = Mockito.mockStatic(CourseTransForm.class)) {

            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseContentDto(courseMock)
            ).thenReturn(dtoMock);

            // When
            CourseContentDto result = courseLearningService.getCourseById(courseId, email);

            // Then
            assertNotNull(result);
            assertSame(dtoMock, result);

            verify(validationResources).validateCustomerWithCourse(email, courseId);
            mockedStatic.verify(
                    () -> CourseTransForm.transformToCourseContentDto(courseMock),
                    times(1)
            );
        }
    }
}
