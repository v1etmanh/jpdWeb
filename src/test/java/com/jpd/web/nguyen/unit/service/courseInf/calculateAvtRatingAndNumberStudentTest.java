package com.jpd.web.nguyen.unit.service.courseInf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.jpd.web.service.CourseInfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CustomerModuleContentRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.service.utils.ValidationResources;

/**
 * Unit tests cho private method calculateAvtRatingAndNumberStudent trong CourseInfService.
 * Bao phủ:
 *  - Happy Paths
 *  - Edge Cases
 *  - Error Scenarios
 *
 * Cách test:
 *  - Mock EnrollmentRepository
 *  - Gọi method private bằng Reflection
 *  - Đọc kết quả record private RatingInfo bằng Reflection
 */
class calculateAvtRatingAndNumberStudentTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ValidationResources validationResources;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CreatorRepository creatorRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerModuleContentRepository contentRepository;

    @InjectMocks
    private CourseInfService courseInfService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // =====================================================================================
    // Helpers dùng lại trong các test
    // =====================================================================================

    // Gọi method private calculateAvtRatingAndNumberStudent(course) qua Reflection
    private Object invokeCalculateAvtRatingAndNumberStudent(Course course) throws Exception {
        Method method = CourseInfService.class.getDeclaredMethod(
                "calculateAvtRatingAndNumberStudent",
                com.jpd.web.model.Course.class
        );
        method.setAccessible(true);
        return method.invoke(courseInfService, course);
    }

    // Lấy avgRating từ private record RatingInfo bằng Reflection
    private double extractAvgRating(Object ratingInfoObj) throws Exception {
        Class<?> ratingInfoClass = ratingInfoObj.getClass();
        Field avgRatingField = ratingInfoClass.getDeclaredField("avgRating");
        avgRatingField.setAccessible(true);
        return (double) avgRatingField.get(ratingInfoObj);
    }

    // Lấy numStudent từ private record RatingInfo bằng Reflection
    private int extractNumStudent(Object ratingInfoObj) throws Exception {
        Class<?> ratingInfoClass = ratingInfoObj.getClass();
        Field numStudentField = ratingInfoClass.getDeclaredField("numStudent");
        numStudentField.setAccessible(true);
        return (int) numStudentField.get(ratingInfoObj);
    }

    // Tạo Enrollment có Feedback(rate = r)
    private Enrollment enrollmentWithFeedback(int r) {
        Enrollment e = new Enrollment();
        Feedback f = new Feedback();
        f.setRate(r);
        e.setFeedback(f);
        return e;
    }

    // Tạo Enrollment không có Feedback (feedback = null)
    private Enrollment enrollmentWithoutFeedback() {
        Enrollment e = new Enrollment();
        e.setFeedback(null);
        return e;
    }

    // =====================================================================================
    // HAPPY PATHS
    // =====================================================================================

    /**
     * TC_HP_01:
     * Tất cả enrollment đều có feedback.
     *
     * Input:
     *  - feedback rates: 4.0, 5.0, 3.0
     * Expect:
     *  - avgRating = (4.0 + 5.0 + 3.0) / 3 = 4.0
     *  - numStudent = 3
     */
    @Test
    void shouldCalculateCorrectAverageAndStudentCount_whenAllEnrollmentsHaveFeedback() throws Exception {
        // =========================
        // Given
        // =========================
        Course course = new Course();

        List<Enrollment> enrollments = Arrays.asList(
                enrollmentWithFeedback(4),
                enrollmentWithFeedback(5),
                enrollmentWithFeedback(3)
        );

        when(enrollmentRepository.findByCourse(any(Course.class)))
                .thenReturn(enrollments);

        // =========================
        // When
        // =========================
        Object ratingInfoObj = invokeCalculateAvtRatingAndNumberStudent(course);
        double actualAvgRating = extractAvgRating(ratingInfoObj);
        int actualNumStudent = extractNumStudent(ratingInfoObj);

        // =========================
        // Then
        // =========================
        assertEquals(4.0, actualAvgRating, 0.0001,
                "avgRating phải bằng 4.0 khi feedback = {4,5,3}");
        assertEquals(3, actualNumStudent,
                "numStudent phải bằng 3 khi có 3 enrollments");
    }

    /**
     * TC_HP_02:
     * Có cả feedback và không feedback.
     *
     * Input:
     *  - feedback rates: 5.0, 4.0, null, null
     * Expect:
     *  - avgRating = (5.0 + 4.0) / 2 = 4.5
     *  - numStudent = 4 (tất cả enrollment, kể cả người chưa feedback)
     */
    @Test
    void shouldCalculateCorrectAverageAndStudentCount_whenSomeEnrollmentsHaveNoFeedback() throws Exception {
        // =========================
        // Given
        // =========================
        Course course = new Course();

        List<Enrollment> enrollments = Arrays.asList(
                enrollmentWithFeedback(5),
                enrollmentWithFeedback(4),
                enrollmentWithoutFeedback(),
                enrollmentWithoutFeedback()
        );

        when(enrollmentRepository.findByCourse(any(Course.class)))
                .thenReturn(enrollments);

        // =========================
        // When
        // =========================
        Object ratingInfoObj = invokeCalculateAvtRatingAndNumberStudent(course);
        double actualAvgRating = extractAvgRating(ratingInfoObj);
        int actualNumStudent = extractNumStudent(ratingInfoObj);

        // =========================
        // Then
        // =========================
        assertEquals(4.5, actualAvgRating, 0.0001,
                "avgRating phải bằng 4.5 khi chỉ 2/4 enrollment có feedback {5.0,4.0}");
        assertEquals(4, actualNumStudent,
                "numStudent phải bằng 4 (tổng enrollment), kể cả người chưa feedback");
    }

    /**
     * TC_HP_03:
     * Chỉ 1 học viên và người đó có feedback.
     *
     * Input:
     *  - feedback rate: 5.0
     * Expect:
     *  - avgRating = 5.0
     *  - numStudent = 1
     */
    @Test
    void shouldCalculateCorrectAverageAndStudentCount_whenSingleEnrollmentWithFeedback() throws Exception {
        // =========================
        // Given
        // =========================
        Course course = new Course();

        List<Enrollment> enrollments = Collections.singletonList(
                enrollmentWithFeedback(5)
        );

        when(enrollmentRepository.findByCourse(any(Course.class)))
                .thenReturn(enrollments);

        // =========================
        // When
        // =========================
        Object ratingInfoObj = invokeCalculateAvtRatingAndNumberStudent(course);
        double actualAvgRating = extractAvgRating(ratingInfoObj);
        int actualNumStudent = extractNumStudent(ratingInfoObj);

        // =========================
        // Then
        // =========================
        assertEquals(5.0, actualAvgRating, 0.0001,
                "avgRating phải là 5.0 khi chỉ có đúng 1 feedback = 5.0");
        assertEquals(1, actualNumStudent,
                "numStudent phải bằng 1 khi chỉ có 1 enrollment");
    }

    // =====================================================================================
    // EDGE CASES
    // =====================================================================================

    /**
     * TC_EC_01:
     * Không có enrollment nào.
     *
     * Expect:
     *  - avgRating = 0
     *  - numStudent = 0
     *
     * (Case này giống test đã viết trước đó:
     * shouldReturnZeroRatingAndZeroStudent_whenEnrollmentListIsEmpty)
     * Vẫn giữ lại ở đây để class này tự đủ kịch bản.
     */
    @Test
    void shouldReturnZeroRatingAndZeroStudent_whenEnrollmentListIsEmpty() throws Exception {
        // =========================
        // Given
        // =========================
        Course course = new Course();

        when(enrollmentRepository.findByCourse(any(Course.class)))
                .thenReturn(Collections.emptyList());

        // =========================
        // When
        // =========================
        Object ratingInfoObj = invokeCalculateAvtRatingAndNumberStudent(course);
        double actualAvgRating = extractAvgRating(ratingInfoObj);
        int actualNumStudent = extractNumStudent(ratingInfoObj);

        // =========================
        // Then
        // =========================
        assertEquals(0.0, actualAvgRating, 0.0001,
                "avgRating phải bằng 0.0 khi enrollment rỗng");
        assertEquals(0, actualNumStudent,
                "numStudent phải bằng 0 khi không có enrollment nào");
    }

    /**
     * TC_EC_02:
     * Có enrollment nhưng không ai feedback (feedback = null hết).
     *
     * Expect:
     *  - avgRating = 0 (vì total = 0 => branch avgRating = 0)
     *  - numStudent = số enrollment
     */
    @Test
    void shouldReturnZeroRatingButCountStudents_whenNoEnrollmentHasFeedback() throws Exception {
        // =========================
        // Given
        // =========================
        Course course = new Course();

        List<Enrollment> enrollments = Arrays.asList(
                enrollmentWithoutFeedback(),
                enrollmentWithoutFeedback()
        );

        when(enrollmentRepository.findByCourse(any(Course.class)))
                .thenReturn(enrollments);

        // =========================
        // When
        // =========================
        Object ratingInfoObj = invokeCalculateAvtRatingAndNumberStudent(course);
        double actualAvgRating = extractAvgRating(ratingInfoObj);
        int actualNumStudent = extractNumStudent(ratingInfoObj);

        // =========================
        // Then
        // =========================
        assertEquals(0.0, actualAvgRating, 0.0001,
                "avgRating phải bằng 0.0 khi không có feedback nào (total=0)");
        assertEquals(2, actualNumStudent,
                "numStudent phải bằng tổng enrollment (2) ngay cả khi không ai feedback");
    }

    /**
     * TC_EC_03:
     * Có feedback nhưng tất cả rate = 0.
     *
     * Expect:
     *  - avgRating = 0.0
     *  - numStudent = số enrollment
     */
    @Test
    void shouldReturnZeroAverageRating_whenAllFeedbackRatesAreZero() throws Exception {
        // =========================
        // Given
        // =========================
        Course course = new Course();

        List<Enrollment> enrollments = Arrays.asList(
                enrollmentWithFeedback(0),
                enrollmentWithFeedback(0)
        );

        when(enrollmentRepository.findByCourse(any(Course.class)))
                .thenReturn(enrollments);

        // =========================
        // When
        // =========================
        Object ratingInfoObj = invokeCalculateAvtRatingAndNumberStudent(course);
        double actualAvgRating = extractAvgRating(ratingInfoObj);
        int actualNumStudent = extractNumStudent(ratingInfoObj);

        // =========================
        // Then
        // =========================
        assertEquals(0.0, actualAvgRating, 0.0001,
                "avgRating phải bằng 0.0 khi tất cả feedback rate đều là 0");
        assertEquals(2, actualNumStudent,
                "numStudent phải bằng 2 vì có 2 enrollment");
    }

    // =====================================================================================
    // ERROR SCENARIOS
    // =====================================================================================

    /**
     * TC_ES_01:
     * Repository trả về null thay vì List.
     *
     * Hành vi hiện tại của method:
     *  - Gọi enrollments.isEmpty()
     *  - Nếu enrollments == null -> NullPointerException
     *
     * Kỳ vọng:
     *  - Ta coi đây là lỗi, và xác nhận rằng lỗi ném ra (bubble lên).
     *
     * Lưu ý kỹ thuật:
     *  - Gọi method private qua reflection -> InvocationTargetException
     *  - Nguyên nhân thực sự nằm trong ex.getCause()
     *  - Ta assert là cause instanceof NullPointerException
     */
    @Test
    void shouldThrowNullPointerException_whenRepositoryReturnsNull() throws Exception {
        // =========================
        // Given
        // =========================
        Course course = new Course();

        when(enrollmentRepository.findByCourse(any(Course.class)))
                .thenReturn(null); // <-- lỗi: repository không nên trả null

        // =========================
        // When / Then
        // =========================
        try {
            invokeCalculateAvtRatingAndNumberStudent(course);
            fail("Kỳ vọng NullPointerException nhưng không ném ra");
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause instanceof NullPointerException,
                    "Kỳ vọng cause là NullPointerException khi repository trả về null");
        }
    }

    /**
     * TC_ES_02:
     * Repository ném RuntimeException (VD: DB down).
     *
     * Kỳ vọng:
     *  - Method không swallow mà để exception bubble lên.
     *  - Vì gọi qua reflection, exception thực sẽ ở InvocationTargetException.getCause()
     */
    @Test
    void shouldPropagateRuntimeException_whenRepositoryThrowsUnexpectedError() throws Exception {
        // =========================
        // Given
        // =========================
        Course course = new Course();

        RuntimeException boom = new RuntimeException("DB down");
        when(enrollmentRepository.findByCourse(any(Course.class)))
                .thenThrow(boom);

        // =========================
        // When / Then
        // =========================
        try {
            invokeCalculateAvtRatingAndNumberStudent(course);
            fail("Kỳ vọng RuntimeException nhưng không ném ra");
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause instanceof RuntimeException,
                    "Kỳ vọng cause là RuntimeException khi repository ném lỗi RuntimeException");
            assertEquals("DB down", cause.getMessage(),
                    "Thông báo lỗi phải khớp với RuntimeException gốc");
        }
    }
}
