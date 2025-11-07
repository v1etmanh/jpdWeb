package com.jpd.web.nguyen.unit.service.courseInf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
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
import com.jpd.web.service.CourseInfService;
import com.jpd.web.service.utils.ValidationResources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests cho private method:
 * <p>
 * private ContentProgressInfo calculateContentProgress(Course course, Enrollment enrollment)
 * <p>
 * trong CourseInfService.
 * <p>
 * Bao phủ:
 * - Happy Paths
 * - Edge Cases
 * - Error Scenarios
 * <p>
 * Cách test:
 * - Build Course thủ công để kiểm soát totalContentsByType (đếm tổng content của course).
 * - Mock contentRepository.findByEnrollment(...) để kiểm soát completedContentsByType (đếm content user đã hoàn thành).
 * - Gọi method private qua Reflection.
 * - Lấy kết quả ContentProgressInfo (private record) qua Reflection để assert totalContents và completedContents.
 */
class calculateContentProgressTest {

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

    // ==========================================================
    // ===== Helpers để build dữ liệu giả cho Course & CMC ======
    // ==========================================================

    /**
     * Vì ModuleContent là abstract class nên ta tạo subclass vô danh và set typeOfContent.
     * Giả định ModuleContent có setter setTypeOfContent(TypeOfContent).
     */
    private ModuleContent createModuleContent(TypeOfContent type) {
        ModuleContent mc = new ModuleContent() {
            // Nếu ModuleContent có abstract method khác thì override tại đây.
        };
        mc.setTypeOfContent(type);
        return mc;
    }

    /**
     * Tạo Module với danh sách ModuleContent có các type tương ứng.
     * Ví dụ createModule(VIDEO, MULTIPLE_CHOICE) => Module có 2 ModuleContent: VIDEO, MULTIPLE_CHOICE.
     */
    private Module createModule(TypeOfContent... types) {
        Module module = new Module();
        List<ModuleContent> contents = new ArrayList<>();
        for (TypeOfContent t : types) {
            contents.add(createModuleContent(t));
        }
        module.setModuleContent(contents);
        return module;
    }

    /**
     * Tạo Chapter với các Module truyền vào.
     */
    private Chapter createChapter(Module... modules) {
        Chapter chapter = new Chapter();
        chapter.setModules(Arrays.asList(modules));
        return chapter;
    }

    /**
     * Tạo Course với các Chapter truyền vào.
     */
    private Course createCourse(Chapter... chapters) {
        Course course = new Course();
        course.setChapters(Arrays.asList(chapters));
        return course;
    }

    /**
     * Tạo Course không có chapter (để totalContents == 0).
     * countTotalContentsByType sẽ trả về map rỗng.
     */
    private Course createEmptyCourseNoContents() {
        Course course = new Course();
        course.setChapters(null); // course.getChapters() == null
        return course;
    }

    /**
     * Tạo CustomerModuleContent với set<TypeOfContent> đã hoàn thành.
     * Ví dụ: cmcWithTypes({VIDEO, MULTIPLE_CHOICE})
     */
    private CustomerModuleContent cmcWithTypes(Set<TypeOfContent> types) {
        CustomerModuleContent cmc = new CustomerModuleContent();
        cmc.setTypeOfContent(types);
        return cmc;
    }

    /**
     * Tạo CustomerModuleContent với 1..n TypeOfContent, sinh HashSet.
     * Ví dụ: cmcWithTypes(TypeOfContent.VIDEO, TypeOfContent.MULTIPLE_CHOICE)
     */
    private CustomerModuleContent cmcWithTypes(TypeOfContent... types) {
        if (types == null) {
            return cmcWithTypes((Set<TypeOfContent>) null);
        }
        return cmcWithTypes(new HashSet<>(Arrays.asList(types)));
    }

    // ==========================================================
    // ========== Helpers Reflection cho method & record ========
    // ==========================================================

    /**
     * Gọi private method calculateContentProgress(course, enrollment) bằng Reflection.
     */
    private Object invokeCalculateContentProgress(Course course, Enrollment enrollment) throws Exception {
        Method m = CourseInfService.class.getDeclaredMethod(
                "calculateContentProgress",
                com.jpd.web.model.Course.class,
                com.jpd.web.model.Enrollment.class
        );
        m.setAccessible(true);
        return m.invoke(courseInfService, course, enrollment);
    }

    /**
     * Lấy field totalContents từ private record ContentProgressInfo bằng Reflection.
     */
    private int extractTotalContents(Object progressInfoObj) throws Exception {
        Class<?> infoClass = progressInfoObj.getClass();
        Field totalField = infoClass.getDeclaredField("totalContents");
        totalField.setAccessible(true);
        return (int) totalField.get(progressInfoObj);
    }

    /**
     * Lấy field completedContents từ private record ContentProgressInfo bằng Reflection.
     */
    private int extractCompletedContents(Object progressInfoObj) throws Exception {
        Class<?> infoClass = progressInfoObj.getClass();
        Field completedField = infoClass.getDeclaredField("completedContents");
        completedField.setAccessible(true);
        return (int) completedField.get(progressInfoObj);
    }

    // ==========================================================
    // ===================== HAPPY PATHS ========================
    // ==========================================================

    /**
     * TC_HP_01:
     * totalContents = 5, completed = 3
     * <p>
     * Course:
     * - VIDEO x3
     * - MULTIPLE_CHOICE x2
     * => totalContents = 5
     * <p>
     * Completed:
     * - VIDEO = 2
     * - MULTIPLE_CHOICE  = 1
     * => completedContents = 2 + 1 = 3
     * <p>
     * Không cần Math.min cắt bớt vì completedCount <= totalCount từng loại.
     */
    @Test
    void shouldReturnCorrectProgress_whenUserCompletedSomeContents() throws Exception {
        // =========================
        // Given
        // =========================
        // Course có tổng VIDEO=3, MULTIPLE_CHOICE=2  => total=5
        Module module1 = createModule(TypeOfContent.VIDEO, TypeOfContent.VIDEO, TypeOfContent.MULTIPLE_CHOICE);
        Module module2 = createModule(TypeOfContent.VIDEO, TypeOfContent.MULTIPLE_CHOICE);
        Chapter chapter = createChapter(module1, module2);
        Course course = createCourse(chapter);

        Enrollment enrollment = new Enrollment();

        // Completed map mong muốn:
        // VIDEO=2, MULTIPLE_CHOICE=1 thông qua danh sách progress:
        //   cmc1 -> {VIDEO, MULTIPLE_CHOICE}
        //   cmc2 -> {VIDEO}
        List<CustomerModuleContent> progressList = Arrays.asList(
                cmcWithTypes(TypeOfContent.VIDEO, TypeOfContent.MULTIPLE_CHOICE),
                cmcWithTypes(TypeOfContent.VIDEO)
        );

        when(contentRepository.findByEnrollment(any(Enrollment.class)))
                .thenReturn(progressList);

        // =========================
        // When
        // =========================
        Object result = invokeCalculateContentProgress(course, enrollment);
        int totalContents = extractTotalContents(result);
        int completedContents = extractCompletedContents(result);

        // =========================
        // Then
        // =========================
        assertEquals(5, totalContents,
                "Tổng content trong course phải là 5 (VIDEO=3, MULTIPLE_CHOICE=2)");
        assertEquals(3, completedContents,
                "Completed phải là 3 (VIDEO=2 + MULTIPLE_CHOICE=1)");
    }

    /**
     * TC_HP_02:
     * User hoàn thành tất cả nội dung.
     * <p>
     * Course:
     * READING x4 => totalContents = 4
     * <p>
     * Completed:
     * READING = 4
     * => completedContents = 4
     */
    @Test
    void shouldReturnFullCompletion_whenUserCompletedAllContents() throws Exception {
        // =========================
        // Given
        // =========================
        // Course có 4 READING
        Module m1 = createModule(TypeOfContent.READING, TypeOfContent.READING);
        Module m2 = createModule(TypeOfContent.READING, TypeOfContent.READING);
        Chapter c1 = createChapter(m1, m2);
        Course course = createCourse(c1);

        Enrollment enrollment = new Enrollment();

        // Completed map: {READING=4}
        List<CustomerModuleContent> progressList = Arrays.asList(
                cmcWithTypes(TypeOfContent.READING),
                cmcWithTypes(TypeOfContent.READING),
                cmcWithTypes(TypeOfContent.READING),
                cmcWithTypes(TypeOfContent.READING)
        );

        when(contentRepository.findByEnrollment(any(Enrollment.class)))
                .thenReturn(progressList);

        // =========================
        // When
        // =========================
        Object result = invokeCalculateContentProgress(course, enrollment);
        int totalContents = extractTotalContents(result);
        int completedContents = extractCompletedContents(result);

        // =========================
        // Then
        // =========================
        assertEquals(4, totalContents,
                "Tổng content trong course phải là 4 (READING=4)");
        assertEquals(4, completedContents,
                "Completed phải là 4 (user hoàn thành toàn bộ READING)");
    }

