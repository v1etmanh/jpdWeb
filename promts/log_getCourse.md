# Prompt Log


## 2025-10-25 — ID: 2025-10-25-001

## Title:  for getAll course , get course by id, get paid course of creator methods in CourseService class
Prompt 1:

* Input:
  Analyze this Java Spring @Service class named "CourseService" and identify all public methods that need unit testing.
  I am specifically interested in testing these 3 methods:

1. retrieveCourseByemail(long creatorId)
2. getCourseById(long courseId, long creatorId)
3. retrieveCCourse(long creatorId)

[attached file]

For *each* of those 3 methods, identify:

1. Main business logic
2. Input parameters
3. Expected return value
4. Potential edge cases (e.g., null inputs, empty lists, ID not found)
5. External dependencies (like @Autowired Repositories or other Services) that need mocking with Mockito

* AI output:


1. retrieveCourseByemail(long creatorId)

   Main business logic: Retrieves a Creator by their ID. It then fetches the list of Course objects associated with that
   creator and transforms each Course into a CourseCardDto.

   Input parameters: long creatorId

   Expected return value: List<CourseCardDto>

   Potential edge cases:

        Creator not found: The creatorRepository.findById(creatorId) returns an Optional.empty(). The current code will throw a NoSuchElementException because it calls .get() without checking isPresent().

        Creator found, but has no courses: The c.get().getCourses() returns an empty list. The method should successfully return an empty List<CourseCardDto>.

   External dependencies (to mock):

        CreatorRepository: Specifically, the findById(long creatorId) method.

2. getCourseById(long courseId, long creatorId)

   Main business logic: Validates that the course exists and is owned by the specified creator using
   validateCourseOwnership. It then (attempts to) eagerly load chapters/modules and transforms the full Course entity
   into a CourseContentDto.

   Input parameters: long courseId, long creatorId

   Expected return value: CourseContentDto

   Potential edge cases:

        Course not found: The resourceValidator.validateCourseOwnership should throw an exception (e.g., ResourceNotFoundException).

        Creator does not own the course: The resourceValidator.validateCourseOwnership should throw an exception (e.g., UnauthorizedException).

        Course exists but has no chapters: The method should return a CourseContentDto with an empty list of chapters.

   External dependencies (to mock):

        ValidationResources: Specifically, the validateCourseOwnership(long courseId, long creatorId) method.

3. retrieveCCourse(long creatorId)

   Main business logic: Validates that the creator exists using validateCreatorExists. It then retrieves all courses for
   that creator, filters the list to include only those with AccessMode.PAID, and transforms the filtered list into
   PopularCourseDTO objects.

   Input parameters: long creatorId

   Expected return value: List<PopularCourseDTO>

   Potential edge cases:

        Creator not found: The resourceValidator.validateCreatorExists should throw an exception.

        Creator exists but has no courses: The method should return an empty list.

        Creator exists but has no PAID courses: All courses are filtered out, and the method should return an empty list.

   External dependencies (to mock):

        ValidationResources: Specifically, the validateCreatorExists(long creatorId) method.


Progress 2

Prompt 2:

* Input:

Generate comprehensive unit test cases for the "retrieveCourseByemail(long creatorId)" method below. Use the
Given-When-Then structure:

/*
public List<CourseCardDto> retrieveCourseByemail(long creatorId)  {
Optional<Creator> c = this.creatorRepository.findById(creatorId);
List<Course> courses = c.get().getCourses();
return courses.stream().map(CourseTransForm::transformToCourseCardDto).collect(Collectors.toList());
}
*/

Include:

- Happy path scenario (Creator exists and has courses)
- Edge case (Creator exists but has no courses)
- Error scenario (Creator ID not found, which will cause a NoSuchElementException on .get())


* AI output:

### 2.2. AI Output (Test Cases Matrix)

## Ma trận 1: `CourseServiceTest.java` (Phương thức: `retrieveCourseByemail`)

Ma trận này dựa trên file `CourseServiceTest.java`, tập trung vào việc lấy các khóa học của một creator.

