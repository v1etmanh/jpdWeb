package com.jpd.web.nguyen.unit.service.courseLearning;

import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.service.CourseLearningService;
import com.jpd.web.service.utils.ValidationResources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho method:
 *
 *  public List<ModuleContent> getModuleContentsByTypeAndModuleId(
 *      TypeOfContent type,
 *      Long moduleId,
 *      Long chapterId,
 *      Long courseId,
 *      String email
 *  )
 *
 * Bao phủ đầy đủ các scenario:
 *
 *  1. Happy Path:
 *     - User có quyền truy cập
 *     - Repo trả về 2 content, cả 2 đều resolve được qua findById()
 *
 *  2. Edge Cases:
 *     - Repo trả về list rỗng ban đầu
 *     - Repo trả về 2 content nhưng 1 trong số đó không tìm thấy lại bằng findById()
 *
 *  3. Error Scenario:
 *     - User KHÔNG có quyền, validationResources ném UnauthorizedException
 *       → Service phải bubble exception lên, không gọi repo
 *
 * Kết cấu test tuân thủ Given / When / Then.
 */
@ExtendWith(MockitoExtension.class)
class CourseLearningServiceGetModuleContentsByTypeAndModuleIdTest {

    @Mock
    private ValidationResources validationResources;

    @Mock
    private ModuleContentRepository moduleContentRepository;

    @Mock
    private com.jpd.web.controller.creator.CourseController courseController;

    @InjectMocks
    private CourseLearningService courseLearningService;

    @BeforeEach
    void init() {
        // Inject các @Autowired dependencies bằng ReflectionTestUtils
        ReflectionTestUtils.setField(courseLearningService, "validationResources", validationResources);
        ReflectionTestUtils.setField(courseLearningService, "moduleContentRepository", moduleContentRepository);
    }

    /**
     * ============================
     * TC_HP_01 (Happy Path)
     * ============================
     *
     * shouldReturnAllModuleContents_whenUserIsAuthorizedAndAllContentsResolve
     *
     * // Given:
     * - User có quyền truy cập module (validateModuleContentOwnerShip trả về moduleMock)
     * - moduleContentRepository.findByTypeOfContentAndModule(...) trả về [mcRef1, mcRef2]
     * - Mỗi phần tử có mcId riêng:
     *      mcRef1.getMcId() -> 111L
     *      mcRef2.getMcId() -> 222L
     * - Sau đó service gọi lại findById(111L) -> Optional.of(mcFull1),
     *                               findById(222L) -> Optional.of(mcFull2)
     * - Cả hai đều present nên kết quả trả về phải chứa đúng [mcFull1, mcFull2] theo thứ tự.
     *
     * // Then:
     * - result.size() == 2
     * - result.get(0) == mcFull1
     * - result.get(1) == mcFull2
     * - Không ném exception
     */
    @Test
    void shouldReturnAllModuleContents_whenUserIsAuthorizedAndAllContentsResolve() {
        // =========================
        // Given
        // =========================
        TypeOfContent type = TypeOfContent.VIDEO;
        Long moduleId = 10L;
        Long chapterId = 20L;
        Long courseId = 30L;
        String email = "student@example.com";

        Module moduleMock = mock(Module.class);

        // User hợp lệ => trả về module
        when(validationResources.validateModuleContentOwnerShip(moduleId, chapterId, courseId, email))
                .thenReturn(moduleMock);

        // Các content thô ban đầu trả về theo type + module
        ModuleContent mcRef1 = mock(ModuleContent.class);
        ModuleContent mcRef2 = mock(ModuleContent.class);

        when(mcRef1.getMcId()).thenReturn(111L);
        when(mcRef2.getMcId()).thenReturn(222L);

        List<ModuleContent> initialList = List.of(mcRef1, mcRef2);

        when(moduleContentRepository.findByTypeOfContentAndModule(type, moduleMock))
                .thenReturn(initialList);

        // Khi service duyệt từng phần tử, nó gọi findById() lại để "chốt" bản ghi
        ModuleContent mcFull1 = mock(ModuleContent.class, "mcFull1");
        ModuleContent mcFull2 = mock(ModuleContent.class, "mcFull2");

        when(moduleContentRepository.findById(111L))
                .thenReturn(Optional.of(mcFull1));
        when(moduleContentRepository.findById(222L))
                .thenReturn(Optional.of(mcFull2));

        // =========================
        // When
        // =========================
        List<ModuleContent> result = courseLearningService.getModuleContentsByTypeAndModuleId(
                type, moduleId, chapterId, courseId, email
        );

        // =========================
        // Then
        // =========================
        assertNotNull(result, "Happy path phải trả về danh sách, không được null");
        assertEquals(2, result.size(), "Phải trả về đủ 2 phần tử đã resolve qua findById()");
        assertSame(mcFull1, result.get(0),
                "Phần tử đầu tiên trong danh sách phải chính là mcFull1 (tương ứng id=111L)");
        assertSame(mcFull2, result.get(1),
                "Phần tử thứ hai trong danh sách phải chính là mcFull2 (tương ứng id=222L)");

        // verify interactions quan trọng
        verify(validationResources, times(1))
                .validateModuleContentOwnerShip(moduleId, chapterId, courseId, email);

        verify(moduleContentRepository, times(1))
                .findByTypeOfContentAndModule(type, moduleMock);

        verify(moduleContentRepository, times(1)).findById(111L);
        verify(moduleContentRepository, times(1)).findById(222L);
    }

