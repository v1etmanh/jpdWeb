package com.jpd.web.nguyen.unit.service.courseInf;

import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;
import com.jpd.web.model.Language;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.service.CourseInfService;
import com.jpd.web.dto.CourseInfDto;
import com.jpd.web.transform.CourseTransForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test bổ sung cho getRecommendCourses() với các scenarios phức tạp hơn
 * 
 * Bổ sung thêm:
 * - Test với multiple feedbacks per course
 * - Test performance với large dataset 
 * - Test boundary conditions
 * - Parameterized tests
 * - Test data consistency
 */
class getRecommendCoursesImprovedTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CourseInfService courseInfService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ==========================================================
    // ================= ENHANCED TEST CASES ===================
    // ==========================================================

    /**
     * Test với multiple feedbacks per course để đảm bảo avg rating tính đúng
     */
    @Test
    void shouldCalculateCorrectAvgRating_whenCourseHasMultipleFeedbacks() {
        // =========================
        // Given
        // =========================
        Language lang = mock(Language.class);
        Course course = mock(Course.class);
        when(course.isPublic()).thenReturn(true);

        when(courseRepository.findDistinctLanguages())
                .thenReturn(List.of(lang));
        when(courseRepository.findByLanguage(lang))
                .thenReturn(List.of(course));

        // Tạo enrollment với nhiều feedbacks khác nhau
        List<Enrollment> enrollments = buildEnrollmentsWithMultipleFeedbacks(
                List.of(50, 40, 30, 20, 10)); // avg = 30.0
        when(enrollmentRepository.findByCourse(course))
                .thenReturn(enrollments);

        CourseInfDto expectedDto = new CourseInfDto();

        try (MockedStatic<CourseTransForm> mockedStatic = Mockito.mockStatic(CourseTransForm.class)) {
            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseInfDto(course, 5, 30.0)
            ).thenReturn(expectedDto);

            // =========================
            // When
            // =========================
            List<CourseInfDto> result = courseInfService.getRecommendCourses();

            // =========================
            // Then
            // =========================
            assertNotNull(result);
            assertEquals(1, result.size());
            assertSame(expectedDto, result.get(0));

            // Verify mapper được gọi với avg rating đúng
            mockedStatic.verify(() ->
                    CourseTransForm.transformToCourseInfDto(course, 5, 30.0),
                    times(1));
        }
    }

    /**
     * Test với courses có 0 enrollment
     */
    @Test
    void shouldHandleCoursesWithZeroEnrollments() {
        // =========================
        // Given
        // =========================
        Language lang = mock(Language.class);
        Course course = mock(Course.class);
        when(course.isPublic()).thenReturn(true);

        when(courseRepository.findDistinctLanguages())
                .thenReturn(List.of(lang));
        when(courseRepository.findByLanguage(lang))
                .thenReturn(List.of(course));

        // Empty enrollment list
        when(enrollmentRepository.findByCourse(course))
                .thenReturn(List.of());

        CourseInfDto expectedDto = new CourseInfDto();

        try (MockedStatic<CourseTransForm> mockedStatic = Mockito.mockStatic(CourseTransForm.class)) {
            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseInfDto(course, 0, 0.0)
            ).thenReturn(expectedDto);

            // =========================
            // When
            // =========================
            List<CourseInfDto> result = courseInfService.getRecommendCourses();

            // =========================
            // Then
            // =========================
            assertNotNull(result);
            assertEquals(1, result.size());
            assertSame(expectedDto, result.get(0));
        }
    }

    /**
     * Parameterized test cho different course counts
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 5, 10})
    void shouldReturnCorrectLimit_forDifferentCourseCounts(int courseCount) {
        // =========================
        // Given
        // =========================
        Language lang = mock(Language.class);
        when(courseRepository.findDistinctLanguages())
                .thenReturn(List.of(lang));

        List<Course> courses = IntStream.range(0, courseCount)
                .mapToObj(i -> {
                    Course course = mock(Course.class);
                    when(course.isPublic()).thenReturn(true);
                    return course;
                })
                .toList();

        when(courseRepository.findByLanguage(lang))
                .thenReturn(courses);

        // Mock enrollment cho tất cả courses
        for (int i = 0; i < courseCount; i++) {
            Course course = courses.get(i);
            when(enrollmentRepository.findByCourse(course))
                    .thenReturn(buildSimpleEnrollments(10, 45));
        }

        try (MockedStatic<CourseTransForm> mockedStatic = Mockito.mockStatic(CourseTransForm.class)) {
            // Mock transform cho tất cả courses
            for (int i = 0; i < courseCount; i++) {
                Course course = courses.get(i);
                CourseInfDto dto = new CourseInfDto();
                mockedStatic.when(() ->
                        CourseTransForm.transformToCourseInfDto(course, 10, 45.0)
                ).thenReturn(dto);
            }

            // =========================
            // When
            // =========================
            List<CourseInfDto> result = courseInfService.getRecommendCourses();

            // =========================
            // Then
            // =========================
            int expectedSize = Math.min(3, courseCount); // Top 3 hoặc ít hơn
            assertNotNull(result);
            assertEquals(expectedSize, result.size(),
                    String.format("Với %d courses, kết quả phải có %d items", courseCount, expectedSize));
        }
    }

    /**
     * Test boundary conditions cho weight calculation
     */
    @Test
    void shouldCalculateWeightCorrectly_forBoundaryValues() {
        // =========================
        // Given
        // =========================
        Language lang = mock(Language.class);
        
        // Course với rating = 0, students = 0
        Course courseMin = mock(Course.class);
        when(courseMin.isPublic()).thenReturn(true);
        
        // Course với rating cao, students nhiều
        Course courseMax = mock(Course.class);
        when(courseMax.isPublic()).thenReturn(true);

        when(courseRepository.findDistinctLanguages())
                .thenReturn(List.of(lang));
        when(courseRepository.findByLanguage(lang))
                .thenReturn(List.of(courseMin, courseMax));

        // Boundary values
        when(enrollmentRepository.findByCourse(courseMin))
                .thenReturn(List.of()); // 0 students, 0 rating
        when(enrollmentRepository.findByCourse(courseMax))
                .thenReturn(buildSimpleEnrollments(1000, 50)); // Max values

        CourseInfDto dtoMin = new CourseInfDto();
        CourseInfDto dtoMax = new CourseInfDto();

        try (MockedStatic<CourseTransForm> mockedStatic = Mockito.mockStatic(CourseTransForm.class)) {
            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseInfDto(courseMin, 0, 0.0)
            ).thenReturn(dtoMin);
            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseInfDto(courseMax, 1000, 50.0)
            ).thenReturn(dtoMax);

            // =========================
            // When
            // =========================
            List<CourseInfDto> result = courseInfService.getRecommendCourses();

            // =========================
            // Then
            // =========================
            assertNotNull(result);
            assertEquals(2, result.size());
            
            // Course với weight cao hơn phải đứng đầu
            // Weight của courseMax = 0.7 * 50.0 + 0.3 * log10(1000 + 1) ≈ 35.9
            // Weight của courseMin = 0.7 * 0.0 + 0.3 * log10(0 + 1) = 0.0
            assertSame(dtoMax, result.get(0), "Course với weight cao hơn phải đứng đầu");
            assertSame(dtoMin, result.get(1), "Course với weight thấp hơn phải đứng sau");
        }
    }

    /**
     * Test consistency - gọi nhiều lần phải cho kết quả giống nhau
     */
    @Test
    void shouldMaintainConsistentOrdering_acrossMultipleCalls() {
        // =========================
        // Given
        // =========================
        Language lang = mock(Language.class);
        
        Course course1 = mock(Course.class);
        Course course2 = mock(Course.class);
        Course course3 = mock(Course.class);
        
        when(course1.isPublic()).thenReturn(true);
        when(course2.isPublic()).thenReturn(true);
        when(course3.isPublic()).thenReturn(true);

        when(courseRepository.findDistinctLanguages())
                .thenReturn(List.of(lang));
        when(courseRepository.findByLanguage(lang))
                .thenReturn(List.of(course1, course2, course3));

        // Mock với weights khác nhau
        when(enrollmentRepository.findByCourse(course1))
                .thenReturn(buildSimpleEnrollments(100, 40));
        when(enrollmentRepository.findByCourse(course2))
                .thenReturn(buildSimpleEnrollments(200, 45));
        when(enrollmentRepository.findByCourse(course3))
                .thenReturn(buildSimpleEnrollments(150, 42));

        CourseInfDto dto1 = new CourseInfDto();
        CourseInfDto dto2 = new CourseInfDto();
        CourseInfDto dto3 = new CourseInfDto();

        try (MockedStatic<CourseTransForm> mockedStatic = Mockito.mockStatic(CourseTransForm.class)) {
            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseInfDto(course1, 100, 40.0)
            ).thenReturn(dto1);
            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseInfDto(course2, 200, 45.0)
            ).thenReturn(dto2);
            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseInfDto(course3, 150, 42.0)
            ).thenReturn(dto3);

            // =========================
            // When - gọi nhiều lần
            // =========================
            List<CourseInfDto> result1 = courseInfService.getRecommendCourses();
            List<CourseInfDto> result2 = courseInfService.getRecommendCourses();
            List<CourseInfDto> result3 = courseInfService.getRecommendCourses();

            // =========================
            // Then
            // =========================
            assertNotNull(result1);
            assertNotNull(result2);
            assertNotNull(result3);
            
            assertEquals(result1.size(), result2.size());
            assertEquals(result1.size(), result3.size());
            
            // Kiểm tra thứ tự giống nhau
            for (int i = 0; i < result1.size(); i++) {
                assertSame(result1.get(i), result2.get(i), 
                    String.format("Element %d phải giống nhau giữa lần gọi 1 và 2", i));
                assertSame(result1.get(i), result3.get(i), 
                    String.format("Element %d phải giống nhau giữa lần gọi 1 và 3", i));
            }
        }
    }

    // ==========================================================
    // =================== HELPER METHODS ======================
    // ==========================================================

    /**
     * Tạo enrollment list với multiple feedbacks
     */
    private List<Enrollment> buildEnrollmentsWithMultipleFeedbacks(List<Integer> feedbackRates) {
        List<Enrollment> enrollments = new ArrayList<>();
        
        for (Integer rate : feedbackRates) {
            Enrollment enrollment = new Enrollment();
            Feedback feedback = new Feedback();
            feedback.setRate(rate);
            enrollment.setFeedback(feedback);
            enrollments.add(enrollment);
        }
        
        return enrollments;
    }

    /**
     * Tạo simple enrollment list với 1 feedback
     */
    private List<Enrollment> buildSimpleEnrollments(int numStudents, int feedbackRate) {
        List<Enrollment> list = new ArrayList<>();
        for (int i = 0; i < numStudents; i++) {
            Enrollment e = new Enrollment();
            if (i == 0) {
                Feedback fb = new Feedback();
                fb.setRate(feedbackRate);
                e.setFeedback(fb);
            } else {
                e.setFeedback(null);
            }
            list.add(e);
        }
        return list;
    }
}