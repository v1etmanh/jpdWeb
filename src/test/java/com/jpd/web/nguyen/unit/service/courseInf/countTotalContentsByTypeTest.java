package com.jpd.web.nguyen.unit.service.courseInf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
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
 * Unit tests cho private method countTotalContentsByType(Course course) trong CourseInfService.
 * Bao phủ:
 *  - Happy Paths
 *  - Edge Cases
 *  - Error Scenarios
 *
 * Kỹ thuật:
 *  - Tự tạo object graph Course -> Chapter -> Module -> ModuleContent
 *  - Gọi method private bằng Reflection
 *  - Vì ModuleContent là abstract, dùng anonymous subclass trong helper createContent(...)
 */
class countTotalContentsByTypeTest {

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

    // -------------------------------------------------------------------------
    // Helpers dùng chung
    // -------------------------------------------------------------------------

    /**
     * Vì ModuleContent là abstract nên ta tạo subclass vô danh và gán type.
     * Giả định ModuleContent có setter setTypeOfContent(TypeOfContent).
     * Nếu ModuleContent có abstract method khác, bạn override trong { }.
     */
    private ModuleContent createContent(TypeOfContent type) {
        ModuleContent mc = new ModuleContent() {
            // override abstract methods here if needed
        };
        mc.setTypeOfContent(type);
        return mc;
    }

    // Tạo Module từ danh sách content
    private Module createModule(ModuleContent... contents) {
        Module m = new Module();
        if (contents == null) {
            m.setModuleContent(null);
        } else {
            m.setModuleContent(Arrays.asList(contents));
        }
        return m;
    }

    // Tạo Chapter từ danh sách module
    private Chapter createChapter(Module... modules) {
        Chapter ch = new Chapter();
        if (modules == null) {
            ch.setModules(null);
        } else {
            ch.setModules(Arrays.asList(modules));
        }
        return ch;
    }

    // Tạo Course từ danh sách chapter
    private Course createCourse(Chapter... chapters) {
        Course c = new Course();
        if (chapters == null) {
            c.setChapters(null);
        } else {
            c.setChapters(Arrays.asList(chapters));
        }
        return c;
    }

    // Gọi private method countTotalContentsByType qua Reflection
    @SuppressWarnings("unchecked")
    private Map<TypeOfContent, Integer> invokeCountTotalContentsByType(Course course) throws Exception {
        Method m = CourseInfService.class.getDeclaredMethod(
                "countTotalContentsByType",
                com.jpd.web.model.Course.class
        );
        m.setAccessible(true);
        return (Map<TypeOfContent, Integer>) m.invoke(courseInfService, course);
    }

    // -------------------------------------------------------------------------
    // HAPPY PATHS
    // -------------------------------------------------------------------------

    /**
     * TC_HP_01:
     * Course có nhiều chapter/module/content hợp lệ.
     *
     * Cấu trúc:
     *  Chapter 1
     *    Module 1.1 -> VIDEO, VIDEO, MULTIPLE_CHOICE
     *    Module 1.2 -> VIDEO
     *  Chapter 2
     *    Module 2.1 -> MULTIPLE_CHOICE
     *
     * Kỳ vọng:
     *  VIDEO = 3
     *  MULTIPLE_CHOICE  = 2
     */
    @Test
    void shouldReturnCorrectVideoAndQuizCount_whenCourseHasMultipleChaptersAndModules() throws Exception {
        // =========================
        // Given
        // =========================
        ModuleContent videoA = createContent(TypeOfContent.VIDEO);
        ModuleContent videoB = createContent(TypeOfContent.VIDEO);
        ModuleContent quizA  = createContent(TypeOfContent.MULTIPLE_CHOICE);

        Module module11 = createModule(videoA, videoB, quizA); // VIDEO, VIDEO, MULTIPLE_CHOICE

        ModuleContent videoC = createContent(TypeOfContent.VIDEO);
        Module module12 = createModule(videoC); // VIDEO

        Chapter chapter1 = createChapter(module11, module12);

        ModuleContent quizB = createContent(TypeOfContent.MULTIPLE_CHOICE);
        Module module21 = createModule(quizB); // MULTIPLE_CHOICE

        Chapter chapter2 = createChapter(module21);

        Course course = createCourse(chapter1, chapter2);

        // =========================
        // When
        // =========================
        Map<TypeOfContent, Integer> resultMap = invokeCountTotalContentsByType(course);

        // =========================
        // Then
        // =========================
        assertEquals(2, resultMap.size(),
                "Map phải chỉ chứa VIDEO và MULTIPLE_CHOICE");

        assertEquals(3,
                resultMap.get(TypeOfContent.VIDEO),
                "VIDEO phải đếm là 3 (2 từ module11 + 1 từ module12)");

        assertEquals(2,
                resultMap.get(TypeOfContent.MULTIPLE_CHOICE),
                "MULTIPLE_CHOICE phải đếm là 2 (1 từ module11 + 1 từ module21)");
    }

