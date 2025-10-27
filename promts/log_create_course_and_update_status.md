# Prompt Log


## 2025-10-25 — ID: 2025-10-25-001

## Title:  for create course and update status
**Prompt:**

**Phân tích**

Analyze this CourseController class and identify createCourse,putMethodName functions that need unit testingPlease analyze which functions should be covered by unit testing.If you need any additional information or resources, just let me know, and I will provide them to you. Just focus on the createCourse,putMethodName method, and the methods that support it, don't go rambling on about methods that I don't require. If there is any missing method please ask me to send more so you have enough information:public ResponseEntity<CourseCardDto> createCourse(@Valid @ModelAttribute CourseFormDto entity, HttpServletRequest request) { // TODO: process POST request Course c; long creatorId= RequestAttributeExtractor.extractCreatorId(request); c = this.courseService.createCourse(entity, creatorId); return ResponseEntity.status(HttpStatus.CREATED).body(CourseTransForm.transformToCourseCardDto(c)); }public ResponseEntity<?> putMethodName(@PathVariable long id,HttpServletRequest request) { //TODO: process PUT request long creatorId= RequestAttributeExtractor.extractCreatorId(request); this.courseService.changeCourseSatus(id, creatorId); return ResponseEntity.status(HttpStatus.OK).build(); }For each function, identify: 1. Main functionality 2. Input parameters and types 3. Expected return values 4. Potential edge cases 5. Dependencies that need mocking

**Response:**
createCourse

Main functionality
Create a course on behalf of the authenticated creator and return a 201 Created with a CourseCardDto payload.

Input parameters & types

@ModelAttribute @Valid CourseFormDto entity — form data validated by Jakarta Bean Validation (e.g., @NotNull, @NotBlank, @PositiveOrZero, etc.).

HttpServletRequest request — used to read "creatorId" via RequestAttributeExtractor.extractCreatorId(request).

Expected return values

ResponseEntity<CourseCardDto> with:

Status: 201 CREATED

Body: CourseTransForm.transformToCourseCardDto(<Course from service>).

Potential edge cases to cover

Missing creatorId request attribute → current RequestAttributeExtractor implementation will actually trigger a NullPointerException before it throws the intended CreatorIdNotFoundInRequestException (it dereferences creatorIdObj when it’s null). This is an important defect to surface in tests.

Non-Long creatorId (e.g., a String) → ClassCastException when casting to Long.

Validation failures on CourseFormDto (e.g., missing required fields, negative price with PAID access): with Spring MVC, @Valid + @ModelAttribute + no BindingResult means a BindException is thrown before the controller body runs (test this with MockMvc).

Service failure (e.g., duplicate name, domain rule violation) → exception from courseService.createCourse(...) should propagate to the global exception handler (assert behavior/translation if you have one).

Unexpected null from service (service returns null Course) → CourseTransForm.transformToCourseCardDto(null) likely throws; surface this or enforce a non-null contract.

Dependencies that need mocking (or controlled)

courseService — mock: when(courseService.createCourse(entity, creatorId)).thenReturn(course)

CourseTransForm.transformToCourseCardDto — static method; either:

Mock statically (Mockito mockStatic) and return a known CourseCardDto, or

Treat as a pure function and assert on its output only if you control the Course test fixture closely.

HttpServletRequest — provide a MockHttpServletRequest or Mockito mock: set/get "creatorId" attribute.

RequestAttributeExtractor.extractCreatorId — you can avoid mocking it by controlling the request; test its error paths separately (see “Supporting methods” below).

Validation — use MockMvc slice tests to assert 400 for invalid CourseFormDto since direct unit calls won’t trigger Bean Validation.

putMethodName

Main functionality
Change the status of a course identified by path variable id for the authenticated creator, then return 200 OK without a body.

Input parameters & types

@PathVariable long id

HttpServletRequest request → creatorId pulled via RequestAttributeExtractor.extractCreatorId(request).

Expected return values

ResponseEntity<?> with:

Status: 200 OK

Body: empty

Potential edge cases to cover

Missing or non-Long creatorId → same NPE / ClassCastException behaviors from RequestAttributeExtractor.

Invalid id (negative, zero) — not validated here; verify how courseService.changeCourseSatus(id, creatorId) reacts (e.g., NotFound, IllegalArgument).

Unauthorized creator (creator does not own the course) — ensure service throws the right exception and that it’s mapped correctly by your exception handler.

Business rule conflicts (e.g., status already the same) — assert service exception mapping.

Service throws (generic runtime) — ensure it propagates / is handled as expected.

Dependencies that need mocking (or controlled)

courseService — mock: verify changeCourseSatus(id, creatorId) is called with the correct values; no return value to stub.

HttpServletRequest — same as above.

RequestAttributeExtractor — rely on actual static method with controlled request; test extractor’s failure paths separately.


**Prompt 2: Generate Test Cases**
Create comprehensive unit test cases for  functions in CourseService class you just analyzed
aboveInclude: - Happy path scenarios
- Edge cases (boundary values)
- Error scenarios
- Integration with cart state
  Example :
  Category Test Case Input Expected Happy Path Add new item product={id:1, price:100}, qty=2 items.length = 1 Happy Path Add existing item Same product, qty=3 quantity updated to 5 Edge Case Quantity = 0 product={...}, qty=0 throws Error Edge Case Negative quantity product={...}, qty=-5 throws Error Error Null product product=null, qty=1 throws Error Error Undefined product product=undefined, qty=1 throws Error
  Using Given-When-Then pattern to structure test cases

**Response:**
createCourse(entity, creatorId)

| Category         | Test Case                                            | Input                                                                                                                                                                                                                                                                                                           | Expected                                                                                                                            |
| ---------------- | ---------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| Happy Path       | FREE/PUBLIC course created                           | **Given** valid row: `accessMode=PUBLIC, price=0`, `imgFile` attached; validator returns `Creator(Status.SUCCESS)`; `uploadFile(..)` → URL; `generate6DigitCode()` → `"ABC123"`. **When** `createCourse(form, creatorId)`. **Then** returns `Course` with `name`, `urlImg=URL`, `joinKey="ABC123"`, saved once. | `result.name == courseName` ; `result.urlImg == expectedImgUrl` ; `result.joinKey == expectedJoinKey` ; verify `save(..)` called 1× |
| Happy Path       | PAID course created                                  | **Given** valid row: `accessMode=PAID`, `price>0`, file attached; validator returns verified creator; upload→URL; code→key. **When** create. **Then** `accessMode==PAID`, `urlImg` set, saved once.                                                                                                             | `result.accessMode == PAID`; `result.urlImg == expectedImgUrl`; verify `save(..)` 1×                                                |
| Happy Path (CSV) | Generic success (multiple rows)                      | **Given** a valid row; `uploadFile(..)` → URL or non-empty; `generate6DigitCode()` → `"JOIN-XYZ"`. **When** create. **Then** saved entity mirrors CSV fields (`name`, `accessMode`, `price`, `language`, `teachingLanguage`) and `joinKey`.                                                                     | All fields equal CSV; `joinKey == "JOIN-XYZ"`; verify `save(..)` 1×                                                                 |
| Edge Case        | PAID with `price=0` (boundary)                       | **Given** row: `PAID, price=0`, validator ok, upload ok. **When** create. **Then** (per your service rule) either **succeeds** and saves, or **throws**; assert your chosen rule.                                                                                                                               | If allowed: saved 1×; else: throws `IllegalArgumentException`                                                                       |
| Edge Case        | Upload returns `null`                                | **Given** `expectedImgUrl = "null"` in CSV ⇒ stub `uploadFile(..)` returns `null`. **When** create. **Then** `urlImg == null`, still saved.                                                                                                                                                                     | `result.urlImg == null`; verify `save(..)` 1×                                                                                       |
| Error            | Creator not found                                    | **Given** `creatorId=999` ⇒ `validateCreatorExists(..)` throws `CreatorNotFoundException`. **When** create. **Then** **throws** `CreatorNotFoundException`; **no** save.                                                                                                                                        | assertThrows `CreatorNotFoundException`; verify `save(..)` **never**                                                                |
| Error            | Invalid form (CSV)                                   | **Given** invalid row (e.g., blank `courseName`, `FREE` with `price>0`, `price<0`), and file attached. **When** create. **Then** **throws** `IllegalArgumentException` containing CSV `expectedErrorMessage`; no upload/save.                                                                                   | `toThrow IllegalArgumentException` with message; verify no `uploadFile`, no `save`                                                  |
| Error            | Duplicate (DB)                                       | **Given** valid form & file; `courseRepository.save(..)` throws `DataIntegrityViolationException("duplicate")`. **When** create. **Then** throws same exception.                                                                                                                                                | `toThrow DataIntegrityViolationException("duplicate")`; verify `save(..)` attempted                                                 |
| Error            | Upload fails                                         | **Given** valid row; `uploadFile(..)` throws `RuntimeException("Firebase connection error")` or `IOException`. **When** create. **Then** **throws**; **no** save.                                                                                                                                               | `toThrow` same exception; verify `save(..)` **never**                                                                               |
| Error            | Code generation fails *(if you enable this in impl)* | **Given** `generate6DigitCode()` throws. **When** create. **Then** **throws**; **no** save.                                                                                                                                                                                                                     | `toThrow`; verify `save(..)` **never**                                                                                              |


**changeCourseSatus(courseId, creatorId)**

| Category         | Test Case             | Input                                                                                                                                                            | Expected                                                                   |
| ---------------- | --------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| Happy Path       | Toggle `false → true` | **Given** validator returns owned course with `isPublic=false`. **When** `changeCourseSatus(id, creator)`. **Then** `isPublic==true`, saved once.                | `owned.isPublic == true`; verify `save(owned)` 1×                          |
| Happy Path       | Toggle `true → false` | **Given** owned course `isPublic=true`. **When** toggle. **Then** `isPublic==false`, saved once.                                                                 | `owned.isPublic == false`; verify `save(owned)` 1×                         |
| Happy Path (CSV) | Toggle by row         | **Given** row: `initialPublic`, `expectedPublic`, validator returns owned course. **When** toggle. **Then** `isPublic==expectedPublic`, saved once.              | `owned.isPublic == expectedPublic`; verify `save(..)` 1×                   |
| Edge Case (CSV)  | Boundary IDs          | **Given** smallest/large `courseId` rows; owned course with `initialPublic`. **When** toggle. **Then** `isPublic` flips; save called.                            | `owned.isPublic == !initialPublic`; verify `save(..)` 1×                   |
| Error (CSV)      | Not owner             | **Given** `errorType="NOT_OWNER"` ⇒ validator throws `IllegalStateException("not owner")`. **When** toggle. **Then** **throws**; **no** save.                    | `toThrow IllegalStateException("not owner")`; verify no `save(..)`         |
| Error (CSV)      | Course not found      | **Given** `errorType="COURSE_NOT_FOUND"` ⇒ validator throws `IllegalArgumentException("course missing")`. **When** toggle. **Then** **throws**; **no** save.     | `toThrow IllegalArgumentException("course missing")`; verify no `save(..)` |
| Error            | Repository fails      | **Given** validator returns owned course; `courseRepository.save(..)` throws `RuntimeException("db down")`. **When** toggle. **Then** **throws** same exception. | `toThrow RuntimeException("db down")`; verify `save(..)` attempted         |


**Prompt 3: Generate  Test Code**

Create unit tests for the changeCourseSatus function,createCourse funtion whose comprehensive unit test cases(Test Cases Matrix) you just analyzed above Requirements:
- Include setup/teardown - Use proper assertions (toEqual, toThrow)
- Add descriptive test names
- Mock any external dependencies . file csv tranfer to function test! not using mock data in function just use date from csv
- my project using spring boot,Junit

**Response:**

package com.jpd.web;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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

import com.jpd.web.dto.*;
import com.jpd.web.service.CreatorService;
import com.jpd.web.transform.CreatorTransform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.exception.ApiException;
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
    private CreatorService creatorService;

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
        mockCreator.setCreatorId(1L);
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
            testCreator.setCreatorId(creatorId);
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

        @DisplayName("Happy path: toggles isPublic và gọi repository.save (CSV)")
        @ParameterizedTest(name = "Row {index}: courseId={0}, creatorId={1}, initialPublic={2} -> expected={3}")
        @CsvFileSource(resources = "/course_status_happy.csv", numLinesToSkip = 1)
        void happyPath_toggleAndSave(long courseId, long creatorId, boolean initialPublic, boolean expectedPublic) {
            // Tạo dữ liệu giả thay vì gọi mock trước khi stub
            Creator creator = new Creator();
            creator.setCreatorId(creatorId);

            Course owned = new Course(courseId, creator, initialPublic);
            when(resourceValidator.validateCourseOwnership(courseId, creatorId)).thenReturn(owned);
            when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

            courseService.changeCourseSatus(courseId, creatorId); // hoặc courseService nếu bạn đặt tên như vậy

            assertThat(owned.isPublic()).as("isPublic đã được toggle").isEqualTo(expectedPublic);
            verify(resourceValidator).validateCourseOwnership(courseId, creatorId);
            verify(courseRepository).save(owned);
            verifyNoMoreInteractions(resourceValidator, courseRepository);
        }

        @DisplayName("Edge cases: ID dương nhỏ nhất và cận lớn (CSV)")
        @ParameterizedTest(name = "Row {index}: boundary courseId={0}, creatorId={1}, initialPublic={2}")
        @CsvFileSource(resources = "/course_status_edges.csv", numLinesToSkip = 1)
        void edgeCases_boundaryIds(long courseId, long creatorId, boolean initialPublic) {
            Creator creator = new Creator();
            creator.setCreatorId(creatorId);

            Course owned = new Course(courseId, creator, initialPublic);
            when(resourceValidator.validateCourseOwnership(courseId, creatorId)).thenReturn(owned);
            when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

            courseService.changeCourseSatus(courseId, creatorId);

            assertThat(owned.isPublic()).isEqualTo(!initialPublic);
            verify(resourceValidator).validateCourseOwnership(courseId, creatorId);
            verify(courseRepository).save(owned);
            verifyNoMoreInteractions(resourceValidator, courseRepository);
        }

        @DisplayName("Error: validateCourseOwnership ném lỗi -> không gọi repository.save (CSV)")
        @ParameterizedTest(name = "Row {index}: courseId={0}, creatorId={1}, errorType={2}")
        @CsvFileSource(resources = "/course_status_errors.csv", numLinesToSkip = 1)
        void errorScenarios_validatorThrows(long courseId, long creatorId, String errorType) {
            RuntimeException ex = switch (errorType) {
                case "NOT_OWNER" -> new IllegalStateException("not owner");
                case "COURSE_NOT_FOUND" -> new IllegalArgumentException("course missing");
                default -> new RuntimeException("validator exploded");
            };
            when(resourceValidator.validateCourseOwnership(courseId, creatorId)).thenThrow(ex);


// toThrow -> AssertJ assertThatThrownBy
assertThatThrownBy(() -> courseService.changeCourseSatus(courseId, creatorId))
.isInstanceOf(ex.getClass())
.hasMessageContaining(ex.getMessage());


            verify(resourceValidator).validateCourseOwnership(courseId, creatorId);
            verifyNoInteractions(courseRepository);
        }


        // --------------------------------------------------
// ❌ Error: repository.save ném lỗi -> propagate
// --------------------------------------------------
@DisplayName("Error: repository.save thất bại -> ném tiếp ngoại lệ")
@Test
void repositoryThrows_isPropagated() {
long courseId = 101L, creatorId = 7L;
Course owned = new Course();
when(resourceValidator.validateCourseOwnership(courseId, creatorId)).thenReturn(owned);
when(courseRepository.save(any(Course.class))).thenThrow(new RuntimeException("db down"));


            assertThatThrownBy(() -> courseService.changeCourseSatus(courseId, creatorId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("db down");


            verify(resourceValidator).validateCourseOwnership(courseId, creatorId);
            verify(courseRepository).save(owned);
            verifyNoMoreInteractions(resourceValidator, courseRepository);
        }
    }
}
## Title:  for ModuleContentService

**Prompt 1: Analyze Functionality**

