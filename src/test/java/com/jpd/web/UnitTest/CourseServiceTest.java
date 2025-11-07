package com.jpd.web.UnitTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.dto.CourseFormDto;
import com.jpd.web.dto.PopularCourseDTO;
import com.jpd.web.exception.CreatorNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Status;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.service.CourseService;
import com.jpd.web.service.FireBaseService;
import com.jpd.web.service.utils.CodeGenerator;
import com.jpd.web.service.utils.ValidationResources;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseService Tests")
class CourseServiceTest {

    @Mock
    private FireBaseService fireBaseService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CreatorRepository creatorRepository;

    @Mock
    private ValidationResources resourceValidator;

    @Mock
    private CodeGenerator codeGenerator;

    @Mock
    private MultipartFile mockImgFile;

    @InjectMocks
    private CourseService courseService;

    private Creator mockCreator;
    private CourseFormDto courseFormDto;
    private Course mockCourse;

    @BeforeEach
    void setUp() {
        // Setup mock Creator
        mockCreator = new Creator();
        ReflectionTestUtils.setField(mockCreator, "creatorId", 1L);
        mockCreator.setStatus(Status.SUCCESS);

        // Setup mock CourseFormDto
        courseFormDto = new CourseFormDto();
        courseFormDto.setName("Test Course");
        courseFormDto.setDescription("Test Description");
        courseFormDto.setImgFile(mockImgFile);

        // Setup mock Course
        mockCourse = new Course();
        mockCourse.setCourseId(1L);
        mockCourse.setName("Test Course");
        mockCourse.setCreator(mockCreator);
    }