    // ==========================================================
    // ===================== EDGE CASES =========================
    // ==========================================================

    /**
     * TC_EC_01:
     * totalContents == 0  => course chưa có nội dung.
     * <p>
     * Hành vi:
     * - Hàm return ngay ContentProgressInfo(0,0)
     * - KHÔNG tiếp tục gọi countCompletedContentsByType.
     * <p>
     * Kỳ vọng:
     * total=0, completed=0
     */
    @Test
    void shouldReturnZeroZero_whenCourseHasNoContents() throws Exception {
        // =========================
        // Given
        // =========================
        Course emptyCourse = createEmptyCourseNoContents(); // chapters = null
        Enrollment enrollment = new Enrollment(); // không quan trọng vì sẽ return sớm

        // Không cần mock contentRepository vì nhánh totalContents==0 return sớm

        // =========================
        // When
        // =========================
        Object result = invokeCalculateContentProgress(emptyCourse, enrollment);
        int totalContents = extractTotalContents(result);
        int completedContents = extractCompletedContents(result);

        // =========================
        // Then
        // =========================
        assertEquals(0, totalContents,
                "totalContents phải bằng 0 khi course không có nội dung");
        assertEquals(0, completedContents,
                "completedContents cũng phải bằng 0 trong trường hợp này");
    }

    /**
     * TC_EC_02:
     * totalContents > 0 nhưng user chưa hoàn thành gì.
     * <p>
     * Course:
     * VIDEO=3, MULTIPLE_CHOICE=2 => total=5
     * <p>
     * CompletedProgress:
     * (empty list) => map rỗng {}
     * <p>
     * Kỳ vọng:
     * total=5
     * completed=0
     */
    @Test
    void shouldReturnZeroCompleted_whenUserHasNoProgress() throws Exception {
        // =========================
        // Given
        // =========================
        // Course có VIDEO=3, MULTIPLE_CHOICE=2 => total 5
        Module module1 = createModule(TypeOfContent.VIDEO, TypeOfContent.VIDEO, TypeOfContent.MULTIPLE_CHOICE);
        Module module2 = createModule(TypeOfContent.VIDEO, TypeOfContent.MULTIPLE_CHOICE);
        Chapter chapter = createChapter(module1, module2);
        Course course = createCourse(chapter);

        Enrollment enrollment = new Enrollment();

        // Repository trả về emptyList => completed map rỗng {}
        when(contentRepository.findByEnrollment(any(Enrollment.class)))
                .thenReturn(Collections.emptyList());

        // =========================
        // When
        // =========================
        Object result = invokeCalculateContentProgress(course, enrollment);
        int totalContents = extractTotalContents(result);
        int completedContents = extractCompletedContents(result);

        // =========================
        // Then
        // =========================
        assertEquals(5, totalContents,
                "totalContents phải bằng 5 (VIDEO=3, MULTIPLE_CHOICE=2)");
        assertEquals(0, completedContents,
                "completedContents phải bằng 0 khi user chưa hoàn thành gì");
    }

    /**
     * TC_EC_03:
     * User hoàn thành nhiều hơn số content thực tế của course.
     * <p>
     * Course:
     * VIDEO=2, MULTIPLE_CHOICE=3 => total=5
     * <p>
     * CompletedProgress (theo repository):
     * VIDEO=5
     * MULTIPLE_CHOICE=1
     * <p>
     * Logic:
     * actualCompleted(VIDEO) = min(5,2) = 2
     * actualCompleted(MULTIPLE_CHOICE)  = min(1,3) = 1
     * completed = 2 + 1 = 3
     * <p>
     * Kỳ vọng:
     * total=5
     * completed=3  (được cắt bằng Math.min)
     */
    @Test
    void shouldClampCompletedCountByTotalUsingMathMin_whenUserProgressExceedsTotal() throws Exception {
        // =========================
        // Given
        // =========================
        // Course có VIDEO=2, MULTIPLE_CHOICE=3 => tổng = 5
        Module moduleA = createModule(TypeOfContent.VIDEO, TypeOfContent.VIDEO);
        Module moduleB = createModule(TypeOfContent.MULTIPLE_CHOICE, TypeOfContent.MULTIPLE_CHOICE,
                TypeOfContent.MULTIPLE_CHOICE);
        Chapter chapter = createChapter(moduleA, moduleB);
        Course course = createCourse(chapter);

        Enrollment enrollment = new Enrollment();

        // Giả lập completedCounts:
        // VIDEO=5 -> 5 CMC mỗi cái {VIDEO}
        // MULTIPLE_CHOICE=1  -> 1 CMC {MULTIPLE_CHOICE}
        List<CustomerModuleContent> progressList = new ArrayList<>();
        progressList.add(cmcWithTypes(TypeOfContent.MULTIPLE_CHOICE)); // MULTIPLE_CHOICE=1
        progressList.add(cmcWithTypes(TypeOfContent.VIDEO));
        progressList.add(cmcWithTypes(TypeOfContent.VIDEO));
        progressList.add(cmcWithTypes(TypeOfContent.VIDEO));
        progressList.add(cmcWithTypes(TypeOfContent.VIDEO));
        progressList.add(cmcWithTypes(TypeOfContent.VIDEO)); // tổng VIDEO=5

        when(contentRepository.findByEnrollment(any(Enrollment.class)))
                .thenReturn(progressList);

        // =========================
        // When
        // =========================
        Object result = invokeCalculateContentProgress(course, enrollment);
        int totalContents = extractTotalContents(result);
        int completedContents = extractCompletedContents(result);

        // =========================
        // Then
        // =========================
        assertEquals(5, totalContents,
                "totalContents phải bằng 5 (VIDEO=2, MULTIPLE_CHOICE=3)");
        assertEquals(3, completedContents,
                "completedContents phải bằng 3 sau Math.min: 2(VIDEO) + 1(MULTIPLE_CHOICE)");
    }

    /**
     * TC_EC_04:
     * User hoàn thành loại content mà course KHÔNG có.
     * <p>
     * Course:
     * VIDEO=4 => total=4
     * <p>
     * CompletedProgress:
     * MULTIPLE_CHOICE=10
     * <p>
     * Vì course không có MULTIPLE_CHOICE:
     * totalCount cho MULTIPLE_CHOICE = 0
     * min(10, 0) = 0
     * <p>
     * Kỳ vọng:
     * total=4
     * completed=0
     */
    @Test
    void shouldIgnoreCompletedTypesThatDoNotExistInCourse() throws Exception {
        // =========================
        // Given
        // =========================
        // Course có VIDEO=4 => tổng = 4
        Module videoModule1 = createModule(TypeOfContent.VIDEO, TypeOfContent.VIDEO);
        Module videoModule2 = createModule(TypeOfContent.VIDEO, TypeOfContent.VIDEO);
        Chapter chapter = createChapter(videoModule1, videoModule2);
        Course course = createCourse(chapter);

        Enrollment enrollment = new Enrollment();

        // CompletedProgress: chỉ MULTIPLE_CHOICE rất nhiều lần (MULTIPLE_CHOICE=10)
        List<CustomerModuleContent> progressList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            progressList.add(cmcWithTypes(TypeOfContent.MULTIPLE_CHOICE));
        }

        when(contentRepository.findByEnrollment(any(Enrollment.class)))
                .thenReturn(progressList);

        // =========================
        // When
        // =========================
        Object result = invokeCalculateContentProgress(course, enrollment);
        int totalContents = extractTotalContents(result);
        int completedContents = extractCompletedContents(result);

