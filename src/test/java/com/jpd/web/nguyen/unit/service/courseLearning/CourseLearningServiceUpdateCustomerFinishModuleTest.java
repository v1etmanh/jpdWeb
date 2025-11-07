package com.jpd.web.nguyen.unit.service.courseLearning;

import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.CustomerModuleContent;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Module;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.repository.CustomerModuleContentRepository;
import com.jpd.web.repository.ModuleRepository;
import com.jpd.web.service.CourseLearningService;
import com.jpd.web.service.utils.ValidationResources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho hàm:
 *
 *   public void updateCustomerFinishModule(
 *       long courseId,
 *       String email,
 *       long moduleId,
 *       TypeOfContent typeOfContent
 *   )
 *
 * Các scenario bao phủ:
 *
 *  HAPPY PATH:
 *   - TC_HP_01: tạo mới CustomerModuleContent khi chưa có record tiến độ
 *   - TC_HP_02: cập nhật CustomerModuleContent đã tồn tại
 *
 *  EDGE CASE:
 *   - TC_EC_01: gọi lại cùng content type đã có trước đó -> set không đổi nhưng vẫn save
 *
 *  ERROR SCENARIOS:
 *   - TC_ES_01: user không hợp lệ / không có quyền (validationResources ném UnauthorizedException)
 *   - TC_ES_02: moduleId không tồn tại -> ModuleNotFoundException
 *   - TC_ES_03: module thuộc course khác -> UnauthorizedException
 *   - TC_ES_04: module không chứa TypeOfContent được yêu cầu -> RuntimeException("module khong chua content do")
 *
 * Ghi chú kỹ thuật:
 *  - Ta dùng @ExtendWith(MockitoExtension.class) để Mockito tự inject @Mock vào @InjectMocks.
 *  - Dùng ArgumentCaptor để bắt đối tượng được save() trong happy path tạo mới.
 *  - Các Entity (Enrollment, Module, Chapter, Course, CustomerModuleContent) giả sử có
 *    constructor rỗng + getter/setter + builder() (Lombok @Builder).
 *    Nếu code thực tế khác một chút (ví dụ: tên getter), bạn chỉnh lại cho khớp.
 */

@ExtendWith(MockitoExtension.class)
class CourseLearningServiceUpdateCustomerFinishModuleTest {

    @Mock
    private ValidationResources validationResources;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private CustomerModuleContentRepository contentRepository;

    @InjectMocks
    private CourseLearningService courseLearningService;

    @BeforeEach
    void setUp() {
        // Inject @Autowired dependencies using ReflectionTestUtils
        ReflectionTestUtils.setField(courseLearningService, "validationResources", validationResources);
        ReflectionTestUtils.setField(courseLearningService, "moduleRepository", moduleRepository);
        ReflectionTestUtils.setField(courseLearningService, "contentRepository", contentRepository);
    }

    // ----------------------------------------------------------------------
    // HAPPY PATH 1:
    // cmds.isEmpty() == true  -> tạo mới CustomerModuleContent và save
    // ----------------------------------------------------------------------
    @Test
    void shouldCreateNewProgressRecord_whenNoExistingCustomerModuleContent() {
        // =====================
        // Given
        // =====================
        long courseId = 100L;
        long moduleId = 10L;
        long chapterId = 20L;     // tham số không dùng trực tiếp trong service, nhưng
        String email = "student@example.com";
        TypeOfContent finishedType = TypeOfContent.VIDEO;

        // mock Enrollment e (đã validate user có quyền học khóa courseId)
        Enrollment enrollmentMock = mock(Enrollment.class);
        when(validationResources.validateCustomerWithCourseGetE(email, courseId))
                .thenReturn(enrollmentMock);

        // mock module + nested course ownership check
        Module moduleMock = mock(Module.class);
        Chapter chapterMock = mock(Chapter.class);
        Course courseMock = mock(Course.class);

        when(moduleRepository.findById(moduleId))
                .thenReturn(Optional.of(moduleMock));

        when(moduleMock.getChapter()).thenReturn(chapterMock);
        when(chapterMock.getCourse()).thenReturn(courseMock);
        when(courseMock.getCourseId()).thenReturn(courseId); // module thuộc đúng courseId

        // module chứa content type cần đánh dấu (ví dụ VIDEO)
        Set<TypeOfContent> moduleSupportedTypes = new HashSet<>();
        moduleSupportedTypes.add(TypeOfContent.VIDEO);
        when(moduleMock.getContentTypes()).thenReturn(moduleSupportedTypes);

        // contentRepository.findByEnrollmentAndModule(...) trả về rỗng (chưa có record tiến độ trước đó)
        when(contentRepository.findByEnrollmentAndModule(enrollmentMock, moduleMock))
                .thenReturn(Optional.empty());

        // =====================
        // When
        // =====================
        courseLearningService.updateCustomerFinishModule(courseId, email, moduleId, finishedType);

        // =====================
        // Then
        // =====================
        // Bắt đối tượng đã được save để assert logic tạo mới
        ArgumentCaptor<CustomerModuleContent> captor =
                ArgumentCaptor.forClass(CustomerModuleContent.class);

        verify(contentRepository, times(1)).save(captor.capture());

        CustomerModuleContent saved = captor.getValue();
        assertNotNull(saved, "Phải save một CustomerModuleContent mới");
        assertSame(enrollmentMock, saved.getEnrollment(),
                "CustomerModuleContent mới phải gắn đúng Enrollment e");
        assertSame(moduleMock, saved.getModule(),
                "CustomerModuleContent mới phải gắn đúng Module m");
        assertNotNull(saved.getTypeOfContent(),
                "Set typeOfContent phải được khởi tạo");
        assertTrue(saved.getTypeOfContent().contains(finishedType),
                "Set typeOfContent phải chứa loại content vừa hoàn thành");
        // Nếu entity có getter availableRequest():
        // assertEquals(5, saved.getAvailableRequest(), "Giá trị mặc định availableRequest phải là 5");

        verify(validationResources).validateCustomerWithCourseGetE(email, courseId);
        verify(moduleRepository).findById(moduleId);
        verify(contentRepository).findByEnrollmentAndModule(enrollmentMock, moduleMock);
    }

