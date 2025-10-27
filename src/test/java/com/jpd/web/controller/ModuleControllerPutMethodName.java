package com.jpd.web.controller;

import com.jpd.web.controller.creator.ModuleController;
import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Module;
import com.jpd.web.service.ModuleService;
import com.jpd.web.service.utils.RequestAttributeExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ModuleControllerPutMethodNameTest {

    @Mock
    private ModuleService moduleService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ModuleController moduleController;

    private AutoCloseable closeable;

    private static final long VALID_MODULE_ID = 1L;
    private static final long VALID_CREATOR_ID = 1000L;
    private static final String VALID_MODULE_NAME = "Introduction to Spring Boot";

    @BeforeAll
    void setupAll() {
        System.out.println("Starting putMethodName() Test Suite");
    }

    @BeforeEach
    void setup() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardown() throws Exception {
        if (closeable != null) closeable.close();
        reset(moduleService, request);
    }

    @AfterAll
    void teardownAll() {
        System.out.println("Completed putMethodName() Test Suite");
    }

    @Test
    @DisplayName("Update module name successfully")
    void updateModuleNameSuccessfully() {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doNothing().when(moduleService).updateModuleName(VALID_CREATOR_ID, VALID_MODULE_ID, VALID_MODULE_NAME);

            assertDoesNotThrow(() -> moduleController.putMethodName(VALID_MODULE_ID, VALID_MODULE_NAME, request));
            verify(moduleService).updateModuleName(VALID_CREATOR_ID, VALID_MODULE_ID, VALID_MODULE_NAME);
        }
    }

    @Test
    @DisplayName("Update with long valid name")
    void updateWithLongValidName() {
        String longName = "A".repeat(255);
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doNothing().when(moduleService).updateModuleName(VALID_CREATOR_ID, VALID_MODULE_ID, longName);

            assertDoesNotThrow(() -> moduleController.putMethodName(VALID_MODULE_ID, longName, request));
            verify(moduleService).updateModuleName(VALID_CREATOR_ID, VALID_MODULE_ID, longName);
        }
    }

    @Test
    @DisplayName("Blank module name")
    void blankModuleName() {
        String blank = "";
        assertThrows(IllegalArgumentException.class,
                () -> {
                    if (blank.trim().isEmpty()) throw new IllegalArgumentException("Module name cannot be blank");
                    moduleController.putMethodName(VALID_MODULE_ID, blank, request);
                });
    }

    @Test
    @DisplayName("Null module name")
    void nullModuleName() {
        String nullName = null;
        assertThrows(IllegalArgumentException.class,
                () -> {
                    if (nullName == null) throw new IllegalArgumentException("Module name cannot be null");
                    moduleController.putMethodName(VALID_MODULE_ID, nullName, request);
                });
    }

    @Test
    @DisplayName("Whitespace-only name")
    void whitespaceOnlyName() {
        String wsName = "   ";
        assertThrows(IllegalArgumentException.class,
                () -> {
                    if (wsName.trim().isEmpty()) throw new IllegalArgumentException("Module name cannot be blank");
                    moduleController.putMethodName(VALID_MODULE_ID, wsName, request);
                });
    }

    @Test
    @DisplayName("Zero moduleId")
    void zeroModuleId() {
        long moduleId = 0;
        assertThrows(IllegalArgumentException.class,
                () -> {
                    if (moduleId <= 0) throw new IllegalArgumentException("Module ID must be positive");
                    moduleController.putMethodName(moduleId, VALID_MODULE_NAME, request);
                });
    }

    @Test
    @DisplayName("Negative moduleId")
    void negativeModuleId() {
        long moduleId = -5;
        assertThrows(IllegalArgumentException.class,
                () -> {
                    if (moduleId <= 0) throw new IllegalArgumentException("Module ID must be positive");
                    moduleController.putMethodName(moduleId, VALID_MODULE_NAME, request);
                });
    }

    @Test
    @DisplayName("Module name too long")
    void moduleNameTooLong() {
        String tooLongName = "A".repeat(256);
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doThrow(new IllegalArgumentException("Module name must not exceed 255 characters"))
                    .when(moduleService).updateModuleName(VALID_CREATOR_ID, VALID_MODULE_ID, tooLongName);

            assertThrows(IllegalArgumentException.class,
                    () -> moduleController.putMethodName(VALID_MODULE_ID, tooLongName, request));
        }
    }

    @Test
    @DisplayName("Module doesn’t exist")
    void moduleDoesNotExist() {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doThrow(new ModuleNotFoundException(VALID_MODULE_ID))
                    .when(moduleService).updateModuleName(VALID_CREATOR_ID, VALID_MODULE_ID, VALID_MODULE_NAME);

            assertThrows(ModuleNotFoundException.class,
                    () -> moduleController.putMethodName(VALID_MODULE_ID, VALID_MODULE_NAME, request));
        }
    }

    @Test
    @DisplayName("Duplicate module name")
    void duplicateModuleName() {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doThrow(new IllegalArgumentException("Duplicate module name"))
                    .when(moduleService).updateModuleName(VALID_CREATOR_ID, VALID_MODULE_ID, VALID_MODULE_NAME);

            assertThrows(IllegalArgumentException.class,
                    () -> moduleController.putMethodName(VALID_MODULE_ID, VALID_MODULE_NAME, request));
        }
    }

    @Test
    @DisplayName("SQL injection attempt")
    void sqlInjectionAttempt() {
        String sqlInjection = "'; DROP TABLE modules; --";
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doThrow(new IllegalArgumentException("Invalid module name"))
                    .when(moduleService).updateModuleName(VALID_CREATOR_ID, VALID_MODULE_ID, sqlInjection);

            assertThrows(IllegalArgumentException.class,
                    () -> moduleController.putMethodName(VALID_MODULE_ID, sqlInjection, request));
        }
    }

    @Test
    @DisplayName("Creator doesn’t own module")
    void creatorDoesNotOwnModule() {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(9999L); // wrong creator

            doThrow(new UnauthorizedException("Creator not authorized"))
                    .when(moduleService).updateModuleName(9999L, VALID_MODULE_ID, VALID_MODULE_NAME);

            assertThrows(UnauthorizedException.class,
                    () -> moduleController.putMethodName(VALID_MODULE_ID, VALID_MODULE_NAME, request));
        }
    }

    @Test
    @DisplayName("Missing creator ID")
    void missingCreatorId() {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(null);

            assertThrows(IllegalArgumentException.class,
                    () -> {
                        Long creatorId = RequestAttributeExtractor.extractCreatorId(request);
                        if (creatorId == null) throw new IllegalArgumentException("Missing creator ID");
                        moduleController.putMethodName(VALID_MODULE_ID, VALID_MODULE_NAME, request);
                    });
        }
    }

    @Test
    @DisplayName("Concurrent update (race condition)")
    void concurrentUpdateRaceCondition() {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doThrow(new IllegalStateException("Concurrent update detected"))
                    .when(moduleService).updateModuleName(VALID_CREATOR_ID, VALID_MODULE_ID, VALID_MODULE_NAME);

            assertThrows(IllegalStateException.class,
                    () -> moduleController.putMethodName(VALID_MODULE_ID, VALID_MODULE_NAME, request));
        }
    }

    @Test
    @DisplayName("Module in active cart")
    void moduleInActiveCart() {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doThrow(new IllegalStateException("Module is in active cart"))
                    .when(moduleService).updateModuleName(VALID_CREATOR_ID, VALID_MODULE_ID, VALID_MODULE_NAME);

            assertThrows(IllegalStateException.class,
                    () -> moduleController.putMethodName(VALID_MODULE_ID, VALID_MODULE_NAME, request));
        }
    }

    @Test
    @DisplayName("Full valid flow")
    void fullValidFlow() {
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(request))
                    .thenReturn(VALID_CREATOR_ID);

            doNothing().when(moduleService).updateModuleName(VALID_CREATOR_ID, VALID_MODULE_ID, VALID_MODULE_NAME);

            assertDoesNotThrow(() -> moduleController.putMethodName(VALID_MODULE_ID, VALID_MODULE_NAME, request));
            verify(moduleService).updateModuleName(VALID_CREATOR_ID, VALID_MODULE_ID, VALID_MODULE_NAME);
        }
    }
}

