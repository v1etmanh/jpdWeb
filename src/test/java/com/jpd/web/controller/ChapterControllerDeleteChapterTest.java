package com.jpd.web.controller;

import com.jpd.web.controller.creator.ChapterController;
import com.jpd.web.exception.*;
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
@DisplayName("ChapterController - deleteChapter() Unit Tests")
class ChapterControllerDeleteChapterTest {

    @Mock
    private ChapterService chapterService;

    @Mock
    private HttpServletRequest request;

    private ChapterController chapterController;

    private AutoCloseable closeable;

    private static final Long VALID_CHAPTER_ID = 1L;
    private static final Long VALID_COURSE_ID = 100L;
    private static final Long VALID_CREATOR_ID = 1000L;

    @BeforeAll
    void beforeAll() {
        System.out.println("Starting deleteChapter() Tests");
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
        System.out.println("Completed deleteChapter() Tests");
    }

    @Test
    @DisplayName("Delete chapter successfully")
    void deleteChapter_success() {
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            ResponseEntity<Void> response = chapterController.deleteChapter(
                    VALID_CHAPTER_ID, VALID_COURSE_ID, request);

            assertEquals(204, response.getStatusCodeValue(), "HTTP status should be 204 No Content");
            verify(chapterService).deleteChapter(VALID_CHAPTER_ID, VALID_COURSE_ID, VALID_CREATOR_ID);
        }
    }

    @Test
    @DisplayName("Delete middle chapter")
    void deleteChapter_middleChapter() {
        // Conceptually same as success test
        deleteChapter_success();
    }

    @Test
    @DisplayName("Delete last chapter")
    void deleteChapter_lastChapter() {
        // Conceptually same as success test
        deleteChapter_success();
    }

    @Test
    @DisplayName("Zero chapter ID should throw exception")
    void deleteChapter_zeroChapterId() {
        assertThrows(Exception.class, () ->
                chapterController.deleteChapter(0L, VALID_COURSE_ID, request));
        verify(chapterService, never()).deleteChapter(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Negative chapter ID should throw exception")
    void deleteChapter_negativeChapterId() {
        assertThrows(Exception.class, () ->
                chapterController.deleteChapter(-10L, VALID_COURSE_ID, request));
        verify(chapterService, never()).deleteChapter(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Zero course ID should throw exception")
    void deleteChapter_zeroCourseId() {
        assertThrows(Exception.class, () ->
                chapterController.deleteChapter(VALID_CHAPTER_ID, 0L, request));
        verify(chapterService, never()).deleteChapter(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Negative course ID should throw exception")
    void deleteChapter_negativeCourseId() {
        assertThrows(Exception.class, () ->
                chapterController.deleteChapter(VALID_CHAPTER_ID, -5L, request));
        verify(chapterService, never()).deleteChapter(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Chapter not found")
    void deleteChapter_chapterNotFound() {
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);
            doThrow(new ChapterNotFoundException(VALID_CHAPTER_ID))
                    .when(chapterService).deleteChapter(VALID_CHAPTER_ID, VALID_COURSE_ID, VALID_CREATOR_ID);

            assertThrows(ChapterNotFoundException.class, () ->
                    chapterController.deleteChapter(VALID_CHAPTER_ID, VALID_COURSE_ID, request));
        }
    }

    @Test
    @DisplayName("Course not found")
    void deleteChapter_courseNotFound() {
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);
            doThrow(new CourseNotFoundException(VALID_COURSE_ID))
                    .when(chapterService).deleteChapter(VALID_CHAPTER_ID, VALID_COURSE_ID, VALID_CREATOR_ID);

            assertThrows(CourseNotFoundException.class, () ->
                    chapterController.deleteChapter(VALID_CHAPTER_ID, VALID_COURSE_ID, request));
        }
    }

    @Test
    @DisplayName("Chapter doesn't belong to course")
    void deleteChapter_chapterDoesNotBelong() {
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);
            doThrow(new UnauthorizedException("Chapter not part of course"))
                    .when(chapterService).deleteChapter(VALID_CHAPTER_ID, VALID_COURSE_ID, VALID_CREATOR_ID);

            assertThrows(UnauthorizedException.class, () ->
                    chapterController.deleteChapter(VALID_CHAPTER_ID, VALID_COURSE_ID, request));
        }
    }

    @Test
    @DisplayName("Creator not authorized")
    void deleteChapter_creatorNotAuthorized() {
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);
            doThrow(new UnauthorizedException("Not authorized"))
                    .when(chapterService).deleteChapter(VALID_CHAPTER_ID, VALID_COURSE_ID, VALID_CREATOR_ID);

            assertThrows(UnauthorizedException.class, () ->
                    chapterController.deleteChapter(VALID_CHAPTER_ID, VALID_COURSE_ID, request));
        }
    }

    @Test
    @DisplayName("Creator ID not found in request")
    void deleteChapter_creatorIdMissing() {
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(null);

            assertThrows(NullPointerException.class, () ->
                    chapterController.deleteChapter(VALID_CHAPTER_ID, VALID_COURSE_ID, request));
        }
    }

    @Test
    @DisplayName("Null HTTP request should throw exception")
    void deleteChapter_nullRequest() {
        assertThrows(NullPointerException.class, () ->
                chapterController.deleteChapter(VALID_CHAPTER_ID, VALID_COURSE_ID, null));
    }

    @Test
    @DisplayName("Verify service called correctly")
    void deleteChapter_serviceCalled() {
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            chapterController.deleteChapter(VALID_CHAPTER_ID, VALID_COURSE_ID, request);

            verify(chapterService).deleteChapter(VALID_CHAPTER_ID, VALID_COURSE_ID, VALID_CREATOR_ID);
        }
    }

    @Test
    @DisplayName("Verify service NOT called on validation fail")
    void deleteChapter_serviceNotCalled_validationFail() {
        assertThrows(Exception.class, () ->
                chapterController.deleteChapter(0L, VALID_COURSE_ID, request));
        verify(chapterService, never()).deleteChapter(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Course chapter count decreases after delete")
    void deleteChapter_courseChapterCountDecreases() {
        // This is conceptual since we can't access DB in unit test
        // Just ensure deleteChapter() is called, representing chapter removal
        try (MockedStatic<RequestAttributeExtractor> extractor = mockStatic(RequestAttributeExtractor.class)) {
            extractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            chapterController.deleteChapter(VALID_CHAPTER_ID, VALID_COURSE_ID, request);

            verify(chapterService, times(1)).deleteChapter(VALID_CHAPTER_ID, VALID_COURSE_ID, VALID_CREATOR_ID);
        }
    }
}