    // ----------------------------------------------------------------------
    // HAPPY PATH 2:
    // cmds.isEmpty() == false -> tái sử dụng record tiến độ cũ, add typeOfContent rồi save
    // ----------------------------------------------------------------------
    @Test
    void shouldUpdateExistingProgressRecord_whenCustomerModuleContentAlreadyExists() {
        // =====================
        // Given
        // =====================
        long courseId = 200L;
        long moduleId = 22L;
        long chapterId = 33L;
        String email = "student@example.com";
        TypeOfContent finishedType = TypeOfContent.MULTIPLE_CHOICE;

        Enrollment enrollmentMock = mock(Enrollment.class);
        when(validationResources.validateCustomerWithCourseGetE(email, courseId))
                .thenReturn(enrollmentMock);

        Module moduleMock = mock(Module.class);
        Chapter chapterMock = mock(Chapter.class);
        Course courseMock = mock(Course.class);

        when(moduleRepository.findById(moduleId))
                .thenReturn(Optional.of(moduleMock));

        when(moduleMock.getChapter()).thenReturn(chapterMock);
        when(chapterMock.getCourse()).thenReturn(courseMock);
        when(courseMock.getCourseId()).thenReturn(courseId); // cùng course => pass check

        // module chứa MULTIPLE_CHOICE
        Set<TypeOfContent> supportedTypes = new HashSet<>();
        supportedTypes.add(TypeOfContent.MULTIPLE_CHOICE);
        supportedTypes.add(TypeOfContent.VIDEO);
        when(moduleMock.getContentTypes()).thenReturn(supportedTypes);

        // giả lập record tiến độ đã tồn tại cho user này
        CustomerModuleContent existingCmc = CustomerModuleContent.builder()
                .enrollment(enrollmentMock)
                .module(moduleMock)
                .availableRequest(5)
                .build();

        // set ban đầu có thể trống hoặc có thứ khác
        Set<TypeOfContent> progressSet = new HashSet<>();
        progressSet.add(TypeOfContent.VIDEO); // user đã hoàn thành VIDEO trước đó
        existingCmc.setTypeOfContent(progressSet);

        when(contentRepository.findByEnrollmentAndModule(enrollmentMock, moduleMock))
                .thenReturn(Optional.of(existingCmc));

        // =====================
        // When
        // =====================
        courseLearningService.updateCustomerFinishModule(courseId, email, moduleId, finishedType);

        // =====================
        // Then
        // existingCmc phải được mutate: thêm MULTIPLE_CHOICE vào set
        assertTrue(existingCmc.getTypeOfContent().contains(TypeOfContent.VIDEO),
                "Set phải giữ lại content đã hoàn thành trước đó (VIDEO)");
        assertTrue(existingCmc.getTypeOfContent().contains(TypeOfContent.MULTIPLE_CHOICE),
                "Set phải bao gồm content mới được đánh dấu hoàn thành (MULTIPLE_CHOICE)");

        // verify save được gọi với đúng instance cũ
        verify(contentRepository, times(1)).save(existingCmc);

        verify(validationResources).validateCustomerWithCourseGetE(email, courseId);
        verify(moduleRepository).findById(moduleId);
        verify(contentRepository).findByEnrollmentAndModule(enrollmentMock, moduleMock);
    }

