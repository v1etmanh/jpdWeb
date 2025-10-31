package com.jpd.web.nguyen.unit.service.courseInf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jpd.web.model.CustomerModuleContent;
import com.jpd.web.model.Enrollment;
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
 *     private Map<TypeOfContent, Integer> countCompletedContentsByType(Enrollment enrollment)
 * trong CourseInfService.
 *
 * Bao phủ:
 *  - Happy Paths
 *  - Edge Cases
 *  - Error Scenarios
 *
 * Kỹ thuật:
 *  - Mock contentRepository.findByEnrollment(...)
 *  - Gọi method private bằng Reflection
 *  - Kiểm tra map kết quả
 */
class countCompletedContentsByTypeTest {

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

    // Helper: lấy Method private countCompletedContentsByType và prepare để invoke
    private Method getCountCompletedMethod() throws Exception {
        Method m = CourseInfService.class.getDeclaredMethod(
                "countCompletedContentsByType",
                com.jpd.web.model.Enrollment.class
        );
        m.setAccessible(true);
        return m;
    }

    // Helper: gọi method private và trả về Map result
    @SuppressWarnings("unchecked")
    private Map<TypeOfContent, Integer> invokeCountCompletedContentsByType(Enrollment enrollment) throws Exception {
        Method m = getCountCompletedMethod();
        return (Map<TypeOfContent, Integer>) m.invoke(courseInfService, enrollment);
    }

    // Helper: tiện tạo CMC với set<TypeOfContent>
    private CustomerModuleContent cmcWithTypes(Set<TypeOfContent> types) {
        CustomerModuleContent cmc = new CustomerModuleContent();
        cmc.setTypeOfContent(types);
        return cmc;
    }

    // ------------------------------------------------------------------------------------
    // HAPPY PATHS
    // ------------------------------------------------------------------------------------

    /**
     * TC_HP_01:
     * Repo trả về nhiều CustomerModuleContent, mỗi cái có Set<TypeOfContent> khác nhau.
     *
     * Scenario mô phỏng:
     *  cmc1: {VIDEO, MULTIPLE_CHOICE}
     *  cmc2: {VIDEO}
     *  cmc3: {VIDEO, MULTIPLE_CHOICE}
     *
     * Kết quả mong đợi:
     *  VIDEO = 3
     *  MULTIPLE_CHOICE  = 2
     *
     * Giải thích:
     *  - VIDEO xuất hiện trong cmc1, cmc2, cmc3  -> 3 lần
     *  - MULTIPLE_CHOICE  xuất hiện trong cmc1 và cmc3      -> 2 lần
     */
    @Test
    void shouldAggregateCountsAcrossAllProgress_whenMultipleCmcReturned() throws Exception {
        // =========================
        // Given
        // =========================
        Enrollment enrollment = new Enrollment();

        CustomerModuleContent cmc1 = cmcWithTypes(new HashSet<>(Arrays.asList(
                TypeOfContent.VIDEO,
                TypeOfContent.MULTIPLE_CHOICE
        )));
        CustomerModuleContent cmc2 = cmcWithTypes(new HashSet<>(Arrays.asList(
                TypeOfContent.VIDEO
        )));
        CustomerModuleContent cmc3 = cmcWithTypes(new HashSet<>(Arrays.asList(
                TypeOfContent.VIDEO,
                TypeOfContent.MULTIPLE_CHOICE
        )));

        List<CustomerModuleContent> repoData = Arrays.asList(cmc1, cmc2, cmc3);

        when(contentRepository.findByEnrollment(any(Enrollment.class)))
                .thenReturn(repoData);

        // =========================
        // When
        // =========================
        Map<TypeOfContent, Integer> resultMap =
                invokeCountCompletedContentsByType(enrollment);

        // =========================
        // Then
        // =========================
        assertEquals(2, resultMap.size(),
                "Map phải chỉ chứa VIDEO và MULTIPLE_CHOICE");

        assertEquals(3,
                resultMap.get(TypeOfContent.VIDEO),
                "VIDEO phải đếm là 3");

        assertEquals(2,
                resultMap.get(TypeOfContent.MULTIPLE_CHOICE),
                "MULTIPLE_CHOICE phải đếm là 2");
    }

    /**
     * TC_HP_02:
     * Chỉ có VIDEO.
     *
     * Scenario mô phỏng:
     *  cmc1: {VIDEO}
     *  cmc2: {VIDEO}
     *  cmc3: {VIDEO}
     *  cmc4: {VIDEO}
     *  cmc5: {VIDEO}
     *
     * Kết quả mong đợi:
     *  VIDEO = 5
     *  (Map chỉ có 1 key VIDEO)
     */
    @Test
    void shouldCountOnlyVideoType_whenAllCompletedContentsAreVideo() throws Exception {
        // =========================
        // Given
        // =========================
        Enrollment enrollment = new Enrollment();

        CustomerModuleContent cmc1 = cmcWithTypes(new HashSet<>(Arrays.asList(TypeOfContent.VIDEO)));
        CustomerModuleContent cmc2 = cmcWithTypes(new HashSet<>(Arrays.asList(TypeOfContent.VIDEO)));
        CustomerModuleContent cmc3 = cmcWithTypes(new HashSet<>(Arrays.asList(TypeOfContent.VIDEO)));
        CustomerModuleContent cmc4 = cmcWithTypes(new HashSet<>(Arrays.asList(TypeOfContent.VIDEO)));
        CustomerModuleContent cmc5 = cmcWithTypes(new HashSet<>(Arrays.asList(TypeOfContent.VIDEO)));

        List<CustomerModuleContent> repoData = Arrays.asList(cmc1, cmc2, cmc3, cmc4, cmc5);

        when(contentRepository.findByEnrollment(any(Enrollment.class)))
                .thenReturn(repoData);

        // =========================
        // When
        // =========================
        Map<TypeOfContent, Integer> resultMap =
                invokeCountCompletedContentsByType(enrollment);

        // =========================
        // Then
        // =========================
        assertEquals(1, resultMap.size(),
                "Map chỉ nên có 1 loại là VIDEO");

        assertEquals(5,
                resultMap.get(TypeOfContent.VIDEO),
                "VIDEO phải đếm là 5 tổng cộng");
    }

    /**
     * TC_HP_03:
     * Chỉ một phần tử duy nhất trong list và set = {MULTIPLE_CHOICE}.
     *
     * Kết quả mong đợi:
     *  MULTIPLE_CHOICE = 1
     *  Map size = 1
     */
    @Test
    void shouldReturnSingleQuizCount_whenOnlyOneQuizCompleted() throws Exception {
        // =========================
        // Given
        // =========================
        Enrollment enrollment = new Enrollment();

        CustomerModuleContent cmc = cmcWithTypes(new HashSet<>(Arrays.asList(
                TypeOfContent.MULTIPLE_CHOICE
        )));

        List<CustomerModuleContent> repoData = Collections.singletonList(cmc);

        when(contentRepository.findByEnrollment(any(Enrollment.class)))
                .thenReturn(repoData);

        // =========================
        // When
        // =========================
        Map<TypeOfContent, Integer> resultMap =
                invokeCountCompletedContentsByType(enrollment);

        // =========================
        // Then
        // =========================
        assertEquals(1, resultMap.size(),
                "Map phải chỉ chứa MULTIPLE_CHOICE");

        assertEquals(1,
                resultMap.get(TypeOfContent.MULTIPLE_CHOICE),
                "MULTIPLE_CHOICE phải đếm là 1");
    }

    // ------------------------------------------------------------------------------------
    // EDGE CASES
    // ------------------------------------------------------------------------------------

    /**
     * TC_EC_01:
     * Repository trả về Collections.emptyList()
     * -> map rỗng.
     */
    @Test
    void shouldReturnEmptyMap_whenRepositoryReturnsEmptyList() throws Exception {
        // =========================
        // Given
        // =========================
        Enrollment enrollment = new Enrollment();

        when(contentRepository.findByEnrollment(any(Enrollment.class)))
                .thenReturn(Collections.emptyList());

        // =========================
        // When
        // =========================
        Map<TypeOfContent, Integer> resultMap =
                invokeCountCompletedContentsByType(enrollment);

        // =========================
        // Then
        // =========================
        assertEquals(0, resultMap.size(),
                "Khi repo trả về emptyList(), map phải rỗng");
    }

