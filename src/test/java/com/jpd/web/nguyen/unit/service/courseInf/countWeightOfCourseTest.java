package com.jpd.web.nguyen.unit.service.courseInf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import com.jpd.web.service.CourseInfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CustomerModuleContentRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.service.utils.ValidationResources;

/**
 * Unit test cho private method countWeightOfCourse trong CourseInfService.
 * Mỗi test dùng Reflection để gọi method private.
 */
class countWeightOfCourseTest {

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
    // Test gốc đã viết trước đó (trường hợp "bình thường" ví dụ rating 4.5, students 100)
    // =====================================================================================
    @Test
    void shouldCalculateWeightCorrectly_whenGivenNormalInputs() throws Exception {
        // =========================
        // Given
        // =========================
        double avgRating = 4.5;
        int numberStudent = 100;

        double normalizedStudent = Math.log10(numberStudent + 1); // log10(101)
        double expectedWeight = 0.7 * avgRating + 0.3 * normalizedStudent;

        Method privateMethod = CourseInfService.class.getDeclaredMethod(
                "countWeightOfCourse",
                double.class,
                int.class
        );
        privateMethod.setAccessible(true);

        // =========================
        // When
        // =========================
        Object result = privateMethod.invoke(courseInfService, avgRating, numberStudent);
        double actualWeight = (double) result;

        // =========================
        // Then
        // =========================
        double delta = 0.001;
        assertEquals(expectedWeight, actualWeight, delta,
                "Weight không khớp công thức kỳ vọng cho trường hợp normal inputs");
    }

    // =====================================================================================
    // HAPPY PATHS
    // =====================================================================================

    /**
     * TC_HP_01:
     * avgRating = 4.8, numberStudent = 1200
     * Khóa học nổi bật: rating cao + rất đông học viên.
     */
    @Test
    void shouldCalculateWeightCorrectly_whenHighRatingAndManyStudents() throws Exception {
        // Given
        double avgRating = 4.8;
        int numberStudent = 1200;

        double normalizedStudent = Math.log10(numberStudent + 1); // log10(1201)
        double expectedWeight = 0.7 * avgRating + 0.3 * normalizedStudent;

        Method privateMethod = CourseInfService.class.getDeclaredMethod(
                "countWeightOfCourse",
                double.class,
                int.class
        );
        privateMethod.setAccessible(true);

        // When
        Object result = privateMethod.invoke(courseInfService, avgRating, numberStudent);
        double actualWeight = (double) result;

        // Then
        double delta = 0.001;
        assertEquals(expectedWeight, actualWeight, delta,
                "Weight không đúng cho trường hợp rating cao và nhiều học viên");
    }

    /**
     * TC_HP_02:
     * avgRating = 3.5, numberStudent = 100
     * Khóa học trung bình hợp lệ.
     */
    @Test
    void shouldCalculateWeightCorrectly_whenAverageRatingAndMediumStudents() throws Exception {
        // Given
        double avgRating = 3.5;
        int numberStudent = 100;

        double normalizedStudent = Math.log10(numberStudent + 1); // log10(101)
        double expectedWeight = 0.7 * avgRating + 0.3 * normalizedStudent;

        Method privateMethod = CourseInfService.class.getDeclaredMethod(
                "countWeightOfCourse",
                double.class,
                int.class
        );
        privateMethod.setAccessible(true);

        // When
        Object result = privateMethod.invoke(courseInfService, avgRating, numberStudent);
        double actualWeight = (double) result;

        // Then
        double delta = 0.001;
        assertEquals(expectedWeight, actualWeight, delta,
                "Weight không đúng cho trường hợp rating trung bình và số học viên trung bình");
    }

    /**
     * TC_HP_03:
     * avgRating = 2.0, numberStudent = 5000
     * rating thấp nhưng đông học viên.
     */
    @Test
    void shouldCalculateWeightCorrectly_whenLowRatingButManyStudents() throws Exception {
        // Given
        double avgRating = 2.0;
        int numberStudent = 5000;

        double normalizedStudent = Math.log10(numberStudent + 1); // log10(5001)
        double expectedWeight = 0.7 * avgRating + 0.3 * normalizedStudent;

        Method privateMethod = CourseInfService.class.getDeclaredMethod(
                "countWeightOfCourse",
                double.class,
                int.class
        );
        privateMethod.setAccessible(true);

        // When
        Object result = privateMethod.invoke(courseInfService, avgRating, numberStudent);
        double actualWeight = (double) result;

        // Then
        double delta = 0.001;
        assertEquals(expectedWeight, actualWeight, delta,
                "Weight không đúng cho trường hợp rating thấp nhưng học viên rất đông");
    }

    // =====================================================================================
    // EDGE CASES
    // =====================================================================================

    /**
     * TC_EC_01:
     * avgRating = 4.5, numberStudent = 0
     * Khóa học mới, kiểm tra log10(1)=0.
     * Kỳ vọng: weight = 0.7 * 4.5 + 0.3 * 0 = 3.15
     */
    @Test
    void shouldCalculateWeightCorrectly_whenZeroStudents() throws Exception {
        // Given
        double avgRating = 4.5;
        int numberStudent = 0;

        double normalizedStudent = Math.log10(numberStudent + 1); // log10(1) = 0
        double expectedWeight = 0.7 * avgRating + 0.3 * normalizedStudent;

        Method privateMethod = CourseInfService.class.getDeclaredMethod(
                "countWeightOfCourse",
                double.class,
                int.class
        );
        privateMethod.setAccessible(true);

        // When
        Object result = privateMethod.invoke(courseInfService, avgRating, numberStudent);
        double actualWeight = (double) result;

        // Then
        double delta = 0.001;
        assertEquals(expectedWeight, actualWeight, delta,
                "Weight không đúng khi số học viên = 0");
    }

    /**
     * TC_EC_02:
     * avgRating = 0.0, numberStudent = 300
     * Không có rating (hoặc rating = 0) nhưng có học viên.
     */
    @Test
    void shouldCalculateWeightCorrectly_whenZeroRatingButHasStudents() throws Exception {
        // Given
        double avgRating = 0.0;
        int numberStudent = 300;

        double normalizedStudent = Math.log10(numberStudent + 1); // log10(301)
        double expectedWeight = 0.7 * avgRating + 0.3 * normalizedStudent;

        Method privateMethod = CourseInfService.class.getDeclaredMethod(
                "countWeightOfCourse",
                double.class,
                int.class
        );
        privateMethod.setAccessible(true);

        // When
        Object result = privateMethod.invoke(courseInfService, avgRating, numberStudent);
        double actualWeight = (double) result;

        // Then
        double delta = 0.001;
        assertEquals(expectedWeight, actualWeight, delta,
                "Weight không đúng khi rating = 0 nhưng vẫn có học viên");
    }

    /**
     * TC_EC_03:
     * avgRating = 5.0, numberStudent = 1
     * Rating tối đa, rất ít học viên.
     */
    @Test
    void shouldCalculateWeightCorrectly_whenMaxRatingAndFewStudents() throws Exception {
        // Given
        double avgRating = 5.0;
        int numberStudent = 1;

        double normalizedStudent = Math.log10(numberStudent + 1); // log10(2)
        double expectedWeight = 0.7 * avgRating + 0.3 * normalizedStudent;

        Method privateMethod = CourseInfService.class.getDeclaredMethod(
                "countWeightOfCourse",
                double.class,
                int.class
        );
        privateMethod.setAccessible(true);

        // When
        Object result = privateMethod.invoke(courseInfService, avgRating, numberStudent);
        double actualWeight = (double) result;

        // Then
        double delta = 0.001;
        assertEquals(expectedWeight, actualWeight, delta,
                "Weight không đúng khi rating tối đa và rất ít học viên");
    }

    /**
     * TC_EC_04:
     * avgRating = 4.2, numberStudent = 1_000_000
     * Stress test với số học viên cực lớn.
     */
    @Test
    void shouldCalculateWeightCorrectly_whenVeryLargeStudentCount() throws Exception {
        // Given
        double avgRating = 4.2;
        int numberStudent = 1_000_000;

        double normalizedStudent = Math.log10(numberStudent + 1); // log10(1_000_001)
        double expectedWeight = 0.7 * avgRating + 0.3 * normalizedStudent;

        Method privateMethod = CourseInfService.class.getDeclaredMethod(
                "countWeightOfCourse",
                double.class,
                int.class
        );
        privateMethod.setAccessible(true);

        // When
        Object result = privateMethod.invoke(courseInfService, avgRating, numberStudent);
        double actualWeight = (double) result;

        // Then
        double delta = 0.001;
        assertEquals(expectedWeight, actualWeight, delta,
                "Weight không đúng khi số học viên cực lớn (stress test)");
    }