    // ----------------------------------------------------------------------
    // EDGE CASE:
    // Người dùng gọi lại cùng content type đã có sẵn trong set
    // -> Set không đổi (do là Set), nhưng vẫn save
    // ----------------------------------------------------------------------
    @Test
    void shouldStillSave_whenTypeWasAlreadyCompletedPreviously() {
        // =====================
        // Given
        // =====================
        long courseId = 300L;
        long moduleId = 44L;
        long chapterId = 55L;
        String email = "student@example.com";
        TypeOfContent finishedType = TypeOfContent.VIDEO; // user đánh dấu VIDEO lần nữa

        Enrollment enrollmentMock = mock(Enrollment.class);
        when(validationResources.validateCustomerWithCourseGetE(email, courseId))
                .thenReturn(enrollmentMock);

        Module moduleMock = mock(Module.class);
        Chapter chapterMock = mock(Chapter.class);
        Course courseMock = mock(Course.class);

        when(moduleRepository.findById(moduleId))
                .thenReturn(Optional.of(moduleMock));

        when(moduleMock.getChapter()).thenReturn(chapterMock);
        when(chapterMock.getCourse()).thenReturn(courseMock);
        when(courseMock.getCourseId()).thenReturn(courseId); // OK cùng khóa

        // Module hỗ trợ VIDEO
        Set<TypeOfContent> supportedTypes = new HashSet<>();
        supportedTypes.add(TypeOfContent.VIDEO);
        when(moduleMock.getContentTypes()).thenReturn(supportedTypes);

        // Record tiến độ đã tồn tại, và đã chứa VIDEO
        CustomerModuleContent existingCmc = CustomerModuleContent.builder()
                .enrollment(enrollmentMock)
                .module(moduleMock)
                .availableRequest(5)
                .build();

        Set<TypeOfContent> alreadyDone = new HashSet<>();
        alreadyDone.add(TypeOfContent.VIDEO); // user đã hoàn thành VIDEO từ trước
        existingCmc.setTypeOfContent(alreadyDone);

        when(contentRepository.findByEnrollmentAndModule(enrollmentMock, moduleMock))
                .thenReturn(Optional.of(existingCmc));

        // =====================
        // When
        // =====================
        courseLearningService.updateCustomerFinishModule(courseId, email, moduleId, finishedType);

        // =====================
        // Then
        assertEquals(1, existingCmc.getTypeOfContent().size(),
                "Set là Set, thêm lại VIDEO không được tạo thêm phần tử trùng lặp");
        assertTrue(existingCmc.getTypeOfContent().contains(TypeOfContent.VIDEO),
                "VIDEO vẫn phải còn trong set sau khi update");

        verify(contentRepository).save(existingCmc);
        verify(validationResources).validateCustomerWithCourseGetE(email, courseId);
        verify(moduleRepository).findById(moduleId);
        verify(contentRepository).findByEnrollmentAndModule(enrollmentMock, moduleMock);
    }

    // ----------------------------------------------------------------------
    // ERROR SCENARIO 1:
    // validationResources.validateCustomerWithCourseGetE(...) ném UnauthorizedException
    // => user không hợp lệ / không thuộc khóa học này
    // ----------------------------------------------------------------------
    @Test
    void shouldThrowUnauthorizedException_whenUserIsNotAuthorizedForCourse() {
        // =====================
        // Given
        // =====================
        long courseId = 400L;
        long moduleId = 66L;
        long chapterId = 77L;
        String email = "hacker@example.com";
        TypeOfContent finishedType = TypeOfContent.MULTIPLE_CHOICE;

        when(validationResources.validateCustomerWithCourseGetE(email, courseId))
                .thenThrow(new UnauthorizedException("forbidden"));

        // =====================
        // When / Then
        // =====================
        assertThrows(
                UnauthorizedException.class,
                () -> courseLearningService.updateCustomerFinishModule(courseId, email, moduleId, finishedType),
                "Nếu user không có quyền trong khóa học này thì phải ném UnauthorizedException ngay"
        );

        // Vì fail ngay ở bước validate => không được đụng repo
        verifyNoInteractions(moduleRepository);
        verifyNoInteractions(contentRepository);
    }