    /**
     * TC_EC_02:
     * Một phần tử có getTypeOfContent() == null
     *
     * Scenario mô phỏng:
     *  cmc1: {VIDEO, MULTIPLE_CHOICE}
     *  cmc2: null
     *  cmc3: {MULTIPLE_CHOICE}
     *
     * Kết quả mong đợi:
     *  VIDEO = 1
     *  MULTIPLE_CHOICE  = 2
     * (cmc2 bị bỏ qua)
     */
    @Test
    void shouldSkipEntryWithNullTypeSet_whenSomeCmcHaveNullTypeOfContent() throws Exception {
        // =========================
        // Given
        // =========================
        Enrollment enrollment = new Enrollment();

        CustomerModuleContent cmc1 = cmcWithTypes(new HashSet<>(Arrays.asList(
                TypeOfContent.VIDEO,
                TypeOfContent.MULTIPLE_CHOICE
        )));

        CustomerModuleContent cmc2 = cmcWithTypes(null); // getTypeOfContent() == null

        CustomerModuleContent cmc3 = cmcWithTypes(new HashSet<>(Arrays.asList(
                TypeOfContent.MULTIPLE_CHOICE
        )));

        List<CustomerModuleContent> repoData = Arrays.asList(cmc1, cmc2, cmc3);

        when(contentRepository.findByEnrollment(any(Enrollment.class)))
                .thenReturn(repoData);

        // =========================
        // When
        // =========================
        Map<TypeOfContent, Integer> resultMap =
                invokeCountCompletedContentsByType(enrollment);

        // =========================
        // Then
        // =========================
        assertEquals(2, resultMap.size(),
                "Map chỉ nên có VIDEO và MULTIPLE_CHOICE, và bỏ qua phần tử có typeOfContent == null");

        assertEquals(1,
                resultMap.get(TypeOfContent.VIDEO),
                "VIDEO phải đếm là 1 (chỉ từ cmc1)");

        assertEquals(2,
                resultMap.get(TypeOfContent.MULTIPLE_CHOICE),
                "MULTIPLE_CHOICE phải đếm là 2 (1 từ cmc1 + 1 từ cmc3)");
    }

    /**
     * TC_EC_03:
     * Một phần tử có getTypeOfContent() là set rỗng
     *
     * Scenario mô phỏng:
     *  cmc1: {VIDEO}
     *  cmc2: {}
     *
     * Kết quả mong đợi:
     *  VIDEO = 1
     *  Map không crash và không tạo key rỗng.
     */
    @Test
    void shouldHandleEmptyTypeSet_whenSomeCmcHaveNoTypes() throws Exception {
        // =========================
        // Given
        // =========================
        Enrollment enrollment = new Enrollment();

        CustomerModuleContent cmc1 = cmcWithTypes(new HashSet<>(Arrays.asList(
                TypeOfContent.VIDEO
        )));

        CustomerModuleContent cmc2 = cmcWithTypes(new HashSet<>()); // empty set

        List<CustomerModuleContent> repoData = Arrays.asList(cmc1, cmc2);

        when(contentRepository.findByEnrollment(any(Enrollment.class)))
                .thenReturn(repoData);

        // =========================
        // When
        // =========================
        Map<TypeOfContent, Integer> resultMap =
                invokeCountCompletedContentsByType(enrollment);

        // =========================
        // Then
        // =========================
        assertEquals(1, resultMap.size(),
                "Map chỉ nên có VIDEO vì cmc2 không có loại nào");

        assertEquals(1,
                resultMap.get(TypeOfContent.VIDEO),
                "VIDEO phải đếm là 1");
    }

    /**
     * TC_EC_04:
     * (Xác nhận business rule mỗi module chỉ đóng góp 1 lần cho mỗi type)
     *
     * Scenario mô phỏng:
     *  cmc1: {VIDEO, MULTIPLE_CHOICE}
     *  cmc2: {VIDEO}
     *
     * Kết quả mong đợi:
     *  VIDEO = 2
     *  MULTIPLE_CHOICE  = 1
     */
    @Test
    void shouldCountUniqueTypesPerCmc_whenTypesAppearAcrossModules() throws Exception {
        // =========================
        // Given
        // =========================
        Enrollment enrollment = new Enrollment();

        CustomerModuleContent cmc1 = cmcWithTypes(new HashSet<>(Arrays.asList(
                TypeOfContent.VIDEO,
                TypeOfContent.MULTIPLE_CHOICE
        )));
        CustomerModuleContent cmc2 = cmcWithTypes(new HashSet<>(Arrays.asList(
                TypeOfContent.VIDEO
        )));

        List<CustomerModuleContent> repoData = Arrays.asList(cmc1, cmc2);

        when(contentRepository.findByEnrollment(any(Enrollment.class)))
                .thenReturn(repoData);

        // =========================
        // When
        // =========================
        Map<TypeOfContent, Integer> resultMap =
                invokeCountCompletedContentsByType(enrollment);

        // =========================
        // Then
        // =========================
        assertEquals(2, resultMap.size(),
                "Map phải có VIDEO và MULTIPLE_CHOICE");

        assertEquals(2,
                resultMap.get(TypeOfContent.VIDEO),
                "VIDEO phải đếm là 2 (cmc1 + cmc2)");

        assertEquals(1,
                resultMap.get(TypeOfContent.MULTIPLE_CHOICE),
                "MULTIPLE_CHOICE phải đếm là 1 (chỉ từ cmc1)");
    }

