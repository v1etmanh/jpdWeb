package com.jpd.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import com.jpd.web.service.utils.ValidationResources;
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

import com.jpd.web.exception.*;
import com.jpd.web.model.*;
import com.jpd.web.repository.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationResources Tests")
class ValidationResourcesTest {

    @Mock
    private CreatorRepository creatorRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private ValidationResources validationResources;

    private Creator mockCreator;
    private Course mockCourse;
    private Chapter mockChapter;
    private com.jpd.web.model.Module mockModule;
    private Customer mockCustomer;
    private Enrollment mockEnrollment;

    @BeforeEach
    void setUp() {
        // Setup mock Creator
        mockCreator = new Creator();
        mockCreator.setCreatorId(1L);

        // Setup mock Course
        mockCourse = new Course();
        mockCourse.setCourseId(1L);
        mockCourse.setCreator(mockCreator);

        // Setup mock Chapter
        mockChapter = new Chapter();
        mockChapter.setChapterId(1L);
        mockChapter.setCourse(mockCourse);

        // Setup mock Module
        mockModule = new com.jpd.web.model.Module();
        mockModule.setModuleId(1L);
        mockModule.setChapter(mockChapter);

        // Setup mock Customer
        mockCustomer = new Customer();
        mockCustomer.setCustomerId(1L);
        mockCustomer.setEmail("test@test.com");

        // Setup mock Enrollment
        mockEnrollment = new Enrollment();
        mockEnrollment.setCourse(mockCourse);
        mockEnrollment.setCustomer(mockCustomer);
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
                System.err.println("WARNING: Empty CSV file: " + fileName);
                return testData;
            }