        // =========================
        // Then
        // =========================
        assertEquals(4, totalContents,
                "totalContents phải bằng 4 (VIDEO=4)");
        assertEquals(0, completedContents,
                "completedContents phải là 0 vì MULTIPLE_CHOICE không tồn tại trong course");
    }

    // ==========================================================
    // ================== ERROR SCENARIOS =======================
    // ==========================================================

    /**
     * TC_ES_01:
     * course = null
     * <p>
     * Hành vi hiện tại:
     * calculateContentProgress gọi countTotalContentsByType(course)
     * -> hàm kia sẽ gọi course.getChapters()
     * -> NPE vì course == null
     * <p>
     * Kỳ vọng:
     * Ném NullPointerException (bubble ra trong InvocationTargetException.getCause()).
     */
    @Test
    void shouldThrowNullPointerException_whenCourseIsNull() throws Exception {
        // =========================
        // Given
        // =========================
        Course nullCourse = null;
        Enrollment enrollment = new Enrollment();

        Method m = CourseInfService.class.getDeclaredMethod(
                "calculateContentProgress",
                com.jpd.web.model.Course.class,
                com.jpd.web.model.Enrollment.class
        );
        m.setAccessible(true);

        // =========================
        // When / Then
        // =========================
        try {
            m.invoke(courseInfService, nullCourse, enrollment);
            fail("Kỳ vọng NullPointerException khi course = null");
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause instanceof NullPointerException,
                    "Cause phải là NullPointerException khi course == null");
        }
    }

    /**
     * TC_ES_02:
     * enrollment = null (và totalContents > 0)
     * <p>
     * Hành vi:
     * - totalContents > 0 nên code tiếp tục gọi countCompletedContentsByType(null)
     * - Bên trong countCompletedContentsByType:
     * List<CustomerModuleContent> customerProgress = contentRepository.findByEnrollment(enrollment);
     * Nếu repo trả về null
     * -> vòng for (for (CustomerModuleContent cmc : customerProgress)) sẽ NPE
     * <p>
     * Kỳ vọng:
     * - Gây NullPointerException bubble lên.
     */
    @Test
    void shouldThrowNullPointerException_whenEnrollmentIsNullAndRepoReturnsNull() throws Exception {
        // =========================
        // Given
        // =========================
        // Course có nội dung để tránh nhánh totalContents==0
        Module module = createModule(TypeOfContent.VIDEO);
        Chapter chapter = createChapter(module);
        Course nonEmptyCourse = createCourse(chapter);

        Enrollment nullEnrollment = null;

        // Khi enrollment = null, mock repo trả về null list => gây NPE trong countCompletedContentsByType
        when(contentRepository.findByEnrollment((Enrollment) null))
                .thenReturn(null);

        Method m = CourseInfService.class.getDeclaredMethod(
                "calculateContentProgress",
                com.jpd.web.model.Course.class,
                com.jpd.web.model.Enrollment.class
        );
        m.setAccessible(true);

        // =========================
        // When / Then
        // =========================
        try {
            m.invoke(courseInfService, nonEmptyCourse, nullEnrollment);
            fail("Kỳ vọng NullPointerException khi enrollment = null và repo trả về null");
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause instanceof NullPointerException,
                    "Cause phải là NullPointerException khi enrollment null dẫn tới repo trả null list");
        }
    }

    /**
     * TC_ES_03:
     * Repository ném RuntimeException("DB down")
     * <p>
     * Hành vi:
     * - totalContents > 0 nên method sẽ gọi countCompletedContentsByType(enrollment)
     * - countCompletedContentsByType gọi contentRepository.findByEnrollment(enrollment)
     * - Repo ném RuntimeException
     * - calculateContentProgress KHÔNG catch => bubble lên
     * <p>
     * Kỳ vọng:
     * RuntimeException với message "DB down"
     */
    @Test
    void shouldPropagateRuntimeException_whenRepositoryThrowsDuringProgressFetch() throws Exception {
        // =========================
        // Given
        // =========================
        // Course có nội dung để đi qua nhánh totalContents > 0
        Module module = createModule(TypeOfContent.VIDEO, TypeOfContent.MULTIPLE_CHOICE);
        Chapter chapter = createChapter(module);
        Course course = createCourse(chapter);

        Enrollment enrollment = new Enrollment();

        RuntimeException boom = new RuntimeException("DB down");
        when(contentRepository.findByEnrollment(any(Enrollment.class)))
                .thenThrow(boom);

        Method m = CourseInfService.class.getDeclaredMethod(
                "calculateContentProgress",
                com.jpd.web.model.Course.class,
                com.jpd.web.model.Enrollment.class
        );
        m.setAccessible(true);

        // =========================
        // When / Then
        // =========================
        try {
            m.invoke(courseInfService, course, enrollment);
            fail("Kỳ vọng RuntimeException bubble lên khi repo ném lỗi");
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause instanceof RuntimeException,
                    "Cause phải là RuntimeException khi DB down");
            assertEquals("DB down", cause.getMessage(),
                    "Thông điệp lỗi phải khớp với RuntimeException gốc");
        }
    }
}