    /**
     * TC_HP_02:
     * Tất cả content đều là VIDEO (4 content VIDEO).
     *
     * Kỳ vọng:
     *  { VIDEO = 4 }
     */
    @Test
    void shouldReturnOnlyVideoCount_whenAllContentsAreVideo() throws Exception {
        // =========================
        // Given
        // =========================
        ModuleContent v1 = createContent(TypeOfContent.VIDEO);
        ModuleContent v2 = createContent(TypeOfContent.VIDEO);
        ModuleContent v3 = createContent(TypeOfContent.VIDEO);
        ModuleContent v4 = createContent(TypeOfContent.VIDEO);

        Module module = createModule(v1, v2, v3, v4);
        Chapter chapter = createChapter(module);
        Course course = createCourse(chapter);

        // =========================
        // When
        // =========================
        Map<TypeOfContent, Integer> resultMap = invokeCountTotalContentsByType(course);

        // =========================
        // Then
        // =========================
        assertEquals(1, resultMap.size(),
                "Map chỉ nên có 1 key duy nhất: VIDEO");

        assertEquals(4,
                resultMap.get(TypeOfContent.VIDEO),
                "VIDEO phải đếm là 4");
    }

    /**
     * TC_HP_03:
     * Chỉ 1 ModuleContent duy nhất có type MULTIPLE_CHOICE.
     *
     * Kỳ vọng:
     *  { MULTIPLE_CHOICE = 1 }
     */
    @Test
    void shouldReturnSingleQuizCount_whenCourseHasOnlyOneQuizContent() throws Exception {
        // =========================
        // Given
        // =========================
        ModuleContent quiz = createContent(TypeOfContent.MULTIPLE_CHOICE);

        Module module = createModule(quiz);
        Chapter chapter = createChapter(module);
        Course course = createCourse(chapter);

        // =========================
        // When
        // =========================
        Map<TypeOfContent, Integer> resultMap = invokeCountTotalContentsByType(course);

        // =========================
        // Then
        // =========================
        assertEquals(1, resultMap.size(),
                "Map chỉ nên có 1 key duy nhất: MULTIPLE_CHOICE");

        assertEquals(1,
                resultMap.get(TypeOfContent.MULTIPLE_CHOICE),
                "MULTIPLE_CHOICE phải đếm là 1");
    }

    // -------------------------------------------------------------------------
    // EDGE CASES
    // -------------------------------------------------------------------------

    /**
     * TC_EC_01:
     * course.getChapters() == null
     *
     * Kỳ vọng:
     *  -> map rỗng {}
     */
    @Test
    void shouldReturnEmptyMap_whenCourseHasNoChapters() throws Exception {
        // =========================
        // Given
        // =========================
        Course course = createCourse((Chapter[]) null); // setChapters(null)

        // =========================
        // When
        // =========================
        Map<TypeOfContent, Integer> resultMap = invokeCountTotalContentsByType(course);

        // =========================
        // Then
        // =========================
        assertEquals(0, resultMap.size(),
                "Khi course.getChapters() == null thì map phải rỗng");
    }