            String[] headers = headerLine.split(",");
            String line;
            int lineNumber = 1;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] values = line.split(",", -1);
                Map<String, String> row = new HashMap<>();

                for (int i = 0; i < headers.length; i++) {
                    String header = headers[i].trim();
                    String value = i < values.length ? values[i].trim() : "";
                    row.put(header, value);
                }

                // Debug: print parsed row
                System.out.println("Line " + lineNumber + " parsed: " + row);

                testData.add(row);
            }
        } catch (Exception e) {
            System.err.println("ERROR reading CSV file: " + fileName);
            e.printStackTrace();
            throw e;
        }

        System.out.println("Total rows parsed from " + fileName + ": " + testData.size());
        return testData;
    }

    @Nested
    @DisplayName("validateCourseOwnership() Tests - Data Driven")
    class ValidateCourseOwnershipTestsFromCSV {

        @TestFactory
        @DisplayName("Run all course ownership validation test cases from CSV")
        List<DynamicTest> testCourseOwnershipFromCSV() throws IOException {
            List<Map<String, String>> testData = readTestDataFromCSV("test-data-course-ownership.csv");
            List<DynamicTest> dynamicTests = new ArrayList<>();

            for (Map<String, String> data : testData) {
                String testCase = data.get("testCase");

                DynamicTest test = DynamicTest.dynamicTest(testCase, () -> {
                    executeCourseOwnershipTest(data);
                });

                dynamicTests.add(test);
            }

            return dynamicTests;
        }

        private void executeCourseOwnershipTest(Map<String, String> data) {
            // Reset mocks
            reset(creatorRepository, courseRepository);

            // Parse test data
            Long courseId = Long.parseLong(data.get("courseId"));
            Long creatorId = Long.parseLong(data.get("creatorId"));
            Long courseOwnerId = Long.parseLong(data.get("courseOwnerId"));
            boolean shouldSucceed = Boolean.parseBoolean(data.get("shouldSucceed"));
            String expectedErrorType = data.get("expectedErrorType");
            String expectedErrorMessage = data.get("expectedErrorMessage");

            // Setup test entities
            Creator testCreator = new Creator();
            testCreator.setCreatorId(creatorId);

            Creator courseOwner = new Creator();
            courseOwner.setCreatorId(courseOwnerId);

            Course testCourse = new Course();
            testCourse.setCourseId(courseId);
            testCourse.setCreator(courseOwner);

            // Setup mocks
            if (creatorId == 999L) {
                lenient().when(creatorRepository.findById(creatorId)).thenReturn(Optional.empty());
            } else {
                lenient().when(creatorRepository.findById(creatorId)).thenReturn(Optional.of(testCreator));
            }

            if (courseId == 999L) {
                lenient().when(courseRepository.findById(courseId)).thenReturn(Optional.empty());
            } else {
                lenient().when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
            }

            // Execute and verify
            if (shouldSucceed) {
                Course result = validationResources.validateCourseOwnership(courseId, creatorId);
                assertNotNull(result);
                assertEquals(courseId, result.getCourseId());
            } else {
                Exception exception = assertThrows(Exception.class,
                        () -> validationResources.validateCourseOwnership(courseId, creatorId));

                String actualExceptionType = exception.getClass().getSimpleName();
                assertTrue(actualExceptionType.equals(expectedErrorType) || actualExceptionType.contains(expectedErrorType.replace("Exception", "")),
                        "Expected " + expectedErrorType + " but got " + actualExceptionType);

                if (!expectedErrorMessage.isEmpty()) {
                    assertTrue(exception.getMessage().contains(expectedErrorMessage),
                            "Expected message to contain: '" + expectedErrorMessage + "' but got: '" + exception.getMessage() + "'");
                }
            }
        }
    }

    @Nested
    @DisplayName("validateChapterBelongsToCourse() Tests - Data Driven")
    class ValidateChapterBelongsToCourseTestsFromCSV {

        @TestFactory
        @DisplayName("Run all chapter validation test cases from CSV")
        List<DynamicTest> testChapterValidationFromCSV() throws IOException {
            List<Map<String, String>> testData = readTestDataFromCSV("test-data-chapter-validation.csv");
            List<DynamicTest> dynamicTests = new ArrayList<>();

            for (Map<String, String> data : testData) {
                String testCase = data.get("testCase");

                DynamicTest test = DynamicTest.dynamicTest(testCase, () -> {
                    executeChapterValidationTest(data);
                });

                dynamicTests.add(test);
            }

            return dynamicTests;
        }

        private void executeChapterValidationTest(Map<String, String> data) {
            // Reset mocks
            reset(chapterRepository);

            // Parse test data
            Long chapterId = Long.parseLong(data.get("chapterId"));
            Long courseId = Long.parseLong(data.get("courseId"));
            Long chapterCourseId = Long.parseLong(data.get("chapterCourseId"));
            boolean shouldSucceed = Boolean.parseBoolean(data.get("shouldSucceed"));
            String expectedErrorType = data.get("expectedErrorType");
            String expectedErrorMessage = data.get("expectedErrorMessage");

            // Setup test entities
            Course testCourse = new Course();
            testCourse.setCourseId(chapterCourseId);

            Chapter testChapter = new Chapter();
            testChapter.setChapterId(chapterId);
            testChapter.setCourse(testCourse);

            // Setup mocks
            if (chapterId == 999L) {
                lenient().when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());
            } else {
                lenient().when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(testChapter));
            }

            // Execute and verify
            if (shouldSucceed) {
                Chapter result = validationResources.validateChapterBelongsToCourse(chapterId, courseId);
                assertNotNull(result);
                assertEquals(chapterId, result.getChapterId());
            } else {
                Exception exception = assertThrows(Exception.class,
                        () -> validationResources.validateChapterBelongsToCourse(chapterId, courseId));

                String actualExceptionType = exception.getClass().getSimpleName();
                assertTrue(actualExceptionType.equals(expectedErrorType) || actualExceptionType.contains(expectedErrorType.replace("Exception", "")),
                        "Expected " + expectedErrorType + " but got " + actualExceptionType);

                if (!expectedErrorMessage.isEmpty()) {
                    assertTrue(exception.getMessage().contains(expectedErrorMessage),
                            "Expected message to contain: '" + expectedErrorMessage + "' but got: '" + exception.getMessage() + "'");
                }
            }
        }
    }

    @Nested
    @DisplayName("validateModuleBelongsToChapter() Tests - Data Driven")
    class ValidateModuleBelongsToChapterTestsFromCSV {

        @TestFactory
        @DisplayName("Run all module validation test cases from CSV")
        List<DynamicTest> testModuleValidationFromCSV() throws IOException {
            List<Map<String, String>> testData = readTestDataFromCSV("test-data-module-validation.csv");
            List<DynamicTest> dynamicTests = new ArrayList<>();

            for (Map<String, String> data : testData) {
                String testCase = data.get("testCase");

                DynamicTest test = DynamicTest.dynamicTest(testCase, () -> {
                    executeModuleValidationTest(data);
                });

                dynamicTests.add(test);
            }

            return dynamicTests;
        }

        private void executeModuleValidationTest(Map<String, String> data) {
            // Reset mocks
            reset(moduleRepository);

            // Parse test data
            Long moduleId = Long.parseLong(data.get("moduleId"));
            Long chapterId = Long.parseLong(data.get("chapterId"));
            Long moduleChapterId = Long.parseLong(data.get("moduleChapterId"));
            boolean shouldSucceed = Boolean.parseBoolean(data.get("shouldSucceed"));
            String expectedErrorType = data.get("expectedErrorType");
            String expectedErrorMessage = data.get("expectedErrorMessage");

            // Setup test entities
            Chapter testChapter = new Chapter();
            testChapter.setChapterId(moduleChapterId);

            com.jpd.web.model.Module testModule = new com.jpd.web.model.Module();
            testModule.setModuleId(moduleId);
            testModule.setChapter(testChapter);

            // Setup mocks
            if (moduleId == 999L) {
                lenient().when(moduleRepository.findById(moduleId)).thenReturn(Optional.empty());
            } else {
                lenient().when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(testModule));
            }

            // Execute and verify
            if (shouldSucceed) {
                com.jpd.web.model.Module result = validationResources.validateModuleBelongsToChapter(moduleId, chapterId);
                assertNotNull(result);
                assertEquals(moduleId, result.getModuleId());
            } else {
                Exception exception = assertThrows(Exception.class,
                        () -> validationResources.validateModuleBelongsToChapter(moduleId, chapterId));

                String actualExceptionType = exception.getClass().getSimpleName();
                assertTrue(actualExceptionType.equals(expectedErrorType) || actualExceptionType.contains(expectedErrorType.replace("Exception", "")),
                        "Expected " + expectedErrorType + " but got " + actualExceptionType);

                if (!expectedErrorMessage.isEmpty()) {
                    assertTrue(exception.getMessage().contains(expectedErrorMessage),
                            "Expected message to contain: '" + expectedErrorMessage + "' but got: '" + exception.getMessage() + "'");
                }
            }
        }
    }

    @Nested
    @DisplayName("validateCustomerExist() Tests - Data Driven")
    class ValidateCustomerExistTestsFromCSV {

        @TestFactory
        @DisplayName("Run all customer validation test cases from CSV")
        List<DynamicTest> testCustomerValidationFromCSV() throws IOException {
            List<Map<String, String>> testData = readTestDataFromCSV("test-data-customer-validation.csv");
            List<DynamicTest> dynamicTests = new ArrayList<>();

            for (Map<String, String> data : testData) {
                String testCase = data.get("testCase");

                DynamicTest test = DynamicTest.dynamicTest(testCase, () -> {
                    executeCustomerValidationTest(data);
                });

                dynamicTests.add(test);
            }

            return dynamicTests;
        }

        private void executeCustomerValidationTest(Map<String, String> data) {
            // Reset mocks
            reset(customerRepository);

            // Parse test data
            String customerEmail = data.get("customerEmail");
            boolean shouldSucceed = Boolean.parseBoolean(data.get("shouldSucceed"));
            String expectedErrorType = data.get("expectedErrorType");
            String expectedErrorMessage = data.get("expectedErrorMessage");

            // Setup test entities
            Customer testCustomer = new Customer();
            testCustomer.setEmail(customerEmail);

            // Setup mocks
            if (customerEmail.equals("notfound@test.com")) {
                lenient().when(customerRepository.findByEmail(customerEmail)).thenReturn(Optional.empty());
            } else {
                lenient().when(customerRepository.findByEmail(customerEmail)).thenReturn(Optional.of(testCustomer));
            }

            // Execute and verify
            if (shouldSucceed) {
                Customer result = validationResources.validateCustomerExist(customerEmail);
                assertNotNull(result);
                assertEquals(customerEmail, result.getEmail());
            } else {
                Exception exception = assertThrows(Exception.class,
                        () -> validationResources.validateCustomerExist(customerEmail));

                String actualExceptionType = exception.getClass().getSimpleName();
                assertTrue(actualExceptionType.equals(expectedErrorType) || actualExceptionType.contains(expectedErrorType.replace("Exception", "")),
                        "Expected " + expectedErrorType + " but got " + actualExceptionType);

                if (!expectedErrorMessage.isEmpty()) {
                    assertTrue(exception.getMessage().contains(expectedErrorMessage),
                            "Expected message to contain: '" + expectedErrorMessage + "' but got: '" + exception.getMessage() + "'");
                }
            }
        }
    }

    @Nested
    @DisplayName("validateCustomerWithCourse() Tests - Data Driven")
    class ValidateCustomerWithCourseTestsFromCSV {

        @TestFactory
        @DisplayName("Run all customer course access test cases from CSV")
        List<DynamicTest> testCustomerCourseAccessFromCSV() throws IOException {
            List<Map<String, String>> testData = readTestDataFromCSV("test-data-customer-course-access.csv");
            List<DynamicTest> dynamicTests = new ArrayList<>();

            for (Map<String, String> data : testData) {
                String testCase = data.get("testCase");

                DynamicTest test = DynamicTest.dynamicTest(testCase, () -> {
                    executeCustomerCourseAccessTest(data);
                });

                dynamicTests.add(test);
            }

            return dynamicTests;
        }

        private void executeCustomerCourseAccessTest(Map<String, String> data) {
            // Reset mocks
            reset(courseRepository, customerRepository, enrollmentRepository, creatorRepository);

            // Parse test data
            String customerEmail = data.get("customerEmail");
            Long courseId = Long.parseLong(data.get("courseId"));
            boolean isCreator = Boolean.parseBoolean(data.get("isCreator"));
            boolean isEnrolled = Boolean.parseBoolean(data.get("isEnrolled"));
            boolean shouldSucceed = Boolean.parseBoolean(data.get("shouldSucceed"));
            String expectedErrorType = data.get("expectedErrorType");
            String expectedErrorMessage = data.get("expectedErrorMessage");

            // Setup test entities
            Customer testCustomer = new Customer();
            testCustomer.setCustomerId(isCreator ? 1L : 2L);
            testCustomer.setEmail(customerEmail);

            Customer creatorCustomer = new Customer();
            creatorCustomer.setCustomerId(1L);
            creatorCustomer.setEmail("creator@test.com");

            Creator testCreator = new Creator();
            testCreator.setCreatorId(1L);
            testCreator.setCustomer(creatorCustomer);

            Course testCourse = new Course();
            testCourse.setCourseId(courseId);
            testCourse.setCreator(testCreator);

            Enrollment testEnrollment = new Enrollment();
            testEnrollment.setCourse(testCourse);
            testEnrollment.setCustomer(testCustomer);

            // Setup mocks
            if (courseId == 999L) {
                lenient().when(courseRepository.findById(courseId)).thenReturn(Optional.empty());
            } else {
                lenient().when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
            }

            if (customerEmail.equals("notfound@test.com")) {
                lenient().when(customerRepository.findByEmail(customerEmail)).thenReturn(Optional.empty());
            } else {
                lenient().when(customerRepository.findByEmail(customerEmail)).thenReturn(Optional.of(testCustomer));
            }

            if (isEnrolled) {
                lenient().when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(anyLong(), anyLong()))
                        .thenReturn(Optional.of(testEnrollment));
            } else {
                lenient().when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(anyLong(), anyLong()))
                        .thenReturn(Optional.empty());
            }

            // Execute and verify
            if (shouldSucceed) {
                Course result = validationResources.validateCustomerWithCourse(customerEmail, courseId);
                assertNotNull(result);
                assertEquals(courseId, result.getCourseId());
            } else {
                Exception exception = assertThrows(Exception.class,
                        () -> validationResources.validateCustomerWithCourse(customerEmail, courseId));

                // Debug output
                System.out.println("Test case: " + data.get("testCase"));
                System.out.println("Expected: " + expectedErrorType);
                System.out.println("Actual: " + exception.getClass().getSimpleName());
                System.out.println("Message: " + exception.getMessage());

                String actualExceptionType = exception.getClass().getSimpleName();
                assertTrue(actualExceptionType.equals(expectedErrorType) || actualExceptionType.contains(expectedErrorType.replace("Exception", "")),
                        "Expected " + expectedErrorType + " but got " + actualExceptionType +
                                ". Message: " + exception.getMessage());

                if (!expectedErrorMessage.isEmpty()) {
                    assertTrue(exception.getMessage().contains(expectedErrorMessage),
                            "Expected message to contain: '" + expectedErrorMessage + "' but got: '" + exception.getMessage() + "'");
                }
            }
        }
    }

    // Keep original individual tests for specific edge cases
    @Nested
    @DisplayName("validateCompleteOwnership() Tests")
    class ValidateCompleteOwnershipTests {

        @Test
        @DisplayName("Should validate complete ownership chain successfully")
        void shouldValidateCompleteOwnershipChainSuccessfully() {
            // Given
            when(creatorRepository.findById(1L)).thenReturn(Optional.of(mockCreator));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(mockCourse));
            when(chapterRepository.findById(1L)).thenReturn(Optional.of(mockChapter));
            when(moduleRepository.findById(1L)).thenReturn(Optional.of(mockModule));

            // When
            com.jpd.web.model.Module result = validationResources.validateCompleteOwnership(1L, 1L, 1L, 1L);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getModuleId());
            assertEquals(mockChapter, result.getChapter());
        }
    }

    @Nested
    @DisplayName("validateCreatorExists() Tests")
    class ValidateCreatorExistsTests {

        @Test
        @DisplayName("Should return creator when exists")
        void shouldReturnCreatorWhenExists() {
            when(creatorRepository.findById(1L)).thenReturn(Optional.of(mockCreator));
            Creator result = validationResources.validateCreatorExists(1L);
            assertNotNull(result);
            assertEquals(1L, result.getCreatorId());
        }

        @Test
        @DisplayName("Should throw exception when creator doesn't exist")
        void shouldThrowExceptionWhenCreatorNotFound() {
            when(creatorRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(CreatorNotFoundException.class,
                    () -> validationResources.validateCreatorExists(999L));
        }
    }
}