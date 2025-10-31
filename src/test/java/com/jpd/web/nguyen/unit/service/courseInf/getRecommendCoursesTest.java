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
import org.mockito.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho:
 *   public List<CourseInfDto> getRecommendCourses()
 *
 * Bao phủ:
 * - Happy Paths:
 *      TC_HP_01, TC_HP_02
 * - Edge Cases:
 *      TC_EC_01, TC_EC_02, TC_EC_03, TC_EC_04, TC_EC_05
 * - Error Scenarios:
 *      TC_ES_01, TC_ES_02, TC_ES_03
 *
 * Kỹ thuật:
 *  - Mock CourseRepository, EnrollmentRepository
 *  - Mock static CourseTransForm.transformToCourseInfDto(...)
 *  - Dựng Enrollment list để điều khiển avgRating & numStudent gián tiếp cho weight
 *
 * Ghi chú:
 *  calculateAvtRatingAndNumberStudent(course):
 *      - enrollmentRepository.findByCourse(course) -> List<Enrollment>
 *      - avgRating = trung bình feedback.getRate() (chỉ tính enrollment có feedback != null)
 *      - numStudent = enrollments.size()
 *
 * countWeightOfCourse(avgRating, numStudent):
 *      = 0.7 * avgRating + 0.3 * log10(numStudent + 1)
 *
 * sort:
 *      courses.sort((a,b) -> Double.compare(scoreB, scoreA)) // giảm dần weight
 */
class getRecommendCoursesTest {

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

    // -------------------------------------------------
    // Helper: tạo danh sách Enrollment để điều khiển
    // avgRating và numStudent của 1 Course
    //
    // numStudents = size của list
    // feedbackRate = điểm rate cho enrollment đầu tiên
    //
    // => avgRating ≈ feedbackRate (vì chỉ 1 feedback != null)
    // => numStudent = numStudents
    // -------------------------------------------------
    private List<Enrollment> buildEnrollments(int numStudents, int feedbackRate) {
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

    // ==========================================================
    // ===================== HAPPY PATHS ========================
    // ==========================================================

    /**
     * TC_HP_01:
     * shouldReturnTop3SortedCourses_whenLanguageHasMultipleCourses
     *
     * Bối cảnh:
     *  - Có 1 ngôn ngữ.
     *  - findByLanguage trả về 5 course: A,B,C,D,E.
     *  - isPublic():
     *      A,B,C,D => true
     *      E => false (loại ngay từ đầu)
     *  - Mock enrollmentRepository để sinh weight:
     *      B: avg=4.8, students=500  (weight cao nhất)
     *      A: avg=4.5, students=100  (thứ 2)
     *      C: avg=4.0, students=50   (thứ 3)
     *      D: avg=3.0, students=10   (thứ 4)
     *  => Sau sort giảm dần weight: [B, A, C, D]
     *  => Top 3: [B, A, C]
     *
     * Kỳ vọng:
     *  - result.size() == 3
     *  - result[0] == dtoB
     *  - result[1] == dtoA
     *  - result[2] == dtoC
     */
    @Test
    void shouldReturnTop3SortedCourses_whenLanguageHasMultipleCourses() {
        // =========================
        // Given
        // =========================
        Language lang = mock(Language.class);

        Course courseA = mock(Course.class);
        Course courseB = mock(Course.class);
        Course courseC = mock(Course.class);
        Course courseD = mock(Course.class);
        Course courseE = mock(Course.class);

        when(courseA.isPublic()).thenReturn(true);
        when(courseB.isPublic()).thenReturn(true);
        when(courseC.isPublic()).thenReturn(true);
        when(courseD.isPublic()).thenReturn(true);
        when(courseE.isPublic()).thenReturn(false); // filtered out

        when(courseRepository.findDistinctLanguages())
                .thenReturn(List.of(lang));

        when(courseRepository.findByLanguage(lang))
                .thenReturn(List.of(courseA, courseB, courseC, courseD, courseE));

        // Mock enrollment stats -> điều khiển weight
        when(enrollmentRepository.findByCourse(courseB))
                .thenReturn(buildEnrollments(500, 48)); // top1
        when(enrollmentRepository.findByCourse(courseA))
                .thenReturn(buildEnrollments(100, 45)); // top2
        when(enrollmentRepository.findByCourse(courseC))
                .thenReturn(buildEnrollments(50, 40));  // top3
        when(enrollmentRepository.findByCourse(courseD))
                .thenReturn(buildEnrollments(10, 30));  // top4
        // courseE không public -> không cần mock

        CourseInfDto dtoA = new CourseInfDto();
        CourseInfDto dtoB = new CourseInfDto();
        CourseInfDto dtoC = new CourseInfDto();

        try (MockedStatic<CourseTransForm> mockedStatic = Mockito.mockStatic(CourseTransForm.class)) {
            // mapper cho 3 course top
            mockedStatic.when(() -> CourseTransForm.transformToCourseInfDto(courseB, 500, 48.0))
                    .thenReturn(dtoB);
            mockedStatic.when(() -> CourseTransForm.transformToCourseInfDto(courseA, 100, 45.0))
                    .thenReturn(dtoA);
            mockedStatic.when(() -> CourseTransForm.transformToCourseInfDto(courseC, 50, 40.0))
                    .thenReturn(dtoC);

            // =========================
            // When
            // =========================
            List<CourseInfDto> result = courseInfService.getRecommendCourses();

            // =========================
            // Then
            // =========================
            assertNotNull(result, "Kết quả không được null");
            assertEquals(3, result.size(), "Top 3 course công khai phải được trả về");

            assertSame(dtoB, result.get(0),
                    "Course có weight cao nhất (B) phải đứng đầu");
            assertSame(dtoA, result.get(1),
                    "Course weight thứ 2 (A) đứng thứ hai");
            assertSame(dtoC, result.get(2),
                    "Course weight thứ 3 (C) đứng thứ ba");
        }
    }

    /**
     * TC_HP_02:
     * shouldReturnTop3ForEachLanguage_whenMultipleLanguages
     *
     * Bối cảnh:
     *  - Có 2 ngôn ngữ: langJava, langPython.
     *  - Mỗi ngôn ngữ có >=3 course public.
     *  - Kết quả cuối cùng là append:
     *      [top3 của langJava] + [top3 của langPython]
     *    (theo thứ tự duyệt distinctLanguages)
     *
     * Ta giả lập:
     * langJava: J1,J2,J3,J4 (all public)
     *   -> weight order: J2 > J1 > J3 > J4
     *   -> top3 => [J2,J1,J3] => dtoJ2,dtoJ1,dtoJ3
     *
     * langPython: P1,P2,P3,P4 (all public)
     *   -> weight order: P3 > P1 > P2 > P4
     *   -> top3 => [P3,P1,P2] => dtoP3,dtoP1,dtoP2
     *
     * Kỳ vọng:
     *  - result.size() == 6
     *  - result = [dtoJ2, dtoJ1, dtoJ3, dtoP3, dtoP1, dtoP2]
     */
    @Test
    void shouldReturnTop3ForEachLanguage_whenMultipleLanguages() {
        // =========================
        // Given
        // =========================
        Language langJava = mock(Language.class);
        Language langPython = mock(Language.class);

        // Java courses
        Course j1 = mock(Course.class);
        Course j2 = mock(Course.class);
        Course j3 = mock(Course.class);
        Course j4 = mock(Course.class);

        when(j1.isPublic()).thenReturn(true);
        when(j2.isPublic()).thenReturn(true);
        when(j3.isPublic()).thenReturn(true);
        when(j4.isPublic()).thenReturn(true);

        // Python courses
        Course p1 = mock(Course.class);
        Course p2 = mock(Course.class);
        Course p3 = mock(Course.class);
        Course p4 = mock(Course.class);

        when(p1.isPublic()).thenReturn(true);
        when(p2.isPublic()).thenReturn(true);
        when(p3.isPublic()).thenReturn(true);
        when(p4.isPublic()).thenReturn(true);

        when(courseRepository.findDistinctLanguages())
                .thenReturn(List.of(langJava, langPython));

        when(courseRepository.findByLanguage(langJava))
                .thenReturn(List.of(j1, j2, j3, j4));

        when(courseRepository.findByLanguage(langPython))
                .thenReturn(List.of(p1, p2, p3, p4));

        // ---- Mock weight cho Java
        // Order mong muốn: j2 > j1 > j3 > j4
        when(enrollmentRepository.findByCourse(j2))
                .thenReturn(buildEnrollments(400, 49));  // top1 Java
        when(enrollmentRepository.findByCourse(j1))
                .thenReturn(buildEnrollments(300, 45));  // top2 Java
        when(enrollmentRepository.findByCourse(j3))
                .thenReturn(buildEnrollments(200, 42));  // top3 Java
        when(enrollmentRepository.findByCourse(j4))
                .thenReturn(buildEnrollments(100, 30));  // lowest Java

        // ---- Mock weight cho Python
        // Order mong muốn: p3 > p1 > p2 > p4
        when(enrollmentRepository.findByCourse(p3))
                .thenReturn(buildEnrollments(900, 48));  // top1 Python
        when(enrollmentRepository.findByCourse(p1))
                .thenReturn(buildEnrollments(600, 46));  // top2 Python
        when(enrollmentRepository.findByCourse(p2))
                .thenReturn(buildEnrollments(500, 45));  // top3 Python
        when(enrollmentRepository.findByCourse(p4))
                .thenReturn(buildEnrollments(50, 35));   // lowest Python

        // Chuẩn bị DTO giả
        CourseInfDto dtoJ1 = new CourseInfDto();
        CourseInfDto dtoJ2 = new CourseInfDto();
        CourseInfDto dtoJ3 = new CourseInfDto();
        CourseInfDto dtoP1 = new CourseInfDto();
        CourseInfDto dtoP2 = new CourseInfDto();
        CourseInfDto dtoP3 = new CourseInfDto();

        try (MockedStatic<CourseTransForm> mockedStatic = Mockito.mockStatic(CourseTransForm.class)) {
            // Java top3: j2, j1, j3
            mockedStatic.when(() -> CourseTransForm.transformToCourseInfDto(j2, 400, 49.0))
                    .thenReturn(dtoJ2);
            mockedStatic.when(() -> CourseTransForm.transformToCourseInfDto(j1, 300, 45.0))
                    .thenReturn(dtoJ1);
            mockedStatic.when(() -> CourseTransForm.transformToCourseInfDto(j3, 200, 42.0))
                    .thenReturn(dtoJ3);

            // Python top3: p3, p1, p2
            mockedStatic.when(() -> CourseTransForm.transformToCourseInfDto(p3, 900, 48.0))
                    .thenReturn(dtoP3);
            mockedStatic.when(() -> CourseTransForm.transformToCourseInfDto(p1, 600, 46.0))
                    .thenReturn(dtoP1);
            mockedStatic.when(() -> CourseTransForm.transformToCourseInfDto(p2, 500, 45.0))
                    .thenReturn(dtoP2);

            // =========================
            // When
            // =========================
            List<CourseInfDto> result = courseInfService.getRecommendCourses();

            // =========================
            // Then
            // =========================
            assertNotNull(result);
            assertEquals(6, result.size(),
                    "Mỗi ngôn ngữ top3 => tổng 6 DTO");

            // Thứ tự phải là [Java top3] rồi [Python top3]
            assertSame(dtoJ2, result.get(0));
            assertSame(dtoJ1, result.get(1));
            assertSame(dtoJ3, result.get(2));

            assertSame(dtoP3, result.get(3));
            assertSame(dtoP1, result.get(4));
            assertSame(dtoP2, result.get(5));
        }
    }

    // ==========================================================
    // ===================== EDGE CASES =========================
    // ==========================================================

    /**
     * TC_EC_01:
     * shouldReturnEmptyList_whenNoLanguagesFound
     *
     * Bối cảnh:
     *  - courseRepository.findDistinctLanguages() trả về []
     *
     * Kỳ vọng:
     *  - Kết quả cuối là list rỗng
     *  - Không gọi CourseTransForm
     */
    @Test
    void shouldReturnEmptyList_whenNoLanguagesFound() {
        // =========================
        // Given
        // =========================
        when(courseRepository.findDistinctLanguages())
                .thenReturn(List.of());

        try (MockedStatic<CourseTransForm> mockedStatic = Mockito.mockStatic(CourseTransForm.class)) {
            // =========================
            // When
            // =========================
            List<CourseInfDto> result = courseInfService.getRecommendCourses();

            // =========================
            // Then
            // =========================
            assertNotNull(result, "Luôn trả về list, không null");
            assertTrue(result.isEmpty(),
                    "Khi không có ngôn ngữ nào thì kết quả phải rỗng");

            // Không được map bất kỳ course nào
            mockedStatic.verifyNoInteractions();
        }
    }

    /**
     * TC_EC_02:
     * shouldReturnEmptyList_whenLanguageHasNoCourses
     *
     * Bối cảnh:
     *  - distinctLanguages trả về [langA]
     *  - findByLanguage(langA) trả về []
     *
     * Kỳ vọng:
     *  - List kết quả rỗng
     *  - Không gọi mapper static
     */
    @Test
    void shouldReturnEmptyList_whenLanguageHasNoCourses() {
        // =========================
        // Given
        // =========================
        Language langA = mock(Language.class);

        when(courseRepository.findDistinctLanguages())
                .thenReturn(List.of(langA));

        when(courseRepository.findByLanguage(langA))
                .thenReturn(List.of()); // rỗng

        try (MockedStatic<CourseTransForm> mockedStatic = Mockito.mockStatic(CourseTransForm.class)) {
            // =========================
            // When
            // =========================
            List<CourseInfDto> result = courseInfService.getRecommendCourses();

            // =========================
            // Then
            // =========================
            assertNotNull(result);
            assertTrue(result.isEmpty(),
                    "Không có khóa học nào cho ngôn ngữ -> kết quả rỗng");

            mockedStatic.verifyNoInteractions();
        }
    }

    /**
     * TC_EC_03:
     * shouldIgnoreNonPublicCourses_whenFiltering
     *
     * Bối cảnh:
     *  - Có 1 ngôn ngữ.
     *  - findByLanguage trả về [publicCourse, privateCourse]
     *    với privateCourse.isPublic() == false
     *  - Chỉ publicCourse được tính weight và được map ra DTO
     *
     * Kỳ vọng:
     *  - result.size() == 1
     *  - mapper static chỉ được gọi với publicCourse
     */
    @Test
    void shouldIgnoreNonPublicCourses_whenFiltering() {
        // =========================
        // Given
        // =========================
        Language lang = mock(Language.class);

        Course publicCourse = mock(Course.class);
        Course privateCourse = mock(Course.class);

        when(publicCourse.isPublic()).thenReturn(true);
        when(privateCourse.isPublic()).thenReturn(false);

        when(courseRepository.findDistinctLanguages())
                .thenReturn(List.of(lang));

        when(courseRepository.findByLanguage(lang))
                .thenReturn(List.of(publicCourse, privateCourse));

        // Tạo weight cho publicCourse
        when(enrollmentRepository.findByCourse(publicCourse))
                .thenReturn(buildEnrollments(50, 45));

        CourseInfDto dtoPublic = new CourseInfDto();

        try (MockedStatic<CourseTransForm> mockedStatic = Mockito.mockStatic(CourseTransForm.class)) {

            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseInfDto(publicCourse, 50, 45.0)
            ).thenReturn(dtoPublic);

            // =========================
            // When
            // =========================
            List<CourseInfDto> result = courseInfService.getRecommendCourses();

            // =========================
            // Then
            // =========================
            assertNotNull(result);
            assertEquals(1, result.size(),
                    "Chỉ có 1 course public nên kết quả phải có đúng 1 DTO");

            assertSame(dtoPublic, result.get(0),
                    "DTO duy nhất phải đến từ course public");

            // Private course không bao giờ được map
            mockedStatic.verify(() ->
                            CourseTransForm.transformToCourseInfDto(privateCourse, 0, 0.0),
                    times(0)
            );
        }
    }

    /**
     * TC_EC_04:
     * shouldReturnOnlyTwo_whenLanguageHasOnlyTwoPublicCourses
     *
     * Bối cảnh:
     *  - Có 1 ngôn ngữ.
     *  - findByLanguage trả về đúng 2 course public: C1, C2
     *  - Sau sort theo weight giảm dần, thứ tự có thể là C2 > C1
     *  - limit = Math.min(3, 2) = 2
     *
     * Kỳ vọng:
     *  - result.size() == 2
     *  - result[0] == dtoC2 (weight cao hơn)
     *  - result[1] == dtoC1
     */
    @Test
    void shouldReturnOnlyTwo_whenLanguageHasOnlyTwoPublicCourses() {
        // =========================
        // Given
        // =========================
        Language lang = mock(Language.class);

        Course c1 = mock(Course.class);
        Course c2 = mock(Course.class);

        when(c1.isPublic()).thenReturn(true);
        when(c2.isPublic()).thenReturn(true);

        when(courseRepository.findDistinctLanguages())
                .thenReturn(List.of(lang));

        when(courseRepository.findByLanguage(lang))
                .thenReturn(List.of(c1, c2));

        // Weight: c2 > c1
        when(enrollmentRepository.findByCourse(c2))
                .thenReturn(buildEnrollments(300, 49)); // mạnh hơn
        when(enrollmentRepository.findByCourse(c1))
                .thenReturn(buildEnrollments(50, 42));  // yếu hơn

        CourseInfDto dtoC1 = new CourseInfDto();
        CourseInfDto dtoC2 = new CourseInfDto();

        try (MockedStatic<CourseTransForm> mockedStatic = Mockito.mockStatic(CourseTransForm.class)) {
            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseInfDto(c2, 300, 49.0)
            ).thenReturn(dtoC2);
            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseInfDto(c1, 50, 42.0)
            ).thenReturn(dtoC1);