    // ------------------------------------------------------------------------------------
    // ERROR SCENARIOS
    // ------------------------------------------------------------------------------------

    /**
     * TC_ES_01:
     * Repository trả về null thay vì List.
     *
     * Hành vi hiện tại:
     *  List<CustomerModuleContent> customerProgress = contentRepository.findByEnrollment(enrollment);
     *  for (CustomerModuleContent cmc : customerProgress) { ... }
     *
     * -> Nếu customerProgress == null thì vòng for sẽ NPE.
     *
     * Kỳ vọng test:
     *  - Ném NullPointerException
     *  - Vì gọi bằng Reflection, exception thật nằm trong InvocationTargetException.getCause()
     */
    @Test
    void shouldThrowNullPointerException_whenRepositoryReturnsNullList() throws Exception {
        // =========================
        // Given
        // =========================
        Enrollment enrollment = new Enrollment();

        when(contentRepository.findByEnrollment(any(Enrollment.class)))
                .thenReturn(null); // repo vi phạm contract, trả null thay vì emptyList

        Method m = getCountCompletedMethod();

        // =========================
        // When / Then
        // =========================
        try {
            m.invoke(courseInfService, enrollment);
            fail("Kỳ vọng NullPointerException khi repo trả về null list");
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause instanceof NullPointerException,
                    "Cause phải là NullPointerException khi customerProgress = null");
        }
    }

    /**
     * TC_ES_02:
     * Repository ném RuntimeException("DB down").
     *
     * Hành vi hiện tại:
     *  - Hàm không catch, nên exception bubble lên.
     */
    @Test
    void shouldPropagateRuntimeException_whenRepositoryThrowsUnexpectedError() throws Exception {
        // =========================
        // Given
        // =========================
        Enrollment enrollment = new Enrollment();

        RuntimeException boom = new RuntimeException("DB down");
        when(contentRepository.findByEnrollment(any(Enrollment.class)))
                .thenThrow(boom);

        Method m = getCountCompletedMethod();

        // =========================
        // When / Then
        // =========================
        try {
            m.invoke(courseInfService, enrollment);
            fail("Kỳ vọng RuntimeException bubble lên khi repository ném lỗi");
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause instanceof RuntimeException,
                    "Cause phải là RuntimeException");
            assertEquals("DB down", cause.getMessage(),
                    "Thông điệp lỗi phải khớp với RuntimeException gốc");
        }
    }

    /**
     * TC_ES_03 (tùy business):
     * Gọi với enrollment = null.
     *
     * Ở đây behavior phụ thuộc vào cách repository xử lý enrollment null.
     * Ta sẽ mock repository để trả về emptyList() khi enrollment là null
     * => Method sẽ chạy bình thường và trả về map rỗng.
     *
     * Ý nghĩa test:
     *  - Chứng minh rằng bản thân hàm không tự validate enrollment != null.
     *  - Đây là điểm cần confirm với BA/dev nếu enrollment null là "không hợp lệ".
     */
    @Test
    void shouldReturnEmptyMap_whenEnrollmentIsNullAndRepositoryReturnsEmptyList() throws Exception {
        // =========================
        // Given
        // =========================
        Enrollment nullEnrollment = null;

        when(contentRepository.findByEnrollment(null))
                .thenReturn(Collections.emptyList());

        // =========================
        // When
        // =========================
        Map<TypeOfContent, Integer> resultMap =
                invokeCountCompletedContentsByType(nullEnrollment);

        // =========================
        // Then
        // =========================
        assertEquals(0, resultMap.size(),
                "Nếu repo trả về emptyList() cho enrollment=null thì map kết quả phải rỗng, không crash");
    }
}