| Category | Test Case (Tên phương thức test) | Input (Given) | Expected (Then) |
| :--- | :--- | :--- | :--- |
| **Error** | `retrieveCourseByemail_WhenCreatorNotFound...` | `creatorId` không tồn tại (ví dụ: `999L`).<br> `creatorRepository.findById(...)` trả về `Optional.empty()`. | Ném ra (throws) `NoSuchElementException`. |
| **Edge Case** | `retrieveCourseByemail_WhenCreatorExistsWithNoCourses...` | `creatorId` tồn tại.<br> `creatorRepository.findById(...)` trả về `Creator` có danh sách `courses` rỗng. | Trả về một `List<CourseCardDto>` rỗng (empty list). |
| **Happy Path** | `retrieveCourseByemail_WhenCreatorExistsWithOneCourse...` | `creatorId` tồn tại.<br> `creatorRepository.findById(...)` trả về `Creator` có 1 `Course`.<br> `CourseTransForm` được mock. | Trả về `List<CourseCardDto>` có 1 phần tử. |
| **Happy Path** | `retrieveCourseByemail_WhenCreatorExistsWithMultipleCourses...` | `creatorId` tồn tại.<br> `creatorRepository.findById(...)` trả về `Creator` có nhiều `Course`.<br> `CourseTransForm` được mock. | Trả về `List<CourseCardDto>` có nhiều phần tử (ví dụ: `courses.size()`). |

---

## Ma trận 2: `CourseServiceGetCourseByIdTest.java` (Phương thức: `getCourseById`)

Ma trận này dựa trên file `CourseServiceGetCourseByIdTest.java`, tập trung vào việc lấy chi tiết một khóa học cụ thể.

| Category | Test Case (Tên phương thức test) | Input (Given) | Expected (Then) |
| :--- | :--- | :--- | :--- |
| **Error** | `getCourseById_WhenCourseNotFound...` | `courseId` không tồn tại.<br> `validationResources.validateCourseOwnership(...)` ném ra `CourseNotFoundException`. | Ném ra `CourseNotFoundException`. |
| **Error** | `getCourseById_WhenCreatorDoesNotOwnCourse...` | `creatorId` không sở hữu `courseId`.<br> `validationResources.validateCourseOwnership(...)` ném ra `UnauthorizedException`. | Ném ra `UnauthorizedException`. |
| **Edge Case** | `getCourseById_WhenCourseExistsWithNoChapters...` | `courseId` tồn tại và `creatorId` sở hữu.<br> `validateCourseOwnership(...)` trả về `Course` có danh sách `chapters` rỗng. | Trả về `CourseContentDto` với danh sách `chapters` rỗng. |
| **Happy Path** | `getCourseById_WhenCourseExistsWithChaptersAndModules...` | `courseId` tồn tại và `creatorId` sở hữu.<br> `validateCourseOwnership(...)` trả về `Course` có đầy đủ `chapters` và `modules`. | Trả về `CourseContentDto` đầy đủ, có 2 `chapters` và 2 `modules` ở chapter đầu tiên. |

---

## Ma trận 3: `CourseServiceRetrieveCCourseTest.java` (Phương thức: `retrieveCCourse`)

Ma trận này dựa trên file `CourseServiceRetrieveCCourseTest.java`, tập trung vào việc lọc và lấy các khóa học *trả phí* (PAID) của creator.

| Category | Test Case (Tên phương thức test) | Input (Given) | Expected (Then) |
| :--- | :--- | :--- | :--- |
| **Error** | `retrieveCCourse_WhenCreatorNotFound...` | `creatorId` không tồn tại (ví dụ: `999L`).<br> `validationResources.validateCreatorExists(...)` ném ra `CreatorNotFoundException`. | Ném ra `CreatorNotFoundException`. |
| **Edge Case** | `retrieveCCourse_WhenCreatorHasNoCourses...` | `creatorId` tồn tại.<br> `validateCreatorExists(...)` trả về `Creator` có danh sách `courses` rỗng. | Trả về một `List<PopularCourseDTO>` rỗng. |
| **Edge Case** | `retrieveCCourse_WhenCreatorHasOnlyNonPaidCourses...` | `creatorId` tồn tại.<br> `validateCreatorExists(...)` trả về `Creator` chỉ có khóa học `PUBLIC` hoặc `PRIVATE` (không có `PAID`). | Trả về một `List<PopularCourseDTO>` rỗng (do bị lọc hết). |
| **Happy Path** | `retrieveCCourse_WhenCreatorHasOnePaidCourse...` | `creatorId` tồn tại.<br> `validateCreatorExists(...)` trả về `Creator` có 1 khóa học `PAID`.<br> `CreatorTransform` được mock. | Trả về `List<PopularCourseDTO>` có 1 phần tử. |
| **Happy Path** | `retrieveCCourse_WhenCreatorHasMultipleCourses...` | `creatorId` tồn tại.<br> `validateCreatorExists(...)` trả về `Creator` có 2 `PAID`, 1 `PUBLIC`, 1 `PRIVATE`.<br> `CreatorTransform` được mock. | Trả về `List<PopularCourseDTO>` chỉ chứa 2 khóa học `PAID`. |