    /**
     * Helper method to read CSV test data
     */
    private List<Map<String, String>> readTestDataFromCSV(String fileName) throws IOException {
        List<Map<String, String>> testData = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource(fileName).getInputStream()))) {

            String headerLine = br.readLine();
            if (headerLine == null) {
                return testData;
            }

            String[] headers = headerLine.split(",");
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",", -1); // -1 to keep empty trailing strings
                Map<String, String> row = new HashMap<>();

                for (int i = 0; i < headers.length && i < values.length; i++) {
                    row.put(headers[i].trim(), values[i].trim());
                }

                testData.add(row);
            }
        }

        return testData;
    }

    @Nested
    @DisplayName("createCourse() Tests - Data Driven from CSV")
    class CreateCourseTestsFromCSV {

        @TestFactory
        @DisplayName("Run all createCourse test cases from CSV")
        List<DynamicTest> testCreateCourseFromCSV() throws IOException {
            List<Map<String, String>> testData = readTestDataFromCSV("test-data-courses.csv");
            List<DynamicTest> dynamicTests = new ArrayList<>();

            for (Map<String, String> data : testData) {
                String testCase = data.get("testCase");

                DynamicTest test = DynamicTest.dynamicTest(testCase, () -> {
                    executeTestCase(data);
                });

                dynamicTests.add(test);
            }

            return dynamicTests;
        }

        private void executeTestCase(Map<String, String> data) throws IOException {
            // Reset mocks
            reset(resourceValidator, fireBaseService, codeGenerator, courseRepository);

            // Parse test data
            Long creatorId = Long.parseLong(data.get("creatorId"));
            Status creatorStatus = Status.valueOf(data.get("creatorStatus"));
            String courseName = data.get("courseName");
            String courseDescription = data.get("courseDescription");
            AccessMode accessMode = AccessMode.valueOf(data.get("accessMode"));
            Double price = Double.parseDouble(data.get("price"));
            String expectedJoinKey = data.get("expectedJoinKey");
            String expectedImgUrl = data.get("expectedImgUrl");
            boolean shouldSucceed = Boolean.parseBoolean(data.get("shouldSucceed"));
            String expectedErrorMessage = data.get("expectedErrorMessage");

            // Setup test data
            Creator testCreator = new Creator();
            ReflectionTestUtils.setField(testCreator, "creatorId", creatorId);
            testCreator.setStatus(creatorStatus);

            CourseFormDto testForm = new CourseFormDto();
            testForm.setName(courseName);
            testForm.setDescription(courseDescription);
            testForm.setAccessMode(accessMode);
            testForm.setPrice(price);
            testForm.setImgFile(mockImgFile);

            // Setup mocks based on test case
            if (creatorId == 999L) {
                when(resourceValidator.validateCreatorExists(creatorId))
                        .thenThrow(new CreatorNotFoundException(creatorId));
            } else {
                when(resourceValidator.validateCreatorExists(creatorId)).thenReturn(testCreator);
            }

            if (!expectedImgUrl.isEmpty()) {
                if ("null".equals(expectedImgUrl)) {
                    lenient().when(fireBaseService.uploadFile(mockImgFile, TypeOfFile.IMG)).thenReturn(null);
                } else if ("EXCEPTION".equals(expectedImgUrl)) {
                    lenient().when(fireBaseService.uploadFile(mockImgFile, TypeOfFile.IMG))
                            .thenThrow(new RuntimeException("Firebase connection error"));
                } else {
                    lenient().when(fireBaseService.uploadFile(mockImgFile, TypeOfFile.IMG)).thenReturn(expectedImgUrl);
                }
            }

            if (!expectedJoinKey.isEmpty()) {
                lenient().when(codeGenerator.generate6DigitCode()).thenReturn(expectedJoinKey);
            }

            lenient().when(courseRepository.save(any(Course.class))).thenAnswer(i -> {
                Course course = i.getArgument(0);
                course.setCourseId(1L);
                return course;
            });

            // Execute test and verify
            if (shouldSucceed) {
                Course result = courseService.createCourse(testForm, creatorId);

                assertNotNull(result);
                assertEquals(courseName, result.getName());
                if (!expectedImgUrl.isEmpty() && !"null".equals(expectedImgUrl)) {
                    assertEquals(expectedImgUrl, result.getUrlImg());
                }
                if (!expectedJoinKey.isEmpty()) {
                    assertEquals(expectedJoinKey, result.getJoinKey());
                }
            } else {
                Exception exception = assertThrows(Exception.class,
                        () -> courseService.createCourse(testForm, creatorId));

                if (!expectedErrorMessage.isEmpty()) {
                    assertTrue(exception.getMessage().contains(expectedErrorMessage),
                            "Expected error message to contain: " + expectedErrorMessage +
                                    " but got: " + exception.getMessage());
                }
            }
        }
    }

    @Nested
    @DisplayName("createCourse() Tests - Original Individual Tests")
    class CreateCourseTests {

        @Test
        @DisplayName("Should create FREE course successfully")
        void shouldCreateFreeCourseSuccessfully() throws IOException {
            // Given
            courseFormDto.setAccessMode(AccessMode.PUBLIC);
            courseFormDto.setPrice(0.0);

            String expectedImgUrl = "https://firebase.storage/test-image.jpg";
            String expectedJoinKey = "ABC123";

            when(resourceValidator.validateCreatorExists(1L)).thenReturn(mockCreator);
            when(fireBaseService.uploadFile(mockImgFile, TypeOfFile.IMG)).thenReturn(expectedImgUrl);
            when(codeGenerator.generate6DigitCode()).thenReturn(expectedJoinKey);
            when(courseRepository.save(any(Course.class))).thenAnswer(i -> {
                Course course = i.getArgument(0);
                course.setCourseId(1L);
                return course;
            });

            // When
            Course result = courseService.createCourse(courseFormDto, 1L);

            // Then
            assertNotNull(result);
            assertEquals("Test Course", result.getName());
            assertEquals(expectedImgUrl, result.getUrlImg());
            assertEquals(expectedJoinKey, result.getJoinKey());
            assertEquals(mockCreator, result.getCreator());

            verify(resourceValidator).validateCreatorExists(1L);
            verify(fireBaseService).uploadFile(mockImgFile, TypeOfFile.IMG);
            verify(codeGenerator).generate6DigitCode();
            verify(courseRepository).save(any(Course.class));
        }

        @Test
        @DisplayName("Should create PAID course with verified creator")
        void shouldCreatePaidCourseWithVerifiedCreator() throws IOException {
            // Given
            courseFormDto.setAccessMode(AccessMode.PAID);
            courseFormDto.setPrice(99.99);

            String expectedImgUrl = "https://firebase.storage/paid-course.jpg";
            mockCreator.setStatus(Status.SUCCESS);

            when(resourceValidator.validateCreatorExists(1L)).thenReturn(mockCreator);
            when(fireBaseService.uploadFile(mockImgFile, TypeOfFile.IMG)).thenReturn(expectedImgUrl);
            when(codeGenerator.generate6DigitCode()).thenReturn("XYZ789");
            when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

            // When
            Course result = courseService.createCourse(courseFormDto, 1L);

            // Then
            assertNotNull(result);
            assertEquals(AccessMode.PAID, result.getAccessMode());
            assertEquals(expectedImgUrl, result.getUrlImg());
            verify(courseRepository).save(any(Course.class));
        }
    }

    @Nested
    @DisplayName("retrieveCourseByemail() Tests")
    class RetrieveCourseByEmailTests {

        @Test
        @DisplayName("Should retrieve all courses for creator")
        void shouldRetrieveAllCoursesForCreator() {
            // Given
            Course course1 = new Course();
            course1.setCourseId(1L);
            course1.setName("Course 1");

            Course course2 = new Course();
            course2.setCourseId(2L);
            course2.setName("Course 2");

            mockCreator.setCourses(Arrays.asList(course1, course2));

            when(creatorRepository.findById(1L)).thenReturn(Optional.of(mockCreator));

            // When
            List<CourseCardDto> result = courseService.retrieveCourseByemail(1L);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(creatorRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return empty list when creator has no courses")
        void shouldReturnEmptyListWhenCreatorHasNoCourses() {
            // Given
            mockCreator.setCourses(new ArrayList<>());
            when(creatorRepository.findById(1L)).thenReturn(Optional.of(mockCreator));

            // When
            List<CourseCardDto> result = courseService.retrieveCourseByemail(1L);

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("getCourseById() Tests")
    class GetCourseByIdTests {

        @Test
        @DisplayName("Should retrieve course with chapters and modules")
        void shouldRetrieveCourseWithChaptersAndModules() {
            // Given
            Chapter chapter = new Chapter();
            chapter.setChapterId(1L);
            chapter.setChapterName("Chapter 1");

            mockCourse.setChapters(Arrays.asList(chapter));

            when(resourceValidator.validateCourseOwnership(1L, 1L)).thenReturn(mockCourse);

            // When
            CourseContentDto result = courseService.getCourseById(1L, 1L);

            // Then
            assertNotNull(result);
            verify(resourceValidator).validateCourseOwnership(1L, 1L);
        }

        @Test
        @DisplayName("Should throw exception when creator doesn't own course")
        void shouldThrowExceptionWhenCreatorDoesntOwnCourse() {
            // Given
            when(resourceValidator.validateCourseOwnership(1L, 2L))
                    .thenThrow(new UnauthorizedException("You don't have permission to modify this course"));

            // When & Then
            assertThrows(
                    UnauthorizedException.class,
                    () -> courseService.getCourseById(1L, 2L)
            );
        }
    }

    @Nested
    @DisplayName("retrieveCCourse() Tests")
    class RetrieveCCourseTests {

        @Test
        @DisplayName("Should retrieve popular courses for creator")
        void shouldRetrievePopularCoursesForCreator() {
            // Given
            Course course1 = new Course();
            course1.setCourseId(1L);
            mockCreator.setCourses(Arrays.asList(course1));

            when(resourceValidator.validateCreatorExists(1L)).thenReturn(mockCreator);

            // When
            List<PopularCourseDTO> result = courseService.retrieveCCourse(1L);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(resourceValidator).validateCreatorExists(1L);
        }
    }

    @Nested
    @DisplayName("changeCourseSatus() Tests")
    class ChangeCourseStatusTests {

        @Test
        @DisplayName("Should toggle course public status from false to true")
        void shouldToggleCourseStatusFromFalseToTrue() {
            // Given
            mockCourse.setPublic(false);
            when(resourceValidator.validateCourseOwnership(1L, 1L)).thenReturn(mockCourse);
            when(courseRepository.save(any(Course.class))).thenReturn(mockCourse);

            // When
            courseService.changeCourseSatus(1L, 1L);

            // Then
            assertTrue(mockCourse.isPublic());
            verify(courseRepository).save(mockCourse);
        }

        @Test
        @DisplayName("Should toggle course public status from true to false")
        void shouldToggleCourseStatusFromTrueToFalse() {
            // Given
            mockCourse.setPublic(true);
            when(resourceValidator.validateCourseOwnership(1L, 1L)).thenReturn(mockCourse);
            when(courseRepository.save(any(Course.class))).thenReturn(mockCourse);

            // When
            courseService.changeCourseSatus(1L, 1L);

            // Then
            assertFalse(mockCourse.isPublic());
            verify(courseRepository).save(mockCourse);
        }
    }
}