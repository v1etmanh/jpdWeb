package com.jpd.web.nguyen.unit.service.courseInf;

import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;
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
 * Test cho hàm public:
 *
 *     public List<CourseInfDto> searchByKey(String searchKey)
 *
 * Bao phủ toàn bộ scenario:
 *
 * 1. Happy Paths
 *    - TC_HP_01: Repository trả về 2 khóa học hợp lệ → trả về 2 DTO tương ứng
 *
 * 2. Edge Cases
 *    - TC_EC_01: Repository trả về list rỗng → kết quả trả về list rỗng, không null
 *    - TC_EC_02: searchKey chỉ chứa dấu cách → sau trim() = "", vẫn gọi repository với "", kết quả rỗng
 *
 * 3. Error Scenarios
 *    - TC_ES_01: searchKey = null → NullPointerException do gọi searchKey.trim()
 *
 * Ghi chú kỹ thuật:
 * - calculateAvtRatingAndNumberStudent(course) là private, nhưng sẽ chạy thật.
 *   → Nó dùng enrollmentRepository.findByCourse(course) để tạo avgRating và numStudent.
 *   → Vì vậy trong test Happy Path, ta phải mock enrollmentRepository để trả về danh sách Enrollment phù hợp.
 *
 * - CourseTransForm.transformToCourseInfDto(...) là static → cần mock bằng MockedStatic.
 */
class searchByKeyTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CourseInfService courseInfService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Helper:
     * Tạo danh sách Enrollment để điều khiển avgRating & numStudent.
     *
     * Quy ước:
     * - numStudents = kích thước list
     * - feedbackRate = điểm rate của feedback trong enrollment đầu tiên
     * - Các enrollment còn lại không có feedback
     *
     * => avgRating = feedbackRate (vì chỉ có 1 feedback khác null)
     * => numStudent = numStudents
     */
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

    /**
     * =====================
     * TC_HP_01 (Happy Path)
     * =====================
     *
     * shouldReturnDtoList_whenRepositoryReturnsCourses
     *
     * // Given:
     * - searchKey = "java basics"
     * - courseRepository.searchByKey("java basics") trả về [courseA, courseB]
     * - enrollmentRepository.findByCourse(courseA) trả về list 100 enrollment,
     *      enrollment đầu tiên có feedback rate = 4.5
     *      => avgRatingA = 4.5, numStudentA = 100
     * - enrollmentRepository.findByCourse(courseB) trả về list 50 enrollment,
     *      enrollment đầu tiên có feedback rate = 4.0
     *      => avgRatingB = 4.0, numStudentB = 50
     *
     * - Mock static mapper:
     *      CourseTransForm.transformToCourseInfDto(courseA, 100, 4.5) -> dtoA
     *      CourseTransForm.transformToCourseInfDto(courseB, 50, 4.0)  -> dtoB
     *
     * // When:
     * - result = courseInfService.searchByKey("java basics")
     *
     * // Then:
     * - result != null
     * - result.size() == 2
     * - result.get(0) == dtoA
     * - result.get(1) == dtoB
     */
    @Test
    void shouldReturnDtoList_whenRepositoryReturnsCourses() {
        // =========================
        // Given
        // =========================
        String searchKey = "java basics";

        Course courseA = mock(Course.class);
        Course courseB = mock(Course.class);

        when(courseRepository.searchByKey("java basics"))
                .thenReturn(List.of(courseA, courseB));

        // Mock enrollment stats cho courseA và courseB
        when(enrollmentRepository.findByCourse(courseA))
                .thenReturn(buildEnrollments(100, 45)); // -> avg=4.5, numStudent=100
        when(enrollmentRepository.findByCourse(courseB))
                .thenReturn(buildEnrollments(50, 40));  // -> avg=4.0, numStudent=50

        // Các DTO giả để mapper static trả về
        CourseInfDto dtoA = new CourseInfDto();
        CourseInfDto dtoB = new CourseInfDto();

        try (MockedStatic<CourseTransForm> mockedStatic = Mockito.mockStatic(CourseTransForm.class)) {

            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseInfDto(courseA, 100, 4.5)
            ).thenReturn(dtoA);

            mockedStatic.when(() ->
                    CourseTransForm.transformToCourseInfDto(courseB, 50, 4.0)
            ).thenReturn(dtoB);

            // =========================
            // When
            // =========================
            List<CourseInfDto> result = courseInfService.searchByKey(searchKey);

            // =========================
            // Then
            // =========================
            assertNotNull(result, "Kết quả không được null trong Happy Path");
            assertEquals(2, result.size(),
                    "Repository trả về 2 course => phải trả về 2 DTO");

            assertSame(dtoA, result.get(0),
                    "DTO đầu tiên phải tương ứng với courseA");
            assertSame(dtoB, result.get(1),
                    "DTO thứ hai phải tương ứng với courseB");

            // Có thể verify mapper static được gọi đúng số lần
            mockedStatic.verify(() ->
                            CourseTransForm.transformToCourseInfDto(courseA, 100, 4.5),
                    times(1));
            mockedStatic.verify(() ->
                            CourseTransForm.transformToCourseInfDto(courseB, 50, 4.0),
                    times(1));
        }
    }

    /**
     * =====================
     * TC_EC_01 (Edge Case)
     * =====================
     *
     * shouldReturnEmptyList_whenRepositoryReturnsEmptyList
     *
     * // Given:
     * - searchKey = "spring"
     * - courseRepository.searchByKey("spring") trả về []
     *
     * // When:
     * - result = courseInfService.searchByKey("spring")
     *
     * // Then:
     * - result != null
     * - result.isEmpty() == true
     * - Không gọi CourseTransForm.transformToCourseInfDto(...)
     */
    @Test
    void shouldReturnEmptyList_whenRepositoryReturnsEmptyList() {
        // =========================
        // Given
        // =========================
        String searchKey = "spring";

        when(courseRepository.searchByKey("spring"))
                .thenReturn(List.of()); // empty list

        // enrollmentRepository và mapper static sẽ không bị gọi
        try (MockedStatic<CourseTransForm> mockedStatic = Mockito.mockStatic(CourseTransForm.class)) {

            // =========================
            // When
            // =========================
            List<CourseInfDto> result = courseInfService.searchByKey(searchKey);

            // =========================
            // Then
            // =========================
            assertNotNull(result, "Ngay cả khi không có course, hàm vẫn phải trả về list rỗng chứ không null");
            assertTrue(result.isEmpty(),
                    "Khi repository trả về empty list, kết quả phải là list rỗng");

            // mapper static tuyệt đối không được gọi
            mockedStatic.verifyNoInteractions();
        }

        // enrollmentRepository không nên bị gọi vì không có course nào để tính rating
        verifyNoInteractions(enrollmentRepository);
    }

    /**
     * =====================
     * TC_EC_02 (Edge Case)
     * =====================
     *
     * shouldCallRepositoryWithTrimmedKey_andReturnEmptyList_whenKeyIsSpacesOnly
     *
     * // Given:
     * - searchKey = "    " (chỉ dấu cách)
     * - searchKey.trim() == "" (chuỗi rỗng, KHÔNG phải null)
     * - courseRepository.searchByKey("") trả về []
     *
     * // When:
     * - result = courseInfService.searchByKey("    ")
     *
     * // Then:
     * - result != null
     * - result.isEmpty() == true
     * - Verify repository được gọi với "" (đã trim)
     * - mapper static không bị gọi
     */
    @Test
    void shouldCallRepositoryWithTrimmedKey_andReturnEmptyList_whenKeyIsSpacesOnly() {
        // =========================
        // Given
        // =========================
        String rawKey = "    ";
        String trimmedKey = ""; // rawKey.trim()

        when(courseRepository.searchByKey(trimmedKey))
                .thenReturn(List.of()); // no courses found

        try (MockedStatic<CourseTransForm> mockedStatic = Mockito.mockStatic(CourseTransForm.class)) {

            // =========================
            // When
            // =========================
            List<CourseInfDto> result = courseInfService.searchByKey(rawKey);

            // =========================
            // Then
            // =========================
            assertNotNull(result,
                    "Phải trả về list rỗng thay vì null kể cả khi key chỉ có dấu cách");
            assertTrue(result.isEmpty(),
                    "Không có course nào -> list kết quả phải rỗng");

            // Repository phải được gọi đúng với key đã trim là ""
            verify(courseRepository, times(1))
                    .searchByKey(trimmedKey);

            // mapper static không được gọi vì không có course
            mockedStatic.verifyNoInteractions();
        }

        // enrollmentRepository không nên bị gọi (không có course để tính rating)
        verifyNoInteractions(enrollmentRepository);
    }

    /**
     * ==========================
     * TC_ES_01 (Error Scenario)
     * ==========================
     *
     * shouldThrowNullPointerException_whenSearchKeyIsNull
     *
     * // Given:
     * - searchKey = null
     *
     * // When:
     * - gọi courseInfService.searchByKey(null)
     *   nó sẽ chạy vào:
     *      if (searchKey.trim() == null)
     *          return null;
     *   => searchKey.trim() sẽ ném NullPointerException trước khi if so sánh
     *
     * // Then:
     * - assertThrows(NullPointerException)
     * - verifyNoInteractions(courseRepository)
     * - verifyNoInteractions(enrollmentRepository)
     */
    @Test
    void shouldThrowNullPointerException_whenSearchKeyIsNull() {
        // =========================
        // Given
        // =========================
        String searchKey = null;

        // =========================
        // When / Then
        // =========================
        assertThrows(
                NullPointerException.class,
                () -> courseInfService.searchByKey(searchKey),
                "Gọi searchByKey(null) phải ném NullPointerException vì gọi trim() trên null"
        );

        // Chưa kịp gọi xuống repository nào
        verifyNoInteractions(courseRepository);
        verifyNoInteractions(enrollmentRepository);
    }
}
