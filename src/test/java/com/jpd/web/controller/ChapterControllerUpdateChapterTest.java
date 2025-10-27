package com.jpd.web.controller;

import com.jpd.web.controller.creator.ChapterController;
import com.jpd.web.exception.ChapterNotFoundException;
import com.jpd.web.exception.DuplicateChapterNameException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.service.ChapterService;
import com.jpd.web.service.utils.RequestAttributeExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("ChapterController - updateChapter() Unit Tests")
class ChapterControllerUpdateChapterTest {

    @Mock
    private ChapterService chapterService;

    @Mock
    private HttpServletRequest request;

    private ChapterController chapterController;
    private AutoCloseable closeable;

    private static final Long VALID_CHAPTER_ID = 1L;
    private static final Long VALID_CREATOR_ID = 1000L;

    private static final String VALID_NAME = "Introduction to Java";
    private static final String MIN_NAME = "A";
    private static final String SPECIAL_NAME = "Chap!@# $%^&*()";

    @BeforeAll
    void beforeAll() {
        System.out.println("Starting updateChapter() Tests");
    }

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        chapterController = new ChapterController(chapterService);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) closeable.close();
        reset(chapterService, request);
    }

    @AfterAll
    void afterAll() {
        System.out.println("Completed updateChapter() Tests");
    }

    // ------------------ SUCCESS TESTS ------------------

    @Test
    @DisplayName("Update chapter name successfully")
    void updateChapter_success() {
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doNothing().when(chapterService).updateChapter(VALID_CREATOR_ID, VALID_NAME, VALID_CHAPTER_ID);

            ResponseEntity<?> response = chapterController.updateChapter(VALID_NAME, request, VALID_CHAPTER_ID);

            assertEquals(204, response.getStatusCodeValue());
            verify(chapterService).updateChapter(VALID_CREATOR_ID, VALID_NAME, VALID_CHAPTER_ID);
        }
    }

    @Test
    @DisplayName("Update with special characters")
    void updateChapter_specialChars() {
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doNothing().when(chapterService).updateChapter(VALID_CREATOR_ID, SPECIAL_NAME, VALID_CHAPTER_ID);

            ResponseEntity<?> response = chapterController.updateChapter(SPECIAL_NAME, request, VALID_CHAPTER_ID);

            assertEquals(204, response.getStatusCodeValue());
            verify(chapterService).updateChapter(VALID_CREATOR_ID, SPECIAL_NAME, VALID_CHAPTER_ID);
        }
    }

    @Test
    @DisplayName("Update with minimum name length")
    void updateChapter_minLength() {
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doNothing().when(chapterService).updateChapter(VALID_CREATOR_ID, MIN_NAME, VALID_CHAPTER_ID);

            ResponseEntity<?> response = chapterController.updateChapter(MIN_NAME, request, VALID_CHAPTER_ID);

            assertEquals(204, response.getStatusCodeValue());
            verify(chapterService).updateChapter(VALID_CREATOR_ID, MIN_NAME, VALID_CHAPTER_ID);
        }
    }

    // ------------------ VALIDATION FAIL TESTS ------------------

    @Test
    @DisplayName("Blank chapter name should fail")
    void updateChapter_blankName() {
        assertThrows(IllegalArgumentException.class,
                () -> chapterController.updateChapter("", request, VALID_CHAPTER_ID));
        verify(chapterService, never()).updateChapter(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("Null chapter name should fail")
    void updateChapter_nullName() {
        assertThrows(IllegalArgumentException.class,
                () -> chapterController.updateChapter(null, request, VALID_CHAPTER_ID));
        verify(chapterService, never()).updateChapter(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("Whitespace-only chapter name should fail")
    void updateChapter_whitespaceOnly() {
        assertThrows(IllegalArgumentException.class,
                () -> chapterController.updateChapter("   ", request, VALID_CHAPTER_ID));
        verify(chapterService, never()).updateChapter(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("Zero chapter ID should fail")
    void updateChapter_zeroId() {
        assertThrows(IllegalArgumentException.class,
                () -> chapterController.updateChapter(VALID_NAME, request, 0L));
        verify(chapterService, never()).updateChapter(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("Negative chapter ID should fail")
    void updateChapter_negativeId() {
        assertThrows(IllegalArgumentException.class,
                () -> chapterController.updateChapter(VALID_NAME, request, -5L));
        verify(chapterService, never()).updateChapter(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("Null HTTP request should fail")
    void updateChapter_nullRequest() {
        assertThrows(NullPointerException.class,
                () -> chapterController.updateChapter(VALID_NAME, null, VALID_CHAPTER_ID));
        verify(chapterService, never()).updateChapter(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("Creator ID not found in request")
    void updateChapter_creatorIdMissing() {
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(null);

            assertThrows(NullPointerException.class,
                    () -> chapterController.updateChapter(VALID_NAME, request, VALID_CHAPTER_ID));
            verify(chapterService, never()).updateChapter(anyLong(), anyString(), anyLong());
        }
    }

    // ------------------ SERVICE EXCEPTION TESTS ------------------

    @Test
    @DisplayName("Chapter not found")
    void updateChapter_chapterNotFound() {
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doThrow(new ChapterNotFoundException(VALID_CHAPTER_ID))
                    .when(chapterService).updateChapter(VALID_CREATOR_ID, VALID_NAME, VALID_CHAPTER_ID);

            assertThrows(ChapterNotFoundException.class,
                    () -> chapterController.updateChapter(VALID_NAME, request, VALID_CHAPTER_ID));
        }
    }

    @Test
    @DisplayName("Creator not authorized")
    void updateChapter_creatorNotAuthorized() {
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doThrow(new UnauthorizedException("Not authorized"))
                    .when(chapterService).updateChapter(VALID_CREATOR_ID, VALID_NAME, VALID_CHAPTER_ID);

            assertThrows(UnauthorizedException.class,
                    () -> chapterController.updateChapter(VALID_NAME, request, VALID_CHAPTER_ID));
        }
    }

    @Test
    @DisplayName("Duplicate chapter name in course")
    void updateChapter_duplicateName() {
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doThrow(new DuplicateChapterNameException("Duplicate"))
                    .when(chapterService).updateChapter(VALID_CREATOR_ID, VALID_NAME, VALID_CHAPTER_ID);

            assertThrows(DuplicateChapterNameException.class,
                    () -> chapterController.updateChapter(VALID_NAME, request, VALID_CHAPTER_ID));
        }
    }

    @Test
    @DisplayName("Service throws unexpected exception")
    void updateChapter_serviceThrows() {
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doThrow(new RuntimeException("Unexpected"))
                    .when(chapterService).updateChapter(VALID_CREATOR_ID, VALID_NAME, VALID_CHAPTER_ID);

            assertThrows(RuntimeException.class,
                    () -> chapterController.updateChapter(VALID_NAME, request, VALID_CHAPTER_ID));
        }
    }

    // ------------------ SERVICE VERIFICATION ------------------

    @Test
    @DisplayName("Verify service called correctly")
    void updateChapter_serviceCalled() {
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doNothing().when(chapterService).updateChapter(VALID_CREATOR_ID, VALID_NAME, VALID_CHAPTER_ID);

            chapterController.updateChapter(VALID_NAME, request, VALID_CHAPTER_ID);
            verify(chapterService).updateChapter(VALID_CREATOR_ID, VALID_NAME, VALID_CHAPTER_ID);
        }
    }

    @Test
    @DisplayName("Verify service NOT called on validation fail")
    void updateChapter_serviceNotCalled_validationFail() {
        assertThrows(IllegalArgumentException.class,
                () -> chapterController.updateChapter("", request, 0L));
        verify(chapterService, never()).updateChapter(anyLong(), anyString(), anyLong());
    }
}