    /**
     * ============================
     * TC_ES_01 (Error Scenario)
     * ============================
     *
     * shouldThrowUnauthorizedException_whenUserIsNotAuthorizedForModule
     *
     * // Given:
     * - validationResources.validateModuleContentOwnerShip(...) ném UnauthorizedException
     *
     * // When:
     * - Gọi service.getModuleContentsByTypeAndModuleId(...)
     *
     * // Then:
     * - assertThrows(UnauthorizedException)
     * - moduleContentRepository KHÔNG bị gọi
     */
    @Test
    void shouldThrowUnauthorizedException_whenUserIsNotAuthorizedForModule() {
        // =========================
        // Given
        // =========================
        TypeOfContent type = TypeOfContent.VIDEO;
        Long moduleId = 10L;
        Long chapterId = 20L;
        Long courseId = 30L;
        String email = "hacker@example.com";

        when(validationResources.validateModuleContentOwnerShip(moduleId, chapterId, courseId, email))
                .thenThrow(new UnauthorizedException("forbidden"));

        // =========================
        // When / Then
        // =========================
        assertThrows(
                UnauthorizedException.class,
                () -> courseLearningService.getModuleContentsByTypeAndModuleId(
                        type, moduleId, chapterId, courseId, email
                ),
                "Nếu user không có quyền trên module này, phải ném UnauthorizedException thay vì trả dữ liệu"
        );

        // Repo KHÔNG được gọi, vì fail ngay ở bước validate
        verifyNoInteractions(moduleContentRepository);
    }

    /**
     * ============================
     * TC_EC_01 (Edge Case)
     * ============================
     *
     * shouldReturnEmptyList_whenNoContentsFoundForType
     *
     * // Given:
     * - User hợp lệ (validation OK)
     * - moduleContentRepository.findByTypeOfContentAndModule(...) trả về Collections.emptyList()
     *
     * // When:
     * - Gọi service
     *
     * // Then:
     * - Kết quả phải là list rỗng (size = 0), không phải null
     * - Không ném exception
     * - findById() không bị gọi lần nào
     */
    @Test
    void shouldReturnEmptyList_whenNoContentsFoundForType() {
        // =========================
        // Given
        // =========================
        TypeOfContent type = TypeOfContent.MULTIPLE_CHOICE;
        Long moduleId = 77L;
        Long chapterId = 88L;
        Long courseId = 99L;
        String email = "student@example.com";

        Module moduleMock = mock(Module.class);

        when(validationResources.validateModuleContentOwnerShip(moduleId, chapterId, courseId, email))
                .thenReturn(moduleMock);

        // Không có nội dung nào cho type này
        when(moduleContentRepository.findByTypeOfContentAndModule(type, moduleMock))
                .thenReturn(Collections.emptyList());

        // =========================
        // When
        // =========================
        List<ModuleContent> result = courseLearningService.getModuleContentsByTypeAndModuleId(
                type, moduleId, chapterId, courseId, email
        );

        // =========================
        // Then
        // =========================
        assertNotNull(result,
                "Ngay cả khi không có nội dung, hàm phải trả về list rỗng chứ không phải null");
        assertTrue(result.isEmpty(),
                "findByTypeOfContentAndModule trả về rỗng => kết quả cuối cùng cũng phải rỗng");
        // vì mds rỗng nên vòng for không chạy => không gọi findById()
        verify(moduleContentRepository, never()).findById(anyLong());

        verify(validationResources, times(1))
                .validateModuleContentOwnerShip(moduleId, chapterId, courseId, email);

        verify(moduleContentRepository, times(1))
                .findByTypeOfContentAndModule(type, moduleMock);
    }