    // ----------------------------------------------------------------------
    // ERROR SCENARIO 2:
    // moduleRepository.findById(moduleId) trả về Optional.empty()
    // => ném ModuleNotFoundException
    // ----------------------------------------------------------------------
    @Test
    void shouldThrowModuleNotFoundException_whenModuleDoesNotExist() {
        // =====================
        // Given
        // =====================
        long courseId = 500L;
        long moduleId = 99L;
        String email = "student@example.com";
        TypeOfContent finishedType = TypeOfContent.READING;

        Enrollment enrollmentMock = mock(Enrollment.class);
        when(validationResources.validateCustomerWithCourseGetE(email, courseId))
                .thenReturn(enrollmentMock);

        // module không tồn tại
        when(moduleRepository.findById(moduleId))
                .thenReturn(Optional.empty());

        // =====================
        // When / Then
        // =====================
        assertThrows(
                ModuleNotFoundException.class,
                () -> courseLearningService.updateCustomerFinishModule(courseId, email, moduleId, finishedType),
                "Nếu moduleId không tồn tại thì phải ném ModuleNotFoundException"
        );

        // Không đi xa tới contentRepository
        verify(moduleRepository).findById(moduleId);
        verifyNoInteractions(contentRepository);
    }

    // ----------------------------------------------------------------------
    // ERROR SCENARIO 3:
    // Module tồn tại nhưng thuộc course khác
    // => m.getChapter().getCourse().getCourseId() != courseId => UnauthorizedException
    // ----------------------------------------------------------------------
    @Test
    void shouldThrowUnauthorizedException_whenModuleBelongsToDifferentCourse() {
        // =====================
        // Given
        // =====================
        long requestedCourseId = 600L;
        long moduleId = 123L;
        String email = "student@example.com";
        TypeOfContent finishedType = TypeOfContent.VIDEO;

        Enrollment enrollmentMock = mock(Enrollment.class);
        when(validationResources.validateCustomerWithCourseGetE(email, requestedCourseId))
                .thenReturn(enrollmentMock);

        Module moduleMock = mock(Module.class);
        Chapter chapterMock = mock(Chapter.class);
        Course otherCourseMock = mock(Course.class);

        // moduleRepository tìm thấy module
        when(moduleRepository.findById(moduleId))
                .thenReturn(Optional.of(moduleMock));

        // Nhưng module này thuộc courseId khác (ví dụ 777L thay vì requestedCourseId=600L)
        when(moduleMock.getChapter()).thenReturn(chapterMock);
        when(chapterMock.getCourse()).thenReturn(otherCourseMock);
        when(otherCourseMock.getCourseId()).thenReturn(777L);

        // =====================
        // When / Then
        // =====================
        assertThrows(
                UnauthorizedException.class,
                () -> courseLearningService.updateCustomerFinishModule(
                        requestedCourseId, email, moduleId, finishedType),
                "Nếu module thuộc course khác, phải ném UnauthorizedException"
        );

        // Không được gọi contentRepository vì dừng ở check quyền course
        verify(contentRepository, never())
                .findByEnrollmentAndModule(any(), any());
        verify(contentRepository, never())
                .save(any());
    }

    // ----------------------------------------------------------------------
    // ERROR SCENARIO 4:
    // Module đúng course, nhưng module.getContentTypes() KHÔNG chứa typeOfContent
    // => RuntimeException("module khong chua content do")
    // ----------------------------------------------------------------------
    @Test
    void shouldThrowRuntimeException_whenModuleDoesNotContainRequestedContentType() {
        // =====================
        // Given
        // =====================
        long courseId = 700L;
        long moduleId = 321L;
        String email = "student@example.com";
        TypeOfContent finishedType = TypeOfContent.MULTIPLE_CHOICE; // user muốn đánh dấu MULTIPLE_CHOICE

        Enrollment enrollmentMock = mock(Enrollment.class);
        when(validationResources.validateCustomerWithCourseGetE(email, courseId))
                .thenReturn(enrollmentMock);

        Module moduleMock = mock(Module.class);
        Chapter chapterMock = mock(Chapter.class);
        Course courseMock = mock(Course.class);

        when(moduleRepository.findById(moduleId))
                .thenReturn(Optional.of(moduleMock));

        // Module thuộc đúng khóa học
        when(moduleMock.getChapter()).thenReturn(chapterMock);
        when(chapterMock.getCourse()).thenReturn(courseMock);
        when(courseMock.getCourseId()).thenReturn(courseId);

        // Module CHỈ hỗ trợ VIDEO, KHÔNG hỗ trợ MULTIPLE_CHOICE
        Set<TypeOfContent> supportedTypes = new HashSet<>();
        supportedTypes.add(TypeOfContent.VIDEO);
        when(moduleMock.getContentTypes()).thenReturn(supportedTypes);

        // =====================
        // When / Then
        // =====================
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> courseLearningService.updateCustomerFinishModule(courseId, email, moduleId, finishedType),
                "Nếu module không chứa content type được yêu cầu thì phải ném RuntimeException"
        );

        assertEquals("module khong chua content do", ex.getMessage(),
                "Thông báo lỗi phải đúng business rule");

        // Không được truy xuất tiến độ hay save vì fail sớm
        verify(contentRepository, never())
                .findByEnrollmentAndModule(any(), any());
        verify(contentRepository, never())
                .save(any());
    }
}