    /**
     * TC_EC_02:
     * Có chapter, nhưng chapter.getModules() == null
     *
     * Kỳ vọng:
     *  -> map rỗng {}
     */
    @Test
    void shouldReturnEmptyMap_whenChapterHasNullModules() throws Exception {
        // =========================
        // Given
        // =========================
        Chapter chapterWithoutModules = createChapter((Module[]) null); // setModules(null)
        Course course = createCourse(chapterWithoutModules);

        // =========================
        // When
        // =========================
        Map<TypeOfContent, Integer> resultMap = invokeCountTotalContentsByType(course);

        // =========================
        // Then
        // =========================
        assertEquals(0, resultMap.size(),
                "Khi chapter.getModules() == null thì không có content nào được đếm");
    }

    /**
     * TC_EC_03:
     * Có module, nhưng module.getModuleContent() == null
     *
     * Kỳ vọng:
     *  -> map rỗng {}
     */
    @Test
    void shouldReturnEmptyMap_whenModuleHasNullContentList() throws Exception {
        // =========================
        // Given
        // =========================
        Module moduleWithoutContent = createModule((ModuleContent[]) null); // setModuleContent(null)
        Chapter chapter = createChapter(moduleWithoutContent);
        Course course = createCourse(chapter);

        // =========================
        // When
        // =========================
        Map<TypeOfContent, Integer> resultMap = invokeCountTotalContentsByType(course);

        // =========================
        // Then
        // =========================
        assertEquals(0, resultMap.size(),
                "Khi module.getModuleContent() == null thì map phải rỗng");
    }

    /**
     * TC_EC_04:
     * Có content nhưng có phần tử có typeOfContent == null.
     * Ví dụ list: [VIDEO, null, MULTIPLE_CHOICE]
     *
     * Kỳ vọng:
     *  -> { VIDEO=1, MULTIPLE_CHOICE=1 }
     *  -> Content có type=null bị bỏ qua (không gây crash, không tạo key null)
     */
    @Test
    void shouldSkipNullTypeContent_whenSomeContentsHaveNullType() throws Exception {
        // =========================
        // Given
        // =========================
        ModuleContent video = createContent(TypeOfContent.VIDEO);
        ModuleContent noType = createContent(null); // setTypeOfContent(null)
        ModuleContent quiz  = createContent(TypeOfContent.MULTIPLE_CHOICE);

        Module module = createModule(video, noType, quiz);
        Chapter chapter = createChapter(module);
        Course course = createCourse(chapter);

        // =========================
        // When
        // =========================
        Map<TypeOfContent, Integer> resultMap = invokeCountTotalContentsByType(course);

        // =========================
        // Then
        // =========================
        assertEquals(2, resultMap.size(),
                "Chỉ nên có VIDEO và MULTIPLE_CHOICE, không nên có key null");

        assertEquals(1,
                resultMap.get(TypeOfContent.VIDEO),
                "VIDEO phải đếm là 1");

        assertEquals(1,
                resultMap.get(TypeOfContent.MULTIPLE_CHOICE),
                "MULTIPLE_CHOICE phải đếm là 1");
    }