    /**
     * ============================
     * TC_EC_02 (Edge Case)
     * ============================
     *
     * shouldSkipMissingContents_whenFindByIdReturnsEmptyForSomeItems
     *
     * // Given:
     * - validation OK -> moduleMock
     * - findByTypeOfContentAndModule(...) trả về [mcRef1(id=111), mcRef2(id=222)]
     * - findById(111) -> Optional.of(mcFull1)
     * - findById(222) -> Optional.empty()
     *
     * // Then:
     * - Kết quả chỉ chứa mcFull1 (size = 1)
     * - Không ném exception
     */
    @Test
    void shouldSkipMissingContents_whenFindByIdReturnsEmptyForSomeItems() {
        // =========================
        // Given
        // =========================
        TypeOfContent type = TypeOfContent.READING;
        Long moduleId = 5L;
        Long chapterId = 6L;
        Long courseId = 7L;
        String email = "student@example.com";

        Module moduleMock = mock(Module.class);

        when(validationResources.validateModuleContentOwnerShip(moduleId, chapterId, courseId, email))
                .thenReturn(moduleMock);

        // Danh sách ban đầu có 2 content tham chiếu
        ModuleContent mcRef1 = mock(ModuleContent.class);
        ModuleContent mcRef2 = mock(ModuleContent.class);

        when(mcRef1.getMcId()).thenReturn(111L);
        when(mcRef2.getMcId()).thenReturn(222L);

        List<ModuleContent> initialList = new ArrayList<>();
        initialList.add(mcRef1);
        initialList.add(mcRef2);

        when(moduleContentRepository.findByTypeOfContentAndModule(type, moduleMock))
                .thenReturn(initialList);

        // Chỉ resolve được 1 trong 2
        ModuleContent mcFull1 = mock(ModuleContent.class, "mcFull1");

        when(moduleContentRepository.findById(111L))
                .thenReturn(Optional.of(mcFull1));
        when(moduleContentRepository.findById(222L))
                .thenReturn(Optional.empty());

        // =========================
        // When
        // =========================
        List<ModuleContent> result = courseLearningService.getModuleContentsByTypeAndModuleId(
                type, moduleId, chapterId, courseId, email
        );

        // =========================
        // Then
        // =========================
        assertNotNull(result,
                "Hàm luôn phải trả về list, không được null");
        assertEquals(1, result.size(),
                "Chỉ 1 content tồn tại thực sự (findById trả về present), nên kết quả phải có size=1");
        assertSame(mcFull1, result.get(0),
                "Content duy nhất trong kết quả phải là mcFull1 (id=111L)");

        // verify thứ tự gọi quan trọng
        verify(validationResources, times(1))
                .validateModuleContentOwnerShip(moduleId, chapterId, courseId, email);

        verify(moduleContentRepository, times(1))
                .findByTypeOfContentAndModule(type, moduleMock);

        verify(moduleContentRepository, times(1)).findById(111L);
        verify(moduleContentRepository, times(1)).findById(222L);
    }
}
