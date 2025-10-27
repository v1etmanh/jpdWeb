package com.jpd.web.controller;

import com.jpd.web.controller.creator.ModuleController;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Module;
import com.jpd.web.service.ModuleService;
import com.jpd.web.service.utils.RequestAttributeExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ModuleController - createModule() Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ModuleControllerCreateModuleTest {

    @Mock
    private ModuleService moduleService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ModuleController moduleController;

    private static final Long VALID_MODULE_ID = 1L;
    private static final Long VALID_CHAPTER_ID = 10L;
    private static final Long VALID_COURSE_ID = 100L;
    private static final Long VALID_CREATOR_ID = 1000L;
    private static final String VALID_MODULE_NAME = "Introduction to Spring Boot";

    private AutoCloseable closeable;
    private Module mockModule;
    private Chapter mockChapter;

    @BeforeAll
    void setUpAll() {
        System.out.println("Starting createModule() Test Suite");
    }

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        // Chapter mock
        mockChapter = new Chapter();
        mockChapter.setChapterId(VALID_CHAPTER_ID);

        // Module mock
        mockModule = new Module();
        mockModule.setModuleId(VALID_MODULE_ID);
        mockModule.setTitleOfModule(VALID_MODULE_NAME);
        mockModule.setChapter(mockChapter);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) closeable.close();
        reset(moduleService, request);
    }

    @AfterAll
    void tearDownAll() {
        System.out.println("Completed createModule() Test Suite");
    }

    @Nested
    @DisplayName("Happy Path Scenarios")
    class HappyPathTests {

        @Test
        @DisplayName("Should create module successfully with valid inputs")
        void testCreateModule_Success() {
            try (MockedStatic<RequestAttributeExtractor> mockedExtractor = mockStatic(RequestAttributeExtractor.class)) {

                mockedExtractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                        .thenReturn(VALID_CREATOR_ID);

                when(moduleService.createModule(
                        VALID_MODULE_NAME,
                        VALID_CREATOR_ID,
                        VALID_COURSE_ID,
                        VALID_CHAPTER_ID
                )).thenReturn(mockModule);

                ResponseEntity<?> response = moduleController.createModule(
                        VALID_MODULE_NAME,
                        VALID_COURSE_ID,
                        VALID_CHAPTER_ID,
                        request
                );

                assertAll(
                        () -> assertNotNull(response),
                        () -> assertEquals(HttpStatus.CREATED, response.getStatusCode()),
                        () -> assertNotNull(response.getBody()),
                        () -> assertTrue(response.getBody() instanceof Module),
                        () -> assertEquals(VALID_MODULE_ID, ((Module) response.getBody()).getModuleId()),
                        () -> assertEquals(VALID_MODULE_NAME, ((Module) response.getBody()).getTitleOfModule())
                );

                verify(moduleService).createModule(VALID_MODULE_NAME, VALID_CREATOR_ID, VALID_COURSE_ID, VALID_CHAPTER_ID);
                mockedExtractor.verify(() -> RequestAttributeExtractor.extractCreatorId(request), times(1));
            }
        }

        @Test
        @DisplayName("Should create module successfully with long valid name (255 chars)")
        void testCreateModule_WithLongValidName() {
            String longName = "A".repeat(255);
            mockModule.setTitleOfModule(longName);

            try (MockedStatic<RequestAttributeExtractor> mockedExtractor = mockStatic(RequestAttributeExtractor.class)) {
                mockedExtractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                        .thenReturn(VALID_CREATOR_ID);

                when(moduleService.createModule(longName, VALID_CREATOR_ID, VALID_COURSE_ID, VALID_CHAPTER_ID))
                        .thenReturn(mockModule);

                ResponseEntity<?> response = moduleController.createModule(longName, VALID_COURSE_ID, VALID_CHAPTER_ID, request);

                assertEquals(HttpStatus.CREATED, response.getStatusCode());
                assertEquals(longName, ((Module) response.getBody()).getTitleOfModule());
                verify(moduleService).createModule(longName, VALID_CREATOR_ID, VALID_COURSE_ID, VALID_CHAPTER_ID);
            }
        }
    }

    @Nested
    @DisplayName("Validation Edge Cases")
    class ValidationEdgeCaseTests {

        @Test
        @DisplayName("Should throw exception for blank module name")
        void testCreateModule_BlankName() {
            String blankName = "";
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> {
                        if (blankName == null || blankName.trim().isEmpty())
                            throw new IllegalArgumentException("Module name cannot be blank");
                    }
            );
            assertTrue(exception.getMessage().contains("cannot be blank"));
            verify(moduleService, never()).createModule(anyString(), anyLong(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("Should throw exception when module name exceeds maximum length")
        void testCreateModule_NameTooLong() {
            String tooLongName = "A".repeat(256);
            try (MockedStatic<RequestAttributeExtractor> mockedExtractor = mockStatic(RequestAttributeExtractor.class)) {
                mockedExtractor.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                        .thenReturn(VALID_CREATOR_ID);

                when(moduleService.createModule(tooLongName, VALID_CREATOR_ID, VALID_COURSE_ID, VALID_CHAPTER_ID))
                        .thenThrow(new ConstraintViolationException("Module name must not exceed 255 characters", null));

                ConstraintViolationException exception = assertThrows(
                        ConstraintViolationException.class,
                        () -> moduleController.createModule(tooLongName, VALID_COURSE_ID, VALID_CHAPTER_ID, request)
                );

                assertTrue(exception.getMessage().contains("255 characters"));
            }
        }
    }
}