Progress 3
## Prompt 3


### 1. Input Prompt:
```text
Create JUnit 5 test classes for the "CourseService" based on the analysis from Phase 1.

I need separate test files for each of these 3 methods:
1.  retrieveCourseByemail(long creatorId)
2.  getCourseById(long courseId, long creatorId)
3.  retrieveCCourse(long creatorId)

Requirements:
- Use JUnit 5 and Mockito.
- Use @ExtendWith(MockitoExtension.class).
- Use @Mock for all dependencies (@Autowired fields) and @InjectMocks for the "CourseService".
- Use @BeforeEach for setup if needed.
- Use AssertJ or standard JUnit 5 assertions (assertEquals, assertThrows, assertTrue).
- Mock the static transform methods (CourseTransForm, CreatorTransform) using "mockStatic".
- Cover all scenarios from our analysis: happy paths (1 item, multiple items), edge cases (empty list), and error cases (ID not found).
- Add descriptive test names using @DisplayName or clear method names.
````
### 2. AI output:

// =============== FILE 1: CourseServiceTest.java ===============

package com.jpd.web;

import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.service.CourseService;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.CourseTransForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CreatorRepository creatorRepository;

    @Mock
    private ValidationResources validationResources;

    @InjectMocks
    private CourseService courseService;


    /**
     * Test verifies that NoSuchElementException is thrown when attempting to retrieve courses
     * for a non-existent creator ID. This validates proper error handling for invalid creator lookups.
     */
    @Test
    void retrieveCourseByemail_WhenCreatorNotFound_ShouldThrowNoSuchElementException() {
        // Arrange
        long nonExistentCreatorId = 999L;
        when(creatorRepository.findById(nonExistentCreatorId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            courseService.retrieveCourseByemail(nonExistentCreatorId);
        }, "Should throw NoSuchElementException when creator is not found");

        verify(creatorRepository, times(1)).findById(nonExistentCreatorId);
    }

    /**
     * Parameterized test that verifies an empty list is returned when a creator exists but has no courses.
     * Test data is loaded from CSV file to test multiple creator IDs.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/test-data/creator-no-courses.csv", numLinesToSkip = 1)
    void retrieveCourseByemail_WhenCreatorExistsWithNoCourses_ShouldReturnEmptyList(long creatorId) {
        // Arrange
        Creator creator = Creator.builder()
                .courses(new ArrayList<>())
                .build();
        when(creatorRepository.findById(creatorId)).thenReturn(Optional.of(creator));

        // Act
        List<CourseCardDto> result = courseService.retrieveCourseByemail(creatorId);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty when creator has no courses");
        assertEquals(0, result.size(), "Result size should be 0");
        verify(creatorRepository, times(1)).findById(creatorId);
    }

    /**
     * Parameterized test that verifies a single course is correctly retrieved and transformed
     * when a creator has exactly one course. Validates course ID and name match expected values.
     * Uses CSV data to test multiple scenarios.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/test-data/creator-one-course.csv", numLinesToSkip = 1)
    void retrieveCourseByemail_WhenCreatorExistsWithOneCourse_ShouldReturnSingleCourse(
            long creatorId, long courseId, String courseName) {
        // Arrange
        Course course = new Course();
        course.setCourseId(courseId);
        course.setName(courseName);

        Creator creator = Creator.builder()
                .courses(List.of(course))
                .build();

        CourseCardDto expectedDto = new CourseCardDto();
        expectedDto.setId(courseId);
        expectedDto.setName(courseName);

        when(creatorRepository.findById(creatorId)).thenReturn(Optional.of(creator));

        try (MockedStatic<CourseTransForm> mockedTransform = mockStatic(CourseTransForm.class)) {
            mockedTransform.when(() -> CourseTransForm.transformToCourseCardDto(course))
                    .thenReturn(expectedDto);

            // Act
            List<CourseCardDto> result = courseService.retrieveCourseByemail(creatorId);

            // Assert
            assertNotNull(result, "Result should not be null");
            assertEquals(1, result.size(), "Result should contain exactly one course");
            assertEquals(courseId, result.get(0).getId(), "Course ID should match");
            assertEquals(courseName, result.get(0).getName(), "Course name should match");

            verify(creatorRepository, times(1)).findById(creatorId);
            mockedTransform.verify(() -> CourseTransForm.transformToCourseCardDto(course), times(1));
        }
    }

    /**
     * Parameterized test that verifies all courses are correctly retrieved and transformed
     * when a creator has multiple courses. Validates that the count, IDs, and names of all
     * courses match expected values. CSV data contains semicolon-separated course lists.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/test-data/creator-multiple-courses.csv", numLinesToSkip = 1)
    void retrieveCourseByemail_WhenCreatorExistsWithMultipleCourses_ShouldReturnAllCourses(
            long creatorId, String courseIds, String courseNames) {
        // Arrange
        String[] ids = courseIds.split(";");
        String[] names = courseNames.split(";");

        List<Course> courses = new ArrayList<>();
        List<CourseCardDto> dtos = new ArrayList<>();

        for (int i = 0; i < ids.length; i++) {
            Course course = new Course();
            course.setCourseId(Long.parseLong(ids[i]));
            course.setName(names[i]);
            courses.add(course);

            CourseCardDto dto = new CourseCardDto();
            dto.setId(Long.parseLong(ids[i]));
            dto.setName(names[i]);
            dtos.add(dto);
        }

        Creator creator = Creator.builder()
                .courses(courses)
                .build();

        when(creatorRepository.findById(creatorId)).thenReturn(Optional.of(creator));

        try (MockedStatic<CourseTransForm> mockedTransform = mockStatic(CourseTransForm.class)) {
            for (int i = 0; i < courses.size(); i++) {
                Course course = courses.get(i);
                CourseCardDto dto = dtos.get(i);
                mockedTransform.when(() -> CourseTransForm.transformToCourseCardDto(course))
                        .thenReturn(dto);
            }

            // Act
            List<CourseCardDto> result = courseService.retrieveCourseByemail(creatorId);

            // Assert
            assertNotNull(result, "Result should not be null");
            assertEquals(courses.size(), result.size(), "Result should contain all courses");

            for (int i = 0; i < result.size(); i++) {
                assertEquals(dtos.get(i).getId(), result.get(i).getId(),
                        "Course ID at index " + i + " should match");
                assertEquals(dtos.get(i).getName(), result.get(i).getName(),
                        "Course name at index " + i + " should match");
            }

            verify(creatorRepository, times(1)).findById(creatorId);
            mockedTransform.verify(() -> CourseTransForm.transformToCourseCardDto(any(Course.class)),
                    times(courses.size()));
        }
    }
}

// =============== FILE 2: CourseServiceGetCourseByIdTest.java ===============
package com.jpd.web;

import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Language;
import com.jpd.web.model.Module;
import com.jpd.web.service.CourseService;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.CourseTransForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
* Unit tests for the getCourseById method in CourseService.
* This test suite focuses on testing course retrieval functionality with various scenarios
* including error cases and successful retrievals with different course structures.
  */
  @ExtendWith(MockitoExtension.class)
  class CourseServiceGetCourseByIdTest {

  @Mock
  private ValidationResources validationResources;

  @InjectMocks
  private CourseService courseService;

  /**
    * Test verifies that CourseNotFoundException is thrown when attempting to retrieve
    * a course that doesn't exist. This validates proper error handling for invalid course IDs.
      */
      @Test
      void getCourseById_WhenCourseNotFound_ShouldThrowCourseNotFoundException() {
      // Arrange
      long courseId = 999L;
      long creatorId = 1L;

      when(validationResources.validateCourseOwnership(courseId, creatorId))
      .thenThrow(new CourseNotFoundException(courseId));

      // Act & Assert
      assertThrows(CourseNotFoundException.class, () -> {
      courseService.getCourseById(courseId, creatorId);
      }, "Should throw CourseNotFoundException when course doesn't exist");

      verify(validationResources, times(1)).validateCourseOwnership(courseId, creatorId);
      }

  /**
    * Test verifies that UnauthorizedException is thrown when a creator attempts to access
    * a course they don't own. This validates proper authorization checks.
      */
      @Test
      void getCourseById_WhenCreatorDoesNotOwnCourse_ShouldThrowUnauthorizedException() {
      // Arrange
      long courseId = 1L;
      long creatorId = 1L;

      when(validationResources.validateCourseOwnership(courseId, creatorId))
      .thenThrow(new UnauthorizedException("You don't have permission to modify this course"));

      // Act & Assert
      assertThrows(UnauthorizedException.class, () -> {
      courseService.getCourseById(courseId, creatorId);
      }, "Should throw UnauthorizedException when creator doesn't own the course");

      verify(validationResources, times(1)).validateCourseOwnership(courseId, creatorId);
      }

  /**
    * Test verifies successful retrieval of course content when the course exists,
    * creator has ownership, but the course has no chapters. Validates basic course
    * information is correctly transformed.
      */
      @Test
      void getCourseById_WhenCourseExistsWithNoChapters_ShouldReturnCourseContent() {
      // Arrange
      long courseId = 1L;
      long creatorId = 1L;

      Course course = Course.builder()
      .courseId(courseId)
      .name("Test Course")
      .language(Language.ENGLISH)
      .teachingLanguage(Language.ENGLISH)
      .isPublic(true)
      .chapters(new ArrayList<>())
      .build();

      CourseContentDto expectedDto = new CourseContentDto();
      expectedDto.setName("Test Course");
      expectedDto.setPublic(true);
      expectedDto.setLanguage(Language.ENGLISH);
      expectedDto.setTeachingLanguage(Language.ENGLISH);
      expectedDto.setChapters(new ArrayList<>());

      when(validationResources.validateCourseOwnership(courseId, creatorId)).thenReturn(course);

      try (MockedStatic<CourseTransForm> mockedTransform = mockStatic(CourseTransForm.class)) {
      mockedTransform.when(() -> CourseTransForm.transformToCourseContentDto(course))
      .thenReturn(expectedDto);

           // Act
           CourseContentDto result = courseService.getCourseById(courseId, creatorId);

           // Assert
           assertNotNull(result, "Result should not be null");
           assertEquals("Test Course", result.getName(), "Course name should match");
           assertEquals(Language.ENGLISH, result.getLanguage(), "Language should match");
           assertEquals(Language.ENGLISH, result.getTeachingLanguage(), "Teaching language should match");
           assertTrue(result.isPublic(), "Course should be public");
           assertNotNull(result.getChapters(), "Chapters list should not be null");
           assertTrue(result.getChapters().isEmpty(), "Chapters list should be empty");

           verify(validationResources, times(1)).validateCourseOwnership(courseId, creatorId);
           mockedTransform.verify(() -> CourseTransForm.transformToCourseContentDto(course), times(1));
      }
      }

  /**
    * Test verifies successful retrieval of complete course content including chapters and modules.
    * This validates that the lazy-loaded relationships are properly accessed and the full
    * course structure is correctly transformed to DTO.
      */
      @Test
      void getCourseById_WhenCourseExistsWithChaptersAndModules_ShouldReturnCompleteCourseContent() {
      // Arrange
      long courseId = 1L;
      long creatorId = 1L;

      // Create modules
      Module module1 = Module.builder()
      .moduleId(1L)
      .titleOfModule("Module 1")
      .build();

      Module module2 = Module.builder()
      .moduleId(2L)
      .titleOfModule("Module 2")
      .build();

      // Create chapters with modules
      Chapter chapter1 = Chapter.builder()
      .chapterId(1L)
      .ChapterName("Chapter 1")
      .orderInCourse(1)
      .modules(List.of(module1, module2))
      .build();

      Chapter chapter2 = Chapter.builder()
      .chapterId(2L)
      .ChapterName("Chapter 2")
      .orderInCourse(2)
      .modules(new ArrayList<>())
      .build();

      // Create course with chapters
      Course course = Course.builder()
      .courseId(courseId)
      .name("Complete Course")
      .language(Language.ENGLISH)
      .teachingLanguage(Language.VIETNAMESE)
      .isPublic(true)
      .chapters(List.of(chapter1, chapter2))
      .build();

      CourseContentDto expectedDto = new CourseContentDto();
      expectedDto.setName("Complete Course");
      expectedDto.setPublic(true);
      expectedDto.setLanguage(Language.ENGLISH);
      expectedDto.setTeachingLanguage(Language.VIETNAMESE);
      expectedDto.setChapters(List.of(chapter1, chapter2));

      when(validationResources.validateCourseOwnership(courseId, creatorId)).thenReturn(course);

      try (MockedStatic<CourseTransForm> mockedTransform = mockStatic(CourseTransForm.class)) {
      mockedTransform.when(() -> CourseTransForm.transformToCourseContentDto(course))
      .thenReturn(expectedDto);

           // Act
           CourseContentDto result = courseService.getCourseById(courseId, creatorId);

           // Assert
           assertNotNull(result, "Result should not be null");
           assertEquals("Complete Course", result.getName(), "Course name should match");
           assertEquals(Language.ENGLISH, result.getLanguage(), "Language should match");
           assertEquals(Language.VIETNAMESE, result.getTeachingLanguage(), "Teaching language should match");
           assertTrue(result.isPublic(), "Course should be public");

           assertNotNull(result.getChapters(), "Chapters list should not be null");
           assertEquals(2, result.getChapters().size(), "Should have 2 chapters");

           // Verify first chapter
           assertEquals("Chapter 1", result.getChapters().get(0).getChapterName(), "First chapter name should match");
           assertEquals(2, result.getChapters().get(0).getModules().size(), "First chapter should have 2 modules");

           // Verify second chapter
           assertEquals("Chapter 2", result.getChapters().get(1).getChapterName(), "Second chapter name should match");
           assertTrue(result.getChapters().get(1).getModules().isEmpty(), "Second chapter should have no modules");

           verify(validationResources, times(1)).validateCourseOwnership(courseId, creatorId);
           mockedTransform.verify(() -> CourseTransForm.transformToCourseContentDto(course), times(1));
      }
      }
      }
// =============== FILE 3: CourseServiceRetrieveCCourseTest.java ===============
      package com.jpd.web;

import com.jpd.web.dto.PopularCourseDTO;
import com.jpd.web.exception.CreatorNotFoundException;
import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.service.CourseService;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.CreatorTransform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
* Unit tests for the retrieveCCourse method in CourseService.
* This test suite focuses on testing the retrieval of paid courses for a creator
* and their transformation to PopularCourseDTO objects.
  */
  @ExtendWith(MockitoExtension.class)
  class CourseServiceRetrieveCCourseTest {

  @Mock
  private ValidationResources validationResources;

  @InjectMocks
  private CourseService courseService;

  /**
    * Test verifies that CreatorNotFoundException is thrown when attempting to retrieve
    * courses for a non-existent creator. This validates proper error handling.
      */
      @Test
      void retrieveCCourse_WhenCreatorNotFound_ShouldThrowCreatorNotFoundException() {
      // Arrange
      long creatorId = 999L;

      when(validationResources.validateCreatorExists(creatorId))
      .thenThrow(new CreatorNotFoundException(creatorId));

      // Act & Assert
      assertThrows(CreatorNotFoundException.class, () -> {
      courseService.retrieveCCourse(creatorId);
      }, "Should throw CreatorNotFoundException when creator doesn't exist");

      verify(validationResources, times(1)).validateCreatorExists(creatorId);
      }

  /**
    * Test verifies that an empty list is returned when a creator exists but has no courses.
    * This validates handling of creators with empty course lists.
      */
      @Test
      void retrieveCCourse_WhenCreatorHasNoCourses_ShouldReturnEmptyList() {
      // Arrange
      long creatorId = 1L;

      Creator creator = Creator.builder()
      .courses(new ArrayList<>())
      .build();

      when(validationResources.validateCreatorExists(creatorId)).thenReturn(creator);

      // Act
      List<PopularCourseDTO> result = courseService.retrieveCCourse(creatorId);

      // Assert
      assertNotNull(result, "Result should not be null");
      assertTrue(result.isEmpty(), "Result should be empty when creator has no courses");
      assertEquals(0, result.size(), "Result size should be 0");

      verify(validationResources, times(1)).validateCreatorExists(creatorId);
      }

  /**
    * Test verifies that an empty list is returned when a creator has only PUBLIC or PRIVATE courses
    * (no PAID courses). This validates the filtering logic for PAID courses only.
      */
      @Test
      void retrieveCCourse_WhenCreatorHasOnlyNonPaidCourses_ShouldReturnEmptyList() {
      // Arrange
      long creatorId = 1L;

      Course publicCourse = Course.builder()
      .courseId(1L)
      .name("Public Course")
      .accessMode(AccessMode.PUBLIC)
      .build();

      Course privateCourse = Course.builder()
      .courseId(2L)
      .name("Private Course")
      .accessMode(AccessMode.PRIVATE)
      .build();

      Creator creator = Creator.builder()
      .courses(List.of(publicCourse, privateCourse))
      .build();

      when(validationResources.validateCreatorExists(creatorId)).thenReturn(creator);

      // Act
      List<PopularCourseDTO> result = courseService.retrieveCCourse(creatorId);

      // Assert
      assertNotNull(result, "Result should not be null");
      assertTrue(result.isEmpty(), "Result should be empty when creator has no PAID courses");
      assertEquals(0, result.size(), "Result size should be 0");

      verify(validationResources, times(1)).validateCreatorExists(creatorId);
      }

  /**
    * Test verifies successful retrieval and transformation of a single PAID course.
    * This validates the basic happy path for one paid course.
      */
      @Test
      void retrieveCCourse_WhenCreatorHasOnePaidCourse_ShouldReturnSingleDTO() {
      // Arrange
      long creatorId = 1L;

      Course paidCourse = Course.builder()
      .courseId(100L)
      .name("Paid Course")
      .accessMode(AccessMode.PAID)
      .price(29.99)
      .build();

      Creator creator = Creator.builder()
      .courses(List.of(paidCourse))
      .build();

      PopularCourseDTO expectedDto = PopularCourseDTO.builder()
      .courseId(100L)
      .title("Paid Course")
      .price(29.99)
      .students(0)
      .revenue(0.0)
      .rating(0.0)
      .build();

      when(validationResources.validateCreatorExists(creatorId)).thenReturn(creator);

      try (MockedStatic<CreatorTransform> mockedTransform = mockStatic(CreatorTransform.class)) {
      mockedTransform.when(() -> CreatorTransform.transform(paidCourse))
      .thenReturn(expectedDto);

           // Act
           List<PopularCourseDTO> result = courseService.retrieveCCourse(creatorId);

           // Assert
           assertNotNull(result, "Result should not be null");
           assertEquals(1, result.size(), "Result should contain exactly one course");
           assertEquals(100L, result.get(0).getCourseId(), "Course ID should match");
           assertEquals("Paid Course", result.get(0).getTitle(), "Course title should match");
           assertEquals(29.99, result.get(0).getPrice(), "Course price should match");

           verify(validationResources, times(1)).validateCreatorExists(creatorId);
           mockedTransform.verify(() -> CreatorTransform.transform(paidCourse), times(1));
      }
      }

  /**
    * Test verifies successful retrieval of multiple PAID courses while filtering out
    * PUBLIC and PRIVATE courses. This validates the complete filtering and transformation logic.
      */
      @Test
      void retrieveCCourse_WhenCreatorHasMultipleCourses_ShouldReturnOnlyPaidCourses() {
      // Arrange
      long creatorId = 1L;

      Course paidCourse1 = Course.builder()
      .courseId(101L)
      .name("Paid Course 1")
      .accessMode(AccessMode.PAID)
      .price(49.99)
      .build();

      Course publicCourse = Course.builder()
      .courseId(102L)
      .name("Public Course")
      .accessMode(AccessMode.PUBLIC)
      .build();

      Course paidCourse2 = Course.builder()
      .courseId(103L)
      .name("Paid Course 2")
      .accessMode(AccessMode.PAID)
      .price(39.99)
      .build();

      Course privateCourse = Course.builder()
      .courseId(104L)
      .name("Private Course")
      .accessMode(AccessMode.PRIVATE)
      .build();

      Creator creator = Creator.builder()
      .courses(List.of(paidCourse1, publicCourse, paidCourse2, privateCourse))
      .build();

      PopularCourseDTO dto1 = PopularCourseDTO.builder()
      .courseId(101L)
      .title("Paid Course 1")
      .price(49.99)
      .students(150)
      .revenue(7498.50)
      .rating(4.5)
      .build();

      PopularCourseDTO dto2 = PopularCourseDTO.builder()
      .courseId(103L)
      .title("Paid Course 2")
      .price(39.99)
      .students(200)
      .revenue(7998.00)
      .rating(4.8)
      .build();

      when(validationResources.validateCreatorExists(creatorId)).thenReturn(creator);

      try (MockedStatic<CreatorTransform> mockedTransform = mockStatic(CreatorTransform.class)) {
      mockedTransform.when(() -> CreatorTransform.transform(paidCourse1))
      .thenReturn(dto1);
      mockedTransform.when(() -> CreatorTransform.transform(paidCourse2))
      .thenReturn(dto2);

           // Act
           List<PopularCourseDTO> result = courseService.retrieveCCourse(creatorId);

           // Assert
           assertNotNull(result, "Result should not be null");
           assertEquals(2, result.size(), "Result should contain exactly two PAID courses");

           // Verify first paid course
           assertEquals(101L, result.get(0).getCourseId(), "First course ID should match");
           assertEquals("Paid Course 1", result.get(0).getTitle(), "First course title should match");
           assertEquals(49.99, result.get(0).getPrice(), "First course price should match");
           assertEquals(150, result.get(0).getStudents(), "First course students should match");
           assertEquals(7498.50, result.get(0).getRevenue(), "First course revenue should match");
           assertEquals(4.5, result.get(0).getRating(), "First course rating should match");

           // Verify second paid course
           assertEquals(103L, result.get(1).getCourseId(), "Second course ID should match");
           assertEquals("Paid Course 2", result.get(1).getTitle(), "Second course title should match");
           assertEquals(39.99, result.get(1).getPrice(), "Second course price should match");
           assertEquals(200, result.get(1).getStudents(), "Second course students should match");
           assertEquals(7998.00, result.get(1).getRevenue(), "Second course revenue should match");
           assertEquals(4.8, result.get(1).getRating(), "Second course rating should match");

           verify(validationResources, times(1)).validateCreatorExists(creatorId);
           mockedTransform.verify(() -> CreatorTransform.transform(paidCourse1), times(1));
           mockedTransform.verify(() -> CreatorTransform.transform(paidCourse2), times(1));
           // Verify PUBLIC and PRIVATE courses are not transformed
           mockedTransform.verify(() -> CreatorTransform.transform(publicCourse), never());
           mockedTransform.verify(() -> CreatorTransform.transform(privateCourse), never());
      }
      }

  /**
    * Test verifies that only PAID courses are returned when a creator has all three types
    * of courses (PUBLIC, PAID, PRIVATE). This ensures the filtering logic works correctly
    * with mixed course types.
      */
      @Test
      void retrieveCCourse_WhenCreatorHasAllCourseTypes_ShouldFilterAndReturnOnlyPaid() {
      // Arrange
      long creatorId = 1L;

      Course paidCourse = Course.builder()
      .courseId(201L)
      .name("Premium Java Course")
      .accessMode(AccessMode.PAID)
      .price(99.99)
      .build();

      Course publicCourse1 = Course.builder()
      .courseId(202L)
      .name("Public Intro Course")
      .accessMode(AccessMode.PUBLIC)
      .build();

      Course publicCourse2 = Course.builder()
      .courseId(203L)
      .name("Another Public Course")
      .accessMode(AccessMode.PUBLIC)
      .build();

      Course privateCourse = Course.builder()
      .courseId(204L)
      .name("Company Training")
      .accessMode(AccessMode.PRIVATE)
      .build();

      Creator creator = Creator.builder()
      .courses(List.of(publicCourse1, paidCourse, publicCourse2, privateCourse))
      .build();

      PopularCourseDTO expectedDto = PopularCourseDTO.builder()
      .courseId(201L)
      .title("Premium Java Course")
      .price(99.99)
      .students(500)
      .revenue(49995.00)
      .rating(4.9)
      .urlImg("[https://example.com/image.jpg](https://example.com/image.jpg)")
      .build();

      when(validationResources.validateCreatorExists(creatorId)).thenReturn(creator);

      try (MockedStatic<CreatorTransform> mockedTransform = mockStatic(CreatorTransform.class)) {
      mockedTransform.when(() -> CreatorTransform.transform(paidCourse))
      .thenReturn(expectedDto);

           // Act
           List<PopularCourseDTO> result = courseService.retrieveCCourse(creatorId);

           // Assert
           assertNotNull(result, "Result should not be null");
           assertEquals(1, result.size(), "Result should contain only one PAID course");
           assertEquals(201L, result.get(0).getCourseId(), "Course ID should match");
           assertEquals("Premium Java Course", result.get(0).getTitle(), "Course title should match");
           assertEquals(99.99, result.get(0).getPrice(), "Course price should match");
           assertEquals(500, result.get(0).getStudents(), "Students count should match");
           assertEquals(49995.00, result.get(0).getRevenue(), "Revenue should match");
           assertEquals(4.9, result.get(0).getRating(), "Rating should match");
           assertEquals("[https://example.com/image.jpg](https://example.com/image.jpg)", result.get(0).getUrlImg(), "Image URL should match");

           verify(validationResources, times(1)).validateCreatorExists(creatorId);
           mockedTransform.verify(() -> CreatorTransform.transform(paidCourse), times(1));
           // Verify non-PAID courses are not transformed
           mockedTransform.verify(() -> CreatorTransform.transform(publicCourse1), never());
           mockedTransform.verify(() -> CreatorTransform.transform(publicCourse2), never());
           mockedTransform.verify(() -> CreatorTransform.transform(privateCourse), never());
      }
      }
      }

Progress 4

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  ... s
[INFO] Finished at: ...
[INFO] ------------------------------------------------------------------------

Progress 5


Progress 6
