package com.jpd.web.nguyen.unit.service.courseInf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Customer;
import com.jpd.web.model.CustomerModuleContent;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CustomerModuleContentRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.dto.CourseLearningCardDto;
import com.jpd.web.service.CourseInfService;
import com.jpd.web.service.utils.ValidationResources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

/**
 * Unit tests cho public method:
 *   List<CourseLearningCardDto> retrieveYourCourse(String email)
 *
 * Bao phủ:
 *   - Happy Paths
 *   - Edge Cases
 *   - Error Scenarios
 *
 * Kỹ thuật:
 *   - Mock customerRepository.findByEmail(...)
 *   - Mock contentRepository.findByEnrollment(...) (được dùng sâu bên trong calculateContentProgress)
 *   - Dựng Course/Enrollment/Customer thật để cho countTotalContentsByType và calculateContentProgress chạy đúng.
 *
 * Giả định:
 *   - CourseLearningCardDto có setProgress(double)
 *   - CourseTransForm.transformToCourseLearningCardDto(course) trả về dto rỗng hợp lệ (không null).
 */
class retrieveYourCourseTest {

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

    // -------------------------------------------------
    // Helpers để dựng dữ liệu domain giống thực tế
    // -------------------------------------------------

    /**
     * ModuleContent là abstract -> tạo subclass vô danh
     */
    private ModuleContent createModuleContent(TypeOfContent type) {
        ModuleContent mc = new ModuleContent() {
            // override abstract methods if needed
        };
        mc.setTypeOfContent(type);
        return mc;
    }

    /**
     * Tạo Module với N ModuleContent theo các type cho trước.
     * Ví dụ createModule(VIDEO, MULTIPLE_CHOICE) -> Module có 2 content: VIDEO, MULTIPLE_CHOICE.
     */
    private Module createModule(TypeOfContent... types) {
        Module mod = new Module();
        List<ModuleContent> contents = new ArrayList<>();
        for (TypeOfContent t : types) {
            contents.add(createModuleContent(t));
        }
        mod.setModuleContent(contents);
        return mod;
    }

    /**
     * Tạo Chapter từ các Module.
     */
    private Chapter createChapter(Module... modules) {
        Chapter ch = new Chapter();
        ch.setModules(Arrays.asList(modules));
        return ch;
    }

    /**
     * Tạo Course từ các Chapter.
     * Bạn nên set cả course.setName(...) để tránh NullPointerException trong log.
     */
    private Course createCourse(String name, Chapter... chapters) {
        Course c = new Course();
        c.setName(name);
        c.setChapters(Arrays.asList(chapters));
        return c;
    }

    /**
     * Helper tạo Enrollment gắn với Course
     */
    private Enrollment createEnrollment(Course course) {
        Enrollment e = new Enrollment();
        e.setCourse(course);
        e.setEnrollId(1L);
        return e;
    }

    /**
     * Helper tạo Customer với danh sách Enrollment
     */
    private Customer createCustomerWithEnrollments(List<Enrollment> enrollments) {
        Customer cus = new Customer();
        cus.setEnrollments(enrollments);
        return cus;
    }

    /**
     * Helper: CustomerModuleContent với set các type đã hoàn thành.
     * Ví dụ cmcOf(VIDEO, MULTIPLE_CHOICE) => CMC có set {VIDEO, MULTIPLE_CHOICE}.
     */
    private CustomerModuleContent cmcOf(TypeOfContent... completedTypes) {
        CustomerModuleContent cmc = new CustomerModuleContent();
        if (completedTypes == null) {
            cmc.setTypeOfContent(null);
        } else {
            cmc.setTypeOfContent(new HashSet<>(Arrays.asList(completedTypes)));
        }
        return cmc;
    }

    // -------------------------------------------------
    // HAPPY PATHS
    // -------------------------------------------------