    /**
     * TC_EC_05:
     * course.getChapters() trả về [chapter1, null, chapter2]
     *
     * Vì code không kiểm tra chapter == null, khi lặp:
     *   for (Chapter chapter : course.getChapters()) {
     *       if (chapter.getModules() == null) ...
     *   }
     * -> chapter = null => NullPointerException
     *
     * Kỳ vọng:
     *  -> Ném NullPointerException (thông qua InvocationTargetException.getCause()).
     */
    @Test
    void shouldThrowNullPointerException_whenChapterListContainsNullChapter() throws Exception {
        // =========================
        // Given
        // =========================
        // chapter1 với 1 VIDEO
        ModuleContent video = createContent(TypeOfContent.VIDEO);
        Module module1 = createModule(video);
        Chapter chapter1 = createChapter(module1);

        // chapter2 với 1 MULTIPLE_CHOICE
        ModuleContent quiz = createContent(TypeOfContent.MULTIPLE_CHOICE);
        Module module2 = createModule(quiz);
        Chapter chapter2 = createChapter(module2);

        // course.chapters = [chapter1, null, chapter2]
        Course course = new Course();
        course.setChapters(Arrays.asList(chapter1, null, chapter2));

        // =========================
        // When / Then
        // =========================
        try {
            invokeCountTotalContentsByType(course);
            fail("Kỳ vọng NullPointerException khi danh sách chapters chứa phần tử null");
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause instanceof NullPointerException,
                    "Cause phải là NullPointerException khi chapter == null");
        }
    }

    /**
     * TC_EC_06:
     * chapter.getModules() trả về [module1, null]
     *
     * Code không check module == null:
     *   for (Module module : chapter.getModules()) {
     *       if (module.getModuleContent() == null) ...
     *   }
     * -> module = null => NullPointerException
     *
     * Kỳ vọng:
     *  -> Ném NullPointerException
     */
    @Test
    void shouldThrowNullPointerException_whenModulesListContainsNullModule() throws Exception {
        // =========================
        // Given
        // =========================
        // module1 với VIDEO
        ModuleContent video = createContent(TypeOfContent.VIDEO);
        Module module1 = createModule(video);

        // modules = [module1, null]
        Chapter chapter = new Chapter();
        chapter.setModules(Arrays.asList(module1, null));

        Course course = createCourse(chapter);

        // =========================
        // When / Then
        // =========================
        try {
            invokeCountTotalContentsByType(course);
            fail("Kỳ vọng NullPointerException khi danh sách modules chứa phần tử null");
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause instanceof NullPointerException,
                    "Cause phải là NullPointerException khi module == null");
        }
    }

    // -------------------------------------------------------------------------
    // ERROR SCENARIOS
    // -------------------------------------------------------------------------

    /**
     * TC_ES_01:
     * Truyền course = null
     *
     * Ở dòng đầu tiên method gọi course.getChapters(), nên sẽ NPE.
     *
     * Kỳ vọng:
     *  -> NullPointerException
     */
    @Test
    void shouldThrowNullPointerException_whenCourseIsNull() throws Exception {
        // =========================
        // Given
        // =========================
        Course course = null;

        // =========================
        // When / Then
        // =========================
        try {
            invokeCountTotalContentsByType(course);
            fail("Kỳ vọng NullPointerException khi truyền course = null");
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause instanceof NullPointerException,
                    "Cause phải là NullPointerException khi course == null");
        }
    }

    /**
     * TC_ES_02:
     * Một phần tử chapter trong danh sách là null
     *
     * (Thực chất giống TC_EC_05, nhưng đây được xem là Error Scenario ở mức nghiệp vụ.)
     *
     * Kỳ vọng:
     *  -> NullPointerException
     */
    @Test
    void shouldThrowNullPointerException_errorScenario_whenChapterElementIsNull() throws Exception {
        // =========================
        // Given
        // =========================
        Chapter validChapter = createChapter(createModule(createContent(TypeOfContent.VIDEO)));

        Course course = new Course();
        course.setChapters(Arrays.asList(validChapter, null));

        // =========================
        // When / Then
        // =========================
        try {
            invokeCountTotalContentsByType(course);
            fail("Kỳ vọng NullPointerException khi một chapter trong danh sách là null (Error Scenario)");
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause instanceof NullPointerException,
                    "Cause phải là NullPointerException khi chapter null (Error Scenario)");
        }
    }

    /**
     * TC_ES_03:
     * Một phần tử module trong danh sách là null
     *
     * (Giống TC_EC_06 nhưng phân loại theo Error Scenario.)
     *
     * Kỳ vọng:
     *  -> NullPointerException
     */
    @Test
    void shouldThrowNullPointerException_errorScenario_whenModuleElementIsNull() throws Exception {
        // =========================
        // Given
        // =========================
        Module goodModule = createModule(createContent(TypeOfContent.MULTIPLE_CHOICE));

        Chapter chapter = new Chapter();
        chapter.setModules(Arrays.asList(goodModule, null));

        Course course = createCourse(chapter);

        // =========================
        // When / Then
        // =========================
        try {
            invokeCountTotalContentsByType(course);
            fail("Kỳ vọng NullPointerException khi một module trong danh sách là null (Error Scenario)");
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause instanceof NullPointerException,
                    "Cause phải là NullPointerException khi module null (Error Scenario)");
        }
    }
}
