package com.jpd.web.controller;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jpd.web.controller.creator.ModuleController;
import com.jpd.web.exception.InvalidOperationException;
import com.jpd.web.exception.ResourceNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.service.ModuleService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.servlet.http.HttpServletRequest;

public class ModuleControllerDeleteModuleTest {

    @Mock
    private ModuleService moduleService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ModuleController moduleController;

    private final long creatorId = 1L;
    private final long courseId = 10L;
    private final long chapterId = 100L;
    private final long moduleId = 1000L;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(request.getAttribute("creatorId")).thenReturn(creatorId);
    }

    @Nested
    class HappyPath {

        @Test
        void deleteModule_success() {
            moduleController.deleteModule(moduleId, chapterId, courseId, request);
            verify(moduleService, times(1)).deleteModule(creatorId, courseId, chapterId, moduleId);
        }

        @Test
        void deleteModule_largeIds() {
            long largeCreator = Long.MAX_VALUE;
            long largeCourse = Long.MAX_VALUE - 1;
            long largeChapter = Long.MAX_VALUE - 2;
            long largeModule = 1000L;
            when(request.getAttribute("creatorId")).thenReturn(largeCreator);

            moduleController.deleteModule(largeModule, largeChapter, largeCourse, request);

            verify(moduleService, times(1)).deleteModule(largeCreator, largeCourse, largeChapter, largeModule);
        }
    }

    @Nested
    class ErrorScenarios {

        @Test
        void deleteModule_moduleNotFound() {
            doThrow(new ResourceNotFoundException("Module not found"))
                    .when(moduleService).deleteModule(creatorId, courseId, chapterId, moduleId);

            assertThrows(ResourceNotFoundException.class,
                    () -> moduleController.deleteModule(moduleId, chapterId, courseId, request));
        }

        @Test
        void deleteModule_chapterNotFound() {
            doThrow(new ResourceNotFoundException("Chapter not found"))
                    .when(moduleService).deleteModule(creatorId, courseId, chapterId, moduleId);

            assertThrows(ResourceNotFoundException.class,
                    () -> moduleController.deleteModule(moduleId, chapterId, courseId, request));
        }

        @Test
        void deleteModule_creatorNotAuthorized() {
            doThrow(new UnauthorizedException("Unauthorized"))
                    .when(moduleService).deleteModule(creatorId, courseId, chapterId, moduleId);

            assertThrows(UnauthorizedException.class,
                    () -> moduleController.deleteModule(moduleId, chapterId, courseId, request));
        }

        @Test
        void deleteModule_moduleHasActiveEnrollments() {
            doThrow(new InvalidOperationException("Module has active enrollments"))
                    .when(moduleService).deleteModule(creatorId, courseId, chapterId, moduleId);

            assertThrows(InvalidOperationException.class,
                    () -> moduleController.deleteModule(moduleId, chapterId, courseId, request));
        }

        @Test
        void deleteModule_moduleInActiveCart() {
            doThrow(new InvalidOperationException("Module in active cart"))
                    .when(moduleService).deleteModule(creatorId, courseId, chapterId, moduleId);

            assertThrows(InvalidOperationException.class,
                    () -> moduleController.deleteModule(moduleId, chapterId, courseId, request));
        }

        @Test
        void deleteModule_moduleInWrongChapter() {
            doThrow(new InvalidOperationException("Module in wrong chapter"))
                    .when(moduleService).deleteModule(creatorId, courseId, chapterId, moduleId);

            assertThrows(InvalidOperationException.class,
                    () -> moduleController.deleteModule(moduleId, chapterId, courseId, request));
        }

        @Test
        void deleteModule_databaseFailure() {
            doThrow(new RuntimeException("DB error"))
                    .when(moduleService).deleteModule(creatorId, courseId, chapterId, moduleId);

            assertThrows(RuntimeException.class,
                    () -> moduleController.deleteModule(moduleId, chapterId, courseId, request));
        }

        @Test
        void deleteModule_concurrentDeletion() {
            doThrow(new ResourceNotFoundException("Already deleted"))
                    .when(moduleService).deleteModule(creatorId, courseId, chapterId, moduleId);

            assertThrows(ResourceNotFoundException.class,
                    () -> moduleController.deleteModule(moduleId, chapterId, courseId, request));
        }
    }
}