    // =====================================================================================
    // ERROR SCENARIOS
    // =====================================================================================

    /**
     * TC_ES_01:
     * avgRating = 4.0, numberStudent = -1
     * -> log10(0) = -Infinity
     * -> weight = 0.7*4.0 + 0.3*(-Infinity) = -Infinity
     * Mục tiêu: xác nhận method KHÔNG ném exception và trả ra -Infinity.
     */
    @Test
    void shouldReturnNegativeInfinity_whenStudentCountIsNegative() throws Exception {
        // Given
        double avgRating = 4.0;
        int numberStudent = -1;

        // expectedWeight toán học:
        // normalizedStudent = log10(0) = -Infinity
        // => expectedWeight = -Infinity
        double expectedWeight = Double.NEGATIVE_INFINITY;

        Method privateMethod = CourseInfService.class.getDeclaredMethod(
                "countWeightOfCourse",
                double.class,
                int.class
        );
        privateMethod.setAccessible(true);

        // When
        Object result = privateMethod.invoke(courseInfService, avgRating, numberStudent);
        double actualWeight = (double) result;

        // Then
        assertTrue(Double.isInfinite(actualWeight) && actualWeight < 0,
                "Kỳ vọng weight là -Infinity khi numberStudent = -1");
        // (tùy chọn) so sánh trực tiếp:
        assertEquals(expectedWeight, actualWeight,
                "Giá trị thực tế không phải -Infinity như mong đợi");
    }

    /**
     * TC_ES_02:
     * avgRating = -2.0, numberStudent = 50
     * Rating âm (không hợp lệ về mặt business) nhưng hàm vẫn tính toán.
     * Kỳ vọng: kết quả có thể âm nhưng là số hữu hạn.
     */
    @Test
    void shouldAllowNegativeWeight_whenRatingIsNegative() throws Exception {
        // Given
        double avgRating = -2.0;
        int numberStudent = 50;

        double normalizedStudent = Math.log10(numberStudent + 1); // log10(51)
        double expectedWeight = 0.7 * avgRating + 0.3 * normalizedStudent;

        Method privateMethod = CourseInfService.class.getDeclaredMethod(
                "countWeightOfCourse",
                double.class,
                int.class
        );
        privateMethod.setAccessible(true);

        // When
        Object result = privateMethod.invoke(courseInfService, avgRating, numberStudent);
        double actualWeight = (double) result;

        // Then
        double delta = 0.001;
        assertEquals(expectedWeight, actualWeight, delta,
                "Weight không đúng khi rating âm (business data bất thường)");
        // và đảm bảo nó KHÔNG phải NaN / Infinity
        assertTrue(!Double.isNaN(actualWeight) && !Double.isInfinite(actualWeight),
                "Giá trị weight phải là số hữu hạn hợp lệ, không phải NaN/Infinity");
    }

    /**
     * TC_ES_03:
     * avgRating = 10.0, numberStudent = 10
     * Rating vượt biên (giả sử hệ thống cho phép tối đa 5.0) -> kiểm tra rằng hàm vẫn tính,
     * không giới hạn/cap rating.
     */
    @Test
    void shouldAllowOverMaxRatingAndReturnHighWeight() throws Exception {
        // Given
        double avgRating = 10.0;
        int numberStudent = 10;

        double normalizedStudent = Math.log10(numberStudent + 1); // log10(11)
        double expectedWeight = 0.7 * avgRating + 0.3 * normalizedStudent;

        Method privateMethod = CourseInfService.class.getDeclaredMethod(
                "countWeightOfCourse",
                double.class,
                int.class
        );
        privateMethod.setAccessible(true);

        // When
        Object result = privateMethod.invoke(courseInfService, avgRating, numberStudent);
        double actualWeight = (double) result;

        // Then
        double delta = 0.001;
        assertEquals(expectedWeight, actualWeight, delta,
                "Weight không đúng khi rating vượt quá ngưỡng tối đa hợp lệ (ví dụ >5.0)");
        // và đảm bảo không bị cap rating xuống 5.0
        assertTrue(actualWeight >= 0.7 * 10.0,
                "Có vẻ rating bị cap thay vì dùng đúng giá trị 10.0");
    }
}