    /**
     * HP_01:
     * 1 enrollment, total=10, completed=5 => progress 50.0%
     *
     * totalContents = 10:
     *   - Ta tạo 10 ModuleContent trong Course (ví dụ 6 VIDEO + 4 MULTIPLE_CHOICE)
     * completedContents = 5:
     *   - Mock contentRepository.findByEnrollment() trả về danh sách
     *     CustomerModuleContent sao cho đếm được 5 completion phân loại theo type.
     *
     * Kết quả mong đợi:
     *   result.size() == 1
     *   result.get(0).getProgress() == 50.0
     */
    @Test
    void shouldReturnCourseWith50PercentProgress_whenSingleEnrollmentHalfCompleted() {
        // =========================
        // Given
        // =========================
        String email = "user@example.com";

        // Course có totalContents = 10.
        // Ví dụ: module1 có 5 VIDEO, module2 có 5 MULTIPLE_CHOICE
        Module m1 = createModule(
                TypeOfContent.VIDEO, TypeOfContent.VIDEO, TypeOfContent.VIDEO, TypeOfContent.VIDEO, TypeOfContent.VIDEO
        );
        Module m2 = createModule(
                TypeOfContent.MULTIPLE_CHOICE, TypeOfContent.MULTIPLE_CHOICE, TypeOfContent.MULTIPLE_CHOICE,
                TypeOfContent.MULTIPLE_CHOICE,
                TypeOfContent.MULTIPLE_CHOICE
        );
        Chapter ch = createChapter(m1, m2);
        Course course = createCourse("Course A", ch);

        Enrollment enrollment = createEnrollment(course);

        Customer customer = createCustomerWithEnrollments(Collections.singletonList(enrollment));

        // Mock tìm customer theo email
        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));

        // completedContents = 5
        // Ví dụ user đã hoàn thành:
        // 3 VIDEO và 2 MULTIPLE_CHOICE
        List<CustomerModuleContent> completedList = Arrays.asList(
                cmcOf(TypeOfContent.VIDEO),
                cmcOf(TypeOfContent.VIDEO),
                cmcOf(TypeOfContent.VIDEO),
                cmcOf(TypeOfContent.MULTIPLE_CHOICE),
                cmcOf(TypeOfContent.MULTIPLE_CHOICE)
        );
        when(contentRepository.findByEnrollment(enrollment)).thenReturn(completedList);

        // =========================
        // When
        // =========================
        List<CourseLearningCardDto> result = courseInfService.retrieveYourCourse(email);

        // =========================
        // Then
        // Tổng content trong course là 10, user hoàn thành 5 -> progress = 50.0
        assertEquals(1, result.size(), "Phải trả về đúng 1 khóa học");
        assertEquals(50.0, result.get(0).getProgress(), 0.0001,
                "Progress phải bằng 50.0%");
    }

    /**
     * HP_02:
     * 2 enrollment, trả về 2 DTO với progress khác nhau.
     *
     * Enrollment A:
     *   total=8, completed=4 -> 50.0%
     *
     * Enrollment B:
     *   total=4, completed=4 -> 100.0%
     *
     * Kết quả mong đợi:
     *   result.size() == 2
     *   progress của DTO[0] và DTO[1] lần lượt ~ 50.0 và 100.0
     *
     * Lưu ý: thứ tự enrollments trong list gốc sẽ là thứ tự trong kết quả.
     */
    @Test
    void shouldReturnTwoDtosWithDifferentProgress_whenCustomerHasTwoEnrollments() {
        // =========================
        // Given
        // =========================
        String email = "multi@example.com";

        // Enrollment A: total=8
        // Course A có 8 contents (4 VIDEO + 4 MULTIPLE_CHOICE)
        Module a1 = createModule(
                TypeOfContent.VIDEO, TypeOfContent.VIDEO, TypeOfContent.VIDEO, TypeOfContent.VIDEO
        );
        Module a2 = createModule(
                TypeOfContent.MULTIPLE_CHOICE, TypeOfContent.MULTIPLE_CHOICE, TypeOfContent.MULTIPLE_CHOICE,
                TypeOfContent.MULTIPLE_CHOICE
        );
        Course courseA = createCourse("Course A", createChapter(a1, a2));
        Enrollment enrollmentA = createEnrollment(courseA);

        // Enrollment B: total=4
        // Course B có 4 contents (4 READING)
        Module b1 = createModule(
                TypeOfContent.READING, TypeOfContent.READING, TypeOfContent.READING, TypeOfContent.READING
        );
        Course courseB = createCourse("Course B", createChapter(b1));
        Enrollment enrollmentB = createEnrollment(courseB);

        Customer customer = createCustomerWithEnrollments(Arrays.asList(enrollmentA, enrollmentB));

        // Mock customer lookup
        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));

        // Mock completed cho enrollment A: completed=4 (2 VIDEO + 2 MULTIPLE_CHOICE chẳng hạn)
        List<CustomerModuleContent> completedA = Arrays.asList(
                cmcOf(TypeOfContent.VIDEO),
                cmcOf(TypeOfContent.VIDEO),
                cmcOf(TypeOfContent.MULTIPLE_CHOICE),
                cmcOf(TypeOfContent.MULTIPLE_CHOICE)
        );
        when(contentRepository.findByEnrollment(enrollmentA)).thenReturn(completedA);

        // Mock completed cho enrollment B: completed=4 (4 READING)
        List<CustomerModuleContent> completedB = Arrays.asList(
                cmcOf(TypeOfContent.READING),
                cmcOf(TypeOfContent.READING),
                cmcOf(TypeOfContent.READING),
                cmcOf(TypeOfContent.READING)
        );
        when(contentRepository.findByEnrollment(enrollmentB)).thenReturn(completedB);

        // =========================
        // When
        // =========================
        List<CourseLearningCardDto> result = courseInfService.retrieveYourCourse(email);

        // =========================
        // Then
        assertEquals(2, result.size(), "Phải trả về đúng 2 DTO tương ứng 2 enrollment");

        // Enrollment A: total=8, completed=4 -> 50.0
        assertEquals(50.0, result.get(0).getProgress(), 0.0001,
                "Enrollment A phải có progress 50.0%");

        // Enrollment B: total=4, completed=4 -> 100.0
        assertEquals(100.0, result.get(1).getProgress(), 0.0001,
                "Enrollment B phải có progress 100.0%");
    }

    /**
     * HP_03:
     * Kiểm tra làm tròn đến 1 chữ số thập phân.
     *
     * total = 7
     * completed = 3
     *
     * rawPercent = (3 * 100.0 / 7) ≈ 42.857142857
     * round(rawPercent * 10) / 10.0 => 42.9
     *
     * Kết quả mong đợi:
     *   dto.getProgress() == 42.9
     */
    @Test
    void shouldRoundProgressToOneDecimal_whenCompletionIsPartial() {
        // =========================
        // Given
        // =========================
        String email = "round@example.com";

        // Course có total=7
        // Ví dụ: module có 7 VIDEO
        Module mod = createModule(
                TypeOfContent.VIDEO, TypeOfContent.VIDEO, TypeOfContent.VIDEO,
                TypeOfContent.VIDEO, TypeOfContent.VIDEO, TypeOfContent.VIDEO,
                TypeOfContent.VIDEO
        );
        Course course = createCourse("Course Round", createChapter(mod));
        Enrollment enrollment = createEnrollment(course);

        Customer customer = createCustomerWithEnrollments(Collections.singletonList(enrollment));
        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));

        // completed=3 (3 VIDEO)
        List<CustomerModuleContent> completedList = Arrays.asList(
                cmcOf(TypeOfContent.VIDEO),
                cmcOf(TypeOfContent.VIDEO),
                cmcOf(TypeOfContent.VIDEO)
        );
        when(contentRepository.findByEnrollment(enrollment)).thenReturn(completedList);

        // =========================
        // When
        // =========================
        List<CourseLearningCardDto> result = courseInfService.retrieveYourCourse(email);

        // =========================
        // Then
        assertEquals(1, result.size(), "Phải trả về 1 DTO");
        assertEquals(42.9, result.get(0).getProgress(), 0.0001,
                "Progress phải được làm tròn còn 42.9%");
    }

    // -------------------------------------------------
    // EDGE CASES
    // -------------------------------------------------

    /**
     * EC_01:
     * Customer tồn tại nhưng không có enrollment.
     * --> trả về list rỗng.
     *
     * Code path:
     *   if (enrollments == null || enrollments.isEmpty()) return result;
     */
    @Test
    void shouldReturnEmptyList_whenCustomerHasNoEnrollments() {
        // =========================
        // Given
        // =========================
        String email = "empty@example.com";

        Customer customer = createCustomerWithEnrollments(Collections.emptyList());

        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));

        // =========================
        // When
        // =========================
        List<CourseLearningCardDto> result = courseInfService.retrieveYourCourse(email);

        // =========================
        // Then
        assertEquals(0, result.size(),
                "Nếu user không có enrollment thì kết quả phải là list rỗng");
    }

    /**
     * EC_02:
     * totalContents = 0 -> progress phải = 0.0 (tránh chia cho 0).
     *
     * Để totalContents=0:
     *   - Course với chapters = null (=> countTotalContentsByType trả map rỗng => total 0)
     *
     * completedContents có thể là bất kỳ, method calculateContentProgress sẽ trả về (0,0)
     * và retrieveYourCourse sẽ gán progress = 0.0.
     */
    @Test
    void shouldReturnZeroProgress_whenCourseHasNoContents() {
        // =========================
        // Given
        // =========================
        String email = "nocontent@example.com";

        // Course không có chapters => totalContents = 0 trong calculateContentProgress
        Course emptyCourse = new Course();
        emptyCourse.setName("Empty Course");
        emptyCourse.setChapters(null);

        Enrollment enrollment = createEnrollment(emptyCourse);

        Customer customer = createCustomerWithEnrollments(Collections.singletonList(enrollment));
        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));

        // Khi totalContents=0, calculateContentProgress sẽ short-circuit và không gọi contentRepository
        // Vì vậy nội dung mock bên dưới chỉ để an toàn, nhưng thực ra sẽ không được dùng.
        when(contentRepository.findByEnrollment(any(Enrollment.class)))
                .thenReturn(Collections.emptyList());

        // =========================
        // When
        // =========================
        List<CourseLearningCardDto> result = courseInfService.retrieveYourCourse(email);

        // =========================
        // Then
        assertEquals(1, result.size(),
                "Phải trả về 1 DTO (1 enrollment)");
        assertEquals(0.0, result.get(0).getProgress(), 0.0001,
                "Khi totalContents=0 thì progress phải là 0.0, tránh chia cho 0");
    }

    /**
     * EC_03 (tuỳ chọn):
     * Một enrollment ném lỗi trong quá trình tính progress,
     * enrollment còn lại vẫn trả về bình thường.
     *
     * Hành vi code:
     *   try {
     *      ... calculateContentProgress ...
     *      result.add(dto);
     *   } catch (Exception e) {
     *      log.error(...); // không throw ra ngoài
     *   }
     *
     * Kịch bản:
     *   - Enrollment A: contentRepository.findByEnrollment(enrollmentA) ném RuntimeException("DB down")
     *   - Enrollment B: hoạt động bình thường (progress 100%)
     *
     * Kỳ vọng:
     *   - Hàm KHÔNG ném exception ra ngoài
     *   - Chỉ trả về DTO cho Enrollment B
     */
    @Test
    void shouldSkipEnrollmentThatThrowsAndStillReturnOthers_whenOneEnrollmentCausesError() {
        // =========================
        // Given
        // =========================
        String email = "partial@example.com";

        // Enrollment A: total=4, DB sẽ nổ khi tính completed
        Module aModule = createModule(
                TypeOfContent.VIDEO, TypeOfContent.VIDEO,
                TypeOfContent.MULTIPLE_CHOICE, TypeOfContent.MULTIPLE_CHOICE
        );
        Course courseA = createCourse("Course A", createChapter(aModule));
        Enrollment enrollmentA = createEnrollment(courseA);

        // Enrollment B: total=4, completed=4 => 100%
        Module bModule = createModule(
                TypeOfContent.READING, TypeOfContent.READING,
                TypeOfContent.READING, TypeOfContent.READING
        );
        Course courseB = createCourse("Course B", createChapter(bModule));
        Enrollment enrollmentB = createEnrollment(courseB);

        Customer customer = createCustomerWithEnrollments(Arrays.asList(enrollmentA, enrollmentB));
        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));

        // Mock cho enrollment A: contentRepository.findByEnrollment(enrollmentA) ném lỗi
        when(contentRepository.findByEnrollment(enrollmentA))
                .thenThrow(new RuntimeException("DB down"));

        // Mock cho enrollment B: thành công với completed=4
        List<CustomerModuleContent> completedB = Arrays.asList(
                cmcOf(TypeOfContent.READING),
                cmcOf(TypeOfContent.READING),
                cmcOf(TypeOfContent.READING),
                cmcOf(TypeOfContent.READING)
        );
        when(contentRepository.findByEnrollment(enrollmentB)).thenReturn(completedB);

        // =========================
        // When
        // =========================
        List<CourseLearningCardDto> result = courseInfService.retrieveYourCourse(email);

        // =========================
        // Then
        // Enrollment A gây lỗi -> bị skip -> chỉ còn Enrollment B
        assertEquals(1, result.size(),
                "Enrollment nổ lỗi phải bị bỏ qua, chỉ trả về DTO của enrollment còn lại");

        assertEquals(100.0, result.get(0).getProgress(), 0.0001,
                "Enrollment B có total=4 completed=4 => progress phải 100%");
    }

    // -------------------------------------------------
    // ERROR SCENARIOS
    // -------------------------------------------------

    /**
     * ES_01:
     * Email không tồn tại -> RuntimeException("Customer not found with email: ...")
     *
     * Đây là test giống TC_ES_01 đã viết trước,
     * mình để lại luôn để class này tự đủ coverage từ đầu đến cuối.
     */
    @Test
    void shouldThrowRuntimeException_whenCustomerNotFoundByEmail() {
        // =========================
        // Given
        // =========================
        String email = "missing@example.com";

        when(customerRepository.findByEmail(email)).thenReturn(Optional.empty());

        // =========================
        // When / Then
        // =========================
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> courseInfService.retrieveYourCourse(email),
                "Nếu không tìm thấy customer theo email thì phải ném RuntimeException"
        );

        String expectedMsg = "Customer not found with email: " + email;
        if (ex.getMessage() != null) {
            org.junit.jupiter.api.Assertions.assertTrue(
                    ex.getMessage().contains(expectedMsg),
                    "Message RuntimeException phải chứa email không tồn tại"
            );
        }
    }
}