            // =========================
            // When
            // =========================
            List<CourseInfDto> result = courseInfService.getRecommendCourses();

            // =========================
            // Then
            // =========================
            assertNotNull(result);
            assertEquals(2, result.size(),
                    "Chỉ có 2 course public nên phải trả đúng 2 DTO");

            assertSame(dtoC2, result.get(0),
                    "Course có weight cao hơn phải đứng đầu");
            assertSame(dtoC1, result.get(1),
                    "Course thứ hai theo weight phải đứng thứ hai");
        }
    }

    /**
     * TC_EC_05:
     * shouldHandleTieWeightsWithoutCrashing
     *
     * Bối cảnh:
     *  - Có 1 ngôn ngữ.
     *  - 3 course public: T1, T2, T3
     *  - Tất cả đều có cùng avgRating và numStudent => cùng weight
     *  - Java List.sort với comparator Double.compare(scoreB, scoreA) khi score bằng nhau => 0
     *    => sort ổn định (TimSort stable), giữ nguyên thứ tự ban đầu sau filter.
     *
     * Kỳ vọng:
     *  - result.size() == 3
     *  - result = [dtoT1, dtoT2, dtoT3] (giữ nguyên thứ tự filter ban đầu)
     *  - không ném exception
     */
    @Test
    void shouldHandleTieWeightsWithoutCrashing() {
        // =========================
        // Given
        // =========================
        Language lang = mock(Language.class);

        Course t1 = mock(Course.class);
        Course t2 = mock(Course.class);
        Course t3 = mock(Course.class);

        when(t1.isPublic()).thenReturn(true);
        when(t2.isPublic()).thenReturn(true);
        when(t3.isPublic()).thenReturn(true);

        when(courseRepository.findDistinctLanguages())
                .thenReturn(List.of(lang));

        // thứ tự ban đầu: [t1, t2, t3]
        when(courseRepository.findByLanguage(lang))
                .thenReturn(List.of(t1, t2, t3));

        // Các course đều có cùng stats => cùng weight
        when(enrollmentRepository.findByCourse(t1))
                .thenReturn(buildEnrollments(100, 45));
        when(enrollmentRepository.findByCourse(t2))
                .thenReturn(buildEnrollments(100, 45));
        when(enrollmentRepository.findByCourse(t3))
                .thenReturn(buildEnrollments(100, 45));

        CourseInfDto dtoT1 = new CourseInfDto();
        CourseInfDto dtoT2 = new CourseInfDto();
        CourseInfDto dtoT3 = new CourseInfDto();

        try (MockedStatic<CourseTransForm> mockedStatic = Mockito.mockStatic(CourseTransForm.class)) {

            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseInfDto(t1, 100, 45.0)
            ).thenReturn(dtoT1);
            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseInfDto(t2, 100, 45.0)
            ).thenReturn(dtoT2);
            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseInfDto(t3, 100, 45.0)
            ).thenReturn(dtoT3);

            // =========================
            // When
            // =========================
            List<CourseInfDto> result = courseInfService.getRecommendCourses();

            // =========================
            // Then
            // =========================
            assertNotNull(result);
            assertEquals(3, result.size(),
                    "Có 3 course public, limit=3 => phải trả đủ 3 DTO");

            // Vì weight ngang nhau, comparator trả 0 cho mọi cặp,
            // TimSort stable => giữ nguyên thứ tự sau filter => t1,t2,t3
            assertSame(dtoT1, result.get(0),
                    "Thứ tự phải giữ nguyên khi weight bằng nhau (t1 trước)");
            assertSame(dtoT2, result.get(1),
                    "Thứ tự phải giữ nguyên khi weight bằng nhau (t2 kế tiếp)");
            assertSame(dtoT3, result.get(2),
                    "Thứ tự phải giữ nguyên khi weight bằng nhau (t3 cuối)");
        }
    }

    // ==========================================================
    // ================== ERROR SCENARIOS =======================
    // ==========================================================

    /**
     * TC_ES_01:
     * shouldThrowNullPointer_whenDistinctLanguagesIsNull
     *
     * Bối cảnh:
     *  - courseRepository.findDistinctLanguages() trả về null (contract violation).
     *  - Code hiện tại: for (Language lang : distinctLanguages) ... sẽ NPE.
     *
     * Kỳ vọng:
     *  - Ném NullPointerException
     *  - Đây là test "để lộ rủi ro": service không phòng thủ null từ repo.
     */
    @Test
    void shouldThrowNullPointer_whenDistinctLanguagesIsNull() {
        // =========================
        // Given
        // =========================
        when(courseRepository.findDistinctLanguages())
                .thenReturn(null); // vi phạm contract

        // =========================
        // When / Then
        // =========================
        assertThrows(NullPointerException.class,
                () -> courseInfService.getRecommendCourses(),
                "Nếu repository trả về null thay vì list, code hiện tại sẽ NPE");
    }

    /**
     * TC_ES_02:
     * shouldThrowNullPointer_whenFindByLanguageReturnsNull
     *
     * Bối cảnh:
     *  - distinctLanguages trả về [langA]
     *  - findByLanguage(langA) trả về null
     *  - Code gọi .stream() trên kết quả => NPE
     *
     * Kỳ vọng:
     *  - Ném NullPointerException
     */
    @Test
    void shouldThrowNullPointer_whenFindByLanguageReturnsNull() {
        // =========================
        // Given
        // =========================
        Language langA = mock(Language.class);

        when(courseRepository.findDistinctLanguages())
                .thenReturn(List.of(langA));

        when(courseRepository.findByLanguage(langA))
                .thenReturn(null); // vi phạm contract

        // =========================
        // When / Then
        // =========================
        assertThrows(NullPointerException.class,
                () -> courseInfService.getRecommendCourses(),
                "Nếu findByLanguage trả null, phần .stream() sẽ gây NPE");
    }

    /**
     * TC_ES_03:
     * shouldThrowNullPointer_whenEnrollmentListIsNullInRatingCalculation
     *
     * Bối cảnh:
     *  - Có 1 ngôn ngữ, 1 course public.
     *  - findByLanguage(lang) trả về list có 1 course public.
     *  - enrollmentRepository.findByCourse(course) trả về null
     *    => calculateAvtRatingAndNumberStudent() sẽ gọi .isEmpty() trên null => NPE.
     *
     * Kỳ vọng:
     *  - assertThrows(NullPointerException)
     */
    @Test
    void shouldThrowNullPointer_whenEnrollmentListIsNullInRatingCalculation() {
        // =========================
        // Given
        // =========================
        Language lang = mock(Language.class);
        Course c1 = mock(Course.class);
        when(c1.isPublic()).thenReturn(true);

        when(courseRepository.findDistinctLanguages())
                .thenReturn(List.of(lang));

        when(courseRepository.findByLanguage(lang))
                .thenReturn(List.of(c1));

        // Đây là vi phạm contract:
        // bình thường repo nên trả về List<Enrollment> (kể cả empty),
        // nhưng ở đây trả null để simulate lỗi hạ tầng DB.
        when(enrollmentRepository.findByCourse(c1))
                .thenReturn(null);

        // =========================
        // When / Then
        // =========================
        assertThrows(NullPointerException.class,
                () -> courseInfService.getRecommendCourses(),
                "Nếu enrollmentRepository trả null, tính weight sẽ NPE");
    }
}
