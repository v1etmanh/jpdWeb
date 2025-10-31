package com.jpd.web.UnitTest;

import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.*;
import com.jpd.web.repository.ChapterRepository;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.repository.ModuleRepository;
import com.jpd.web.service.ChapterService;
import com.jpd.web.service.utils.ValidationResources;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChapterService - Unit Tests (Fixed)")
class ChapterServiceUnitTest {

    @Mock private ChapterRepository chapterRepo;
    @Mock private ModuleRepository moduleRepo;
    @Mock private ModuleContentRepository moduleContentRepo;
    @Mock private ValidationResources validator;

    @InjectMocks private ChapterService chapterService;

    private Creator creator;
    private Course course;
    private Chapter chapter;

    @BeforeEach
    void setUp() {
        // ensure builder field names match your entity (chapterName, creatorId, etc)
        creator = Creator.builder().creatorId(1L).build();
        course = Course.builder().courseId(10L).creator(creator).build();

        chapter = Chapter.builder()
                .chapterId(100L)
                .ChapterName("Old")   // <-- dùng field đúng: chapterName
                .course(course)
                .modules(new ArrayList<>())
                .build();
    }

    // ================================================================
    @Nested
    @DisplayName("createChapter()")
    class CreateChapter {

        @Test
        @DisplayName("Valid ownership → creates chapter successfully")
        void createChapter_Success() {
            when(validator.validateCourseOwnership(10L, 1L)).thenReturn(course);
            when(chapterRepo.save(any())).thenAnswer(i -> {
                Chapter c = i.getArgument(0);
                c.setChapterId(100L);
                return c;
            });

            Chapter result = chapterService.createChapter("Ch1", 10L, 1L);

            assertEquals("Ch1", result.getChapterName());
            verify(chapterRepo).save(any());
        }

        @Test
        @DisplayName("Course not owned → throws UnauthorizedException")
        void createChapter_CourseNotOwned_Throws() {
            when(validator.validateCourseOwnership(10L, 2L))
                    .thenThrow(new UnauthorizedException("Not owner"));

            assertThrows(UnauthorizedException.class, () ->
                    chapterService.createChapter("Ch1", 10L, 2L));
        }
    }

    // ================================================================
    @Nested
    @DisplayName("updateChapter()")
    class UpdateChapter {

        @Test
        @DisplayName("Valid ownership → updates name")
        void updateChapter_Success() {
            when(validator.validateCreatorExists(1L)).thenReturn(creator);
            when(chapterRepo.findById(100L)).thenReturn(Optional.of(chapter));
            when(chapterRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            chapterService.updateChapter(1L, "New Name", 100L);

            assertEquals("New Name", chapter.getChapterName());
            verify(chapterRepo).save(chapter);
        }

        @Test
        @DisplayName("Chapter not found → throws ModuleNotFoundException")
        void updateChapter_ChapterNotFound_Throws() {
            when(validator.validateCreatorExists(1L)).thenReturn(creator);
            when(chapterRepo.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ModuleNotFoundException.class, () ->
                    chapterService.updateChapter(1L, "Name", 999L));
        }

        @Test
        @DisplayName("Not owner → throws UnauthorizedException")
        void updateChapter_NotOwner_Throws() {
            Creator otherCreator = Creator.builder().creatorId(2L).build();
            Course otherCourse = Course.builder().courseId(20L).creator(otherCreator).build();
            Chapter otherChapter = Chapter.builder()
                    .chapterId(100L)
                    .ChapterName("Old")   // <-- field name fixed
                    .course(otherCourse)
                    .modules(new ArrayList<>())
                    .build();

            when(validator.validateCreatorExists(1L)).thenReturn(creator);
            when(chapterRepo.findById(100L)).thenReturn(Optional.of(otherChapter));

            assertThrows(UnauthorizedException.class, () ->
                    chapterService.updateChapter(1L, "Name", 100L));

            verify(chapterRepo, never()).save(any());
        }
    }

    // ================================================================
    @Nested
    @DisplayName("deleteChapter()")
    class DeleteChapter {

        @Test
        @DisplayName("Valid ownership → deletes chapter successfully")
        void deleteChapter_Success() {
            when(validator.validateCourseOwnership(10L, 1L)).thenReturn(course);
            when(validator.validateChapterBelongsToCourse(100L, 10L)).thenReturn(chapter);

            chapterService.deleteChapter(100L, 10L, 1L);

            // chapter has no modules -> no moduleContent deletion
            verify(moduleContentRepo, times(0)).deleteByModuleId(any());
            verify(moduleRepo).deleteByChapterChapterId(100L);
            verify(chapterRepo).deleteByChapterId(100L);
        }

        @Test
        @DisplayName("Not owner → throws UnauthorizedException (simulate validator) ")
        void deleteChapter_NotOwner_Throws() {
            // Simulate validator refusing access at course-ownership check
            when(validator.validateCourseOwnership(10L, 1L))
                    .thenThrow(new UnauthorizedException("Not owner"));

            assertThrows(UnauthorizedException.class, () ->
                    chapterService.deleteChapter(100L, 10L, 1L));

            // No delete calls should happen
            verify(moduleRepo, never()).deleteByChapterChapterId(anyLong());
            verify(chapterRepo, never()).deleteByChapterId(anyLong());
        }

        @Test
        @DisplayName("Chapter not found → throws ModuleNotFoundException")
        void deleteChapter_ChapterNotFound_Throws() {
            when(validator.validateCourseOwnership(10L, 1L)).thenReturn(course);
            when(validator.validateChapterBelongsToCourse(999L, 10L))
                    .thenThrow(new ModuleNotFoundException(999L));

            assertThrows(ModuleNotFoundException.class, () ->
                    chapterService.deleteChapter(999L, 10L, 1L));

            verify(chapterRepo, never()).deleteByChapterId(anyLong());
        }

        @Test
        @DisplayName("Course not found → throws RuntimeException")
        void deleteChapter_CourseNotFound_Throws() {
            when(validator.validateCourseOwnership(99L, 1L))
                    .thenThrow(new RuntimeException("Course not found"));

            assertThrows(RuntimeException.class, () ->
                    chapterService.deleteChapter(100L, 99L, 1L));

            verifyNoInteractions(chapterRepo);
        }
    }
}
