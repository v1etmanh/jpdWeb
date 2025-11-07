package com.jpd.web.nguyen.unit.service.courseInf;

import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;
import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.dto.CourseDescriptionDto;
import com.jpd.web.service.CourseInfService;
import com.jpd.web.service.utils.ValidationResources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Unit tests cho public method:
 * CourseDescriptionDto getCourseDescription(long courseId)
 * <p>
 * Bao phủ toàn bộ scenario:
 * - Happy Path
 * - Error Scenarios
 * - Edge Cases (null / rỗng / các giá trị đặc biệt)
 * <p>
 * Chiến lược test:
 * - Mock các repository:
 * courseRepository, creatorRepository, enrollmentRepository
 * - Dựng dữ liệu domain thật (Course, Creator, Enrollment...) bằng setter.
 * - Gọi method public getCourseDescription(courseId) TRỰC TIẾP (không reflection).
 * - Assert vào DTO trả về:
 * courseId, name, totalStudents, totalFeedbacks, averageRating, totalModules,
 * creator (null / not null), feedbacks (size), chapters (null / not null).
 */
class getCourseDescriptionTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CreatorRepository creatorRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    // Các dependency khác tồn tại trong CourseInfService (nếu có) nhưng không ảnh hưởng trực tiếp
    @Mock
    private ValidationResources validationResources;

    @InjectMocks
    private CourseInfService courseInfService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ==========================================================
    // ========== Helper builders cho dữ liệu giả ===============
    // ==========================================================

    private Course createCourse(long id,
                                String name,
                                boolean isPublic,
                                Creator creator,
                                List<Chapter> chapters) {
        Course c = new Course();
        c.setCourseId(id);
        c.setName(name);
        c.setPublic(isPublic);
        c.setCreator(creator);
        c.setChapters(chapters);
        return c;
    }

    private Creator createCreator(long creatorId, List<Course> creatorCourses) {
        Creator creator = new Creator();
        creator.setCreatorId(creatorId);
        creator.setCourses(creatorCourses);
        return creator;
    }

    private Module createModuleWithContents(ModuleContent... contents) {
        Module m = new Module();
        m.setModuleContent(Arrays.asList(contents));
        return m;
    }

    /**
     * ModuleContent là abstract => tạo subclass vô danh để set type, tránh compile error.
     * Ở đây không cần type cụ thể cho test getCourseDescription vì totalModules chỉ đếm module,
     * không đếm moduleContent. Nhưng ta tạo stub cho an toàn.
     */
    private ModuleContent createDummyModuleContent() {
        return new ModuleContent() {
            // override abstract methods if needed
        };
    }

    private Chapter createChapterWithModules(Module... modules) {
        Chapter ch = new Chapter();
        ch.setModules(Arrays.asList(modules));
        return ch;
    }

    private Chapter createChapterWithNullModules() {
        Chapter ch = new Chapter();
        ch.setModules(null);
        return ch;
    }

    private Enrollment createEnrollmentWithFeedback(Course course, boolean includeFeedback, boolean includeCustomer) {
        Enrollment e = new Enrollment();
        e.setCourse(course);

        if (includeFeedback) {
            Feedback fb = new Feedback();
            // nếu Feedback có các setter như setRate/setComment/... thì có thể set thêm
            e.setFeedback(fb);
        }

        if (includeCustomer) {
            Customer cus = new Customer();
            // nếu Customer có setName()/... thì có thể set thêm
            e.setCustomer(cus);
        }

        return e;
    }

    // ==========================================================
    // ===================== HAPPY PATH =========================
    // ==========================================================

    /**
     * TC_HP_01:
     * - Course tồn tại và public.
     * - Repo trả về thống kê hợp lệ:
     * totalStudents = 42
     * totalFeedbacks = 10
     * avgRating DB = 4.36 -> DTO.averageRating = 4.4 (round 1 decimal)
     * - Creator tồn tại.
     * - Course có 2 Chapter:
     * chapter1 có 3 modules
     * chapter2 có 1 module
     * => totalModules = 4
     * - enrollmentRepository trả về list Enrollment chứa feedback hợp lệ
     * => feedbacks trong DTO không rỗng
     * <p>
     * Kỳ vọng:
     * - Không ném exception
     * - DTO chứa thông tin chính xác
     */
    @Test
    void shouldReturnCourseDescriptionDto_whenCourseIsPublicAndDataAvailable() {
        // =========================
        // Given
        // =========================
        long courseId = 123L;

        // Creator (có danh sách khoá học của creator để mapCreatorToDto() tính totalCourses)
        Creator creator = createCreator(999L, new ArrayList<>());

        // Chapter 1 với 3 modules
        Module moduleA1 = createModuleWithContents(createDummyModuleContent());
        Module moduleA2 = createModuleWithContents(createDummyModuleContent());
        Module moduleA3 = createModuleWithContents(createDummyModuleContent());
        // Chapter 2 với 1 module  
        Module moduleB = createModuleWithContents(createDummyModuleContent());

        Chapter chapter1 = createChapterWithModules(moduleA1, moduleA2, moduleA3);
        Chapter chapter2 = createChapterWithModules(moduleB);
        List<Chapter> chapters = Arrays.asList(chapter1, chapter2);

        Course course = createCourse(
                courseId,
                "N5 Japanese Basics",
                true,          // isPublic = true
                creator,
                chapters
        );

        // Mock courseRepository.findById -> trả về course hợp lệ
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        // Mock các thống kê course-level
        when(courseRepository.countEnrollmentsByCourseId(courseId)).thenReturn(42);
        when(courseRepository.countFeedbacksByCourseId(courseId)).thenReturn(10);
        when(courseRepository.getAverageRatingByCourseId(courseId)).thenReturn(4.36); // sẽ round -> 4.4

        // Mock các thống kê creator-level
        when(creatorRepository.countTotalStudentsByCreatorId(999L)).thenReturn(1000);
        when(creatorRepository.getAverageRatingByCreatorId(999L)).thenReturn(4.8);

        // Mock feedbacks: enrollmentRepository.findByCourse(course)
        // Một enrollment có cả feedback và customer -> giữ lại
        Enrollment e1 = createEnrollmentWithFeedback(course, true, true);
        // Một enrollment thiếu feedback hoặc thiếu customer -> sẽ bị filter bỏ
        Enrollment e2 = createEnrollmentWithFeedback(course, false, true);

        when(enrollmentRepository.findByCourse(course))
                .thenReturn(Arrays.asList(e1, e2));

        // =========================
        // When
        // =========================
        CourseDescriptionDto dto = courseInfService.getCourseDescription(courseId);

        // =========================
        // Then
        // =========================
        assertNotNull(dto, "DTO trả về không được null cho course public hợp lệ");

        assertEquals(courseId, dto.getCourseId(), "courseId trong DTO phải khớp");
        assertEquals("N5 Japanese Basics", dto.getName(), "Tên course phải khớp");

        assertEquals(42, dto.getTotalStudents(), "totalStudents phải lấy từ countEnrollmentsByCourseId");
        assertEquals(10, dto.getTotalFeedbacks(), "totalFeedbacks phải lấy từ countFeedbacksByCourseId");

        assertEquals(4.4, dto.getAverageRating(), 0.0001,
                "averageRating phải được làm tròn 1 chữ số thập phân từ 4.36 => 4.4");

        assertEquals(4, dto.getTotalModules(),
                "totalModules = tổng số modules của tất cả chapters (3 + 1 = 4)");

        assertNotNull(dto.getCreator(),
                "creatorDto phải khác null khi course có creator hợp lệ");

        assertNotNull(dto.getFeedbacks(),
                "feedback list không được null");
        assertTrue(dto.getFeedbacks().size() >= 1,
                "feedback list phải chứa ít nhất 1 phần tử map từ enrollment có feedback + customer");

        assertNotNull(dto.getChapters(),
                "Chapters trong DTO phải khác null khi course có chapters");
        assertEquals(2, dto.getChapters().size(),
                "DTO phải giữ nguyên số chapters (2 chương)");
    }

    // ==========================================================
    // ================== ERROR SCENARIOS =======================
    // ==========================================================

    /**
     * TC_ES_01:
     * - courseRepository.findById(courseId) => Optional.empty()
     * - Kỳ vọng: RuntimeException("Course not found with id: " + courseId)
     */
    @Test
    void shouldThrowRuntimeException_whenCourseNotFoundById() {
        // =========================
        // Given
        // =========================
        long courseId = 404L;

        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        // =========================
        // When / Then
        // =========================
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> courseInfService.getCourseDescription(courseId),
                "Nếu không tìm thấy course thì phải ném RuntimeException"
        );

        // Kiểm tra message (nếu có)
        String expected = "Course not found with id: " + courseId;
        if (ex.getMessage() != null) {
            assertTrue(ex.getMessage().contains(expected),
                    "Message exception phải chứa courseId không tồn tại");
        }
    }

    /**
     * TC_ES_02:
     * - Course tồn tại nhưng isPublic() == false
     * - Kỳ vọng: UnauthorizedException("this course is not exist")
     */
    @Test
    void shouldThrowUnauthorizedException_whenCourseIsNotPublic() {
        // =========================
        // Given
        // =========================
        long courseId = 777L;

        Course privateCourse = createCourse(
                courseId,
                "Secret Course",
                false,    // isPublic = false
                null,
                Collections.emptyList()
        );

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(privateCourse));

        // =========================
        // When / Then
        // =========================
        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> courseInfService.getCourseDescription(courseId),
                "Nếu course không public thì phải ném UnauthorizedException"
        );

        if (ex.getMessage() != null) {
            assertTrue(ex.getMessage().contains("this course is not exist"),
                    "Message UnauthorizedException phải giống logic nghiệp vụ");
        }
    }

    // ==========================================================
    // ===================== EDGE CASES =========================
    // ==========================================================

    /**
     * TC_EC_01:
     * - avgRating từ DB là null
     * - Kỳ vọng: dto.getAverageRating() == 0.0
     * <p>
     * Các giá trị khác (totalStudents, totalFeedbacks) vẫn trả về bình thường.
     */
    @Test
    void shouldReturnAverageRatingZero_whenAvgRatingIsNull() {
        // =========================
        // Given
        // =========================
        long courseId = 100L;

        Creator creator = createCreator(10L, new ArrayList<>());

        // 1 chapter có 2 modules => totalModules=2
        Module m1 = createModuleWithContents(createDummyModuleContent());
        Module m2 = createModuleWithContents(createDummyModuleContent());
        Chapter chapter = createChapterWithModules(m1, m2);
        Course course = createCourse(
                courseId,
                "Course Without Rating",
                true,
                creator,
                Collections.singletonList(chapter)
        );

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseRepository.countEnrollmentsByCourseId(courseId)).thenReturn(5);
        when(courseRepository.countFeedbacksByCourseId(courseId)).thenReturn(0);
        when(courseRepository.getAverageRatingByCourseId(courseId))
                .thenReturn(null); // <-- avgRating null

        when(creatorRepository.countTotalStudentsByCreatorId(10L)).thenReturn(12);
        when(creatorRepository.getAverageRatingByCreatorId(10L)).thenReturn(4.5);

        // Không có feedback nào
        when(enrollmentRepository.findByCourse(course)).thenReturn(Collections.emptyList());

        // =========================
        // When
        // =========================
        CourseDescriptionDto dto = courseInfService.getCourseDescription(courseId);

        // =========================
        // Then
        // averageRating phải fallback về 0.0
        assertEquals(0.0, dto.getAverageRating(), 0.0001,
                "Khi avgRating từ DB là null, DTO.averageRating phải = 0.0");

        assertEquals(2, dto.getTotalModules(),
                "totalModules phải là tổng số modules trong các chapters (2 modules)");

        assertEquals(5, dto.getTotalStudents(),
                "totalStudents lấy từ countEnrollmentsByCourseId");
        assertEquals(0, dto.getTotalFeedbacks(),
                "totalFeedbacks lấy từ countFeedbacksByCourseId");

        assertNotNull(dto.getCreator(),
                "creator vẫn phải khác null vì course có creator");
        assertNotNull(dto.getFeedbacks(),
                "feedback list không được null");
        assertEquals(0, dto.getFeedbacks().size(),
                "feedback list rỗng vì không có enrollment nào có feedback");
    }

    /**
     * TC_EC_02:
     * - course.getCreator() == null
     * - mapCreatorToDto() sẽ trả về null ngay.
     * - Kỳ vọng: dto.getCreator() == null
     * <p>
     * Các phần khác (totalStudents, avgRating, ...) vẫn xử lý bình thường.
     */
    @Test
    void shouldReturnNullCreatorDto_whenCourseHasNoCreator() {
        // =========================
        // Given
        // =========================
        long courseId = 200L;

        // Course public nhưng creator = null
        Module m1 = createModuleWithContents(createDummyModuleContent());
        Chapter ch = createChapterWithModules(m1);
        Course course = createCourse(
                courseId,
                "Course No Creator",
                true,
                null, // <-- creator null
                Collections.singletonList(ch)
        );

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseRepository.countEnrollmentsByCourseId(courseId)).thenReturn(9);
        when(courseRepository.countFeedbacksByCourseId(courseId)).thenReturn(3);
        when(courseRepository.getAverageRatingByCourseId(courseId)).thenReturn(4.0);

        // Vì creator == null, mapCreatorToDto() sẽ return null và KHÔNG gọi creatorRepository,
        // nên ta không cần stub creatorRepository ở đây.

        // Không feedback hợp lệ
        when(enrollmentRepository.findByCourse(course)).thenReturn(Collections.emptyList());

        // =========================
        // When
        // =========================
        CourseDescriptionDto dto = courseInfService.getCourseDescription(courseId);

        // =========================
        // Then
        assertEquals(9, dto.getTotalStudents());
        assertEquals(3, dto.getTotalFeedbacks());
        assertEquals(4.0, dto.getAverageRating(), 0.0001,
                "avgRating != null => dùng Math.round(4.0 * 10)/10 = 4.0");

        assertEquals(1, dto.getTotalModules(),
                "Có 1 chapter với 1 module => totalModules=1");

        assertNull(dto.getCreator(),
                "Khi course không có creator, dto.getCreator() phải null");

        assertNotNull(dto.getFeedbacks(),
                "feedbacks không được null");
        assertEquals(0, dto.getFeedbacks().size(),
                "feedbacks rỗng vì không có enrollment hợp lệ");
    }

    /**
     * TC_EC_03:
     * - enrollmentRepository.findByCourse(course) trả về list rỗng
     * => không có feedback hợp lệ
     * - Kỳ vọng: dto.getFeedbacks() là list rỗng (size=0), không crash.
     */
    @Test
    void shouldReturnEmptyFeedbackList_whenNoEnrollmentFeedbacks() {
        // =========================
        // Given
        // =========================
        long courseId = 300L;

        Creator creator = createCreator(30L, new ArrayList<>());
        Chapter ch = createChapterWithModules(
                createModuleWithContents(createDummyModuleContent(),
                        createDummyModuleContent())
        );

        Course course = createCourse(
                courseId,
                "Course No Feedback Yet",
                true,
                creator,
                Collections.singletonList(ch)
        );

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseRepository.countEnrollmentsByCourseId(courseId)).thenReturn(0);
        when(courseRepository.countFeedbacksByCourseId(courseId)).thenReturn(0);
        when(courseRepository.getAverageRatingByCourseId(courseId)).thenReturn(3.5);

        when(creatorRepository.countTotalStudentsByCreatorId(30L)).thenReturn(100);
        when(creatorRepository.getAverageRatingByCreatorId(30L)).thenReturn(4.7);

        // Không enrollment nào => feedbacks rỗng
        when(enrollmentRepository.findByCourse(course)).thenReturn(Collections.emptyList());

        // =========================
        // When
        // =========================
        CourseDescriptionDto dto = courseInfService.getCourseDescription(courseId);

        // =========================
        // Then
        assertNotNull(dto.getFeedbacks(),
                "feedbacks không được null ngay cả khi không có enrollment");
        assertEquals(0, dto.getFeedbacks().size(),
                "feedbacks phải rỗng nếu không có enrollment hợp lệ");

        assertEquals(0, dto.getTotalStudents());
        assertEquals(0, dto.getTotalFeedbacks());

        // avgRating = 3.5 -> round(3.5*10)/10 = 3.5
        assertEquals(3.5, dto.getAverageRating(), 0.0001);

        assertEquals(1, dto.getTotalModules(),
                "1 chapter với 1 module => totalModules=1");

        assertNotNull(dto.getCreator(),
                "Creator không null trong case này");
    }

    /**
     * TC_EC_04:
     * - Course có nhiều Chapter, trong đó có Chapter.modules == null.
     * - Logic totalModules:
     * sum(ch.getModules() != null ? ch.getModules().size() : 0)
     * => modules null không gây NPE và được tính như 0.
     * <p>
     * Ví dụ:
     * chapter1.modules.size() = 2
     * chapter2.modules == null
     * => totalModules = 2
     */
    @Test
    void shouldCalculateTotalModulesIgnoringNullModules_whenSomeChaptersHaveNullModules() {
        // =========================
        // Given
        // =========================
        long courseId = 400L;

        Creator creator = createCreator(40L, new ArrayList<>());

        // chapterWithModules có 2 modules
        Module modA = createModuleWithContents(createDummyModuleContent());
        Module modB = createModuleWithContents(createDummyModuleContent());
        Chapter chapterWithModules = createChapterWithModules(modA, modB);

        // chapterWithoutModules có modules == null
        Chapter chapterWithoutModules = createChapterWithNullModules();

        Course course = createCourse(
                courseId,
                "Mixed Chapters Course",
                true,
                creator,
                Arrays.asList(chapterWithModules, chapterWithoutModules)
        );

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseRepository.countEnrollmentsByCourseId(courseId)).thenReturn(12);
        when(courseRepository.countFeedbacksByCourseId(courseId)).thenReturn(4);
        when(courseRepository.getAverageRatingByCourseId(courseId)).thenReturn(4.9);

        when(creatorRepository.countTotalStudentsByCreatorId(40L)).thenReturn(777);
        when(creatorRepository.getAverageRatingByCreatorId(40L)).thenReturn(4.95);

        // feedback giả có ít nhất 1 enrollment hợp lệ
        Enrollment e1 = createEnrollmentWithFeedback(course, true, true);
        when(enrollmentRepository.findByCourse(course)).thenReturn(Collections.singletonList(e1));

        // =========================
        // When
        // =========================
        CourseDescriptionDto dto = courseInfService.getCourseDescription(courseId);

        // =========================
        // Then
        assertEquals(2, dto.getTotalModules(),
                "totalModules phải = 2 (2 modules ở chapter1 + 0 ở chapter2 null modules)");

        assertEquals(12, dto.getTotalStudents());
        assertEquals(4, dto.getTotalFeedbacks());

        // 4.9 -> round(4.9*10)/10 = 4.9
        assertEquals(4.9, dto.getAverageRating(), 0.0001);

        assertNotNull(dto.getFeedbacks(),
                "feedbacks không được null");
        assertTrue(dto.getFeedbacks().size() >= 1,
                "Vì có enrollment hợp lệ nên feedbacks phải >=1");

        assertNotNull(dto.getCreator(),
                "creatorDto không được null khi có creator");
        assertNotNull(dto.getChapters(),
                "Chapters không được null trong case này");
        assertEquals(2, dto.getChapters().size(),
                "Phải có đúng 2 chapter trong DTO");
    }

    /**
     * TC_EC_05:
     * - course.getChapters() == null hoàn toàn.
     * - Logic:
     * totalModules = 0
     * chaptersDto = course.getChapters() = null
     * - enrollmentRepository.findByCourse(course) có thể trả về empty list bình thường.
     * <p>
     * Kỳ vọng:
     * - dto.getChapters() == null
     * - dto.getTotalModules() == 0
     * - Không crash
     */
    @Test
    void shouldReturnZeroModulesAndNullChapters_whenCourseHasNoChapters() {
        // =========================
        // Given
        // =========================
        long courseId = 500L;

        Creator creator = createCreator(50L, new ArrayList<>());

        // Course public nhưng chapters = null
        Course course = createCourse(
                courseId,
                "Empty Course",
                true,
                creator,
                null // <-- chapters null
        );

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseRepository.countEnrollmentsByCourseId(courseId)).thenReturn(0);
        when(courseRepository.countFeedbacksByCourseId(courseId)).thenReturn(0);
        when(courseRepository.getAverageRatingByCourseId(courseId)).thenReturn(5.0);

        when(creatorRepository.countTotalStudentsByCreatorId(50L)).thenReturn(0);
        when(creatorRepository.getAverageRatingByCreatorId(50L)).thenReturn(0.0);

        // Không enrollment -> không feedback
        when(enrollmentRepository.findByCourse(course)).thenReturn(Collections.emptyList());

        // =========================
        // When
        // =========================
        CourseDescriptionDto dto = courseInfService.getCourseDescription(courseId);

        // =========================
        // Then
        assertEquals(0, dto.getTotalModules(),
                "totalModules phải bằng 0 khi course.getChapters() == null");

        assertNull(dto.getChapters(),
                "dto.getChapters() phải null nếu course.getChapters() là null");

        assertNotNull(dto.getFeedbacks(),
                "feedbacks không được null");
        assertEquals(0, dto.getFeedbacks().size(),
                "Nếu không có enrollment thì feedbacks phải rỗng");

        // averageRating != null (5.0) => round(5.0*10)/10 = 5.0
        assertEquals(5.0, dto.getAverageRating(), 0.0001,
                "averageRating phải là 5.0 vì DB trả 5.0");

        assertNotNull(dto.getCreator(),
                "creatorDto không được null khi có creator");
    }
}
