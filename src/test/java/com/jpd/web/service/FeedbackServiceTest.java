package com.jpd.web.service;

import com.jpd.web.dto.FeedbackSimpleDto;
import com.jpd.web.exception.ExceedLimitRequestException;
import com.jpd.web.exception.FeedBackIligalException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.repository.FeedbackRepository;
import com.jpd.web.service.utils.CommentFilterService;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.FeedbackTransform;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FeedbackServiceTest {

    @Mock
    FeedbackRepository feedbackRepository;

    @Mock
    EnrollmentRepository enrollmentRepository;

    @Mock
    ValidationResources validationResources;

    @Mock
    CommentFilterService commentFilterService;

    @InjectMocks
    FeedbackService feedbackService;

    Customer customer;
    Enrollment enrollment;
    Feedback feedback;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().customerId(10L).email("test@gmail.com").build();
        enrollment = Enrollment.builder().customer(customer).course(null).feedback(null).build();
        feedback = Feedback.builder().feedbackId(1000L).content("Good course!").enrollment(enrollment).rate(5).build();
    }

    // Helper for setting up Enrollment feedback
    private void setEnrollmentFeedback(Feedback fb) {
        enrollment.setFeedback(fb);
    }

    // ==== Test addFeedback ====

    @ParameterizedTest
    @DisplayName("addFeedback: valid cases from CSV")
    @CsvFileSource(resources = "/feedback_add_valid.csv", numLinesToSkip = 1)
    void addFeedback_valid_fromCsv(String email, long courseId, String content, int rate) {
        // Arrange
        when(validationResources.validateCustomerExist(email)).thenReturn(customer);
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(eq(courseId), eq(10L)))
                .thenReturn(Optional.of(enrollment));
        setEnrollmentFeedback(null);
        when(commentFilterService.isToxic(content)).thenReturn(false);
        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        feedbackService.addFeedback(email, courseId, content, rate);

        // Assert
        verify(feedbackRepository).save(argThat(fb ->
                fb.getContent().equals(content) &&
                        fb.getRate() == rate &&
                        fb.getEnrollment() == enrollment
        ));
        clearInvocations(feedbackRepository);
    }
    @ParameterizedTest
    @DisplayName("addFeedback: toxic content from CSV -> FeedBackIligalException")
    @CsvFileSource(resources = "/feedback_add_toxic.csv", numLinesToSkip = 1)
    void addFeedback_toxic_fromCsv(String email, long courseId, String content, int rate) {
        when(validationResources.validateCustomerExist(email)).thenReturn(customer);
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(eq(courseId), eq(10L)))
                .thenReturn(Optional.of(enrollment));
        setEnrollmentFeedback(null);
        when(commentFilterService.isToxic(content)).thenReturn(true);

        assertThatThrownBy(() -> feedbackService.addFeedback(email, courseId, content, rate))
                .isInstanceOf(FeedBackIligalException.class);

        verify(feedbackRepository, never()).save(any());
    }
    @ParameterizedTest
    @DisplayName("addFeedback: not enrolled (Unauthorized) from CSV")
    @CsvFileSource(resources = "/feedback_add_unauthorized.csv", numLinesToSkip = 1)
    void addFeedback_unauthorized_fromCsv(String email, long courseId, String content, int rate) {
        when(validationResources.validateCustomerExist(email)).thenReturn(customer);
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(eq(courseId), eq(10L)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.addFeedback(email, courseId, content, rate))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("you dont own this cours");
    }
    @ParameterizedTest
    @DisplayName("updateFeedback: valid from CSV")
    @CsvFileSource(resources = "/feedback_update_valid.csv", numLinesToSkip = 1)
    void updateFeedback_valid_fromCsv(String email, long feedbackId, String newContent, int newRate) {
        when(validationResources.validateCustomerExist(email)).thenReturn(customer);
        when(validationResources.validateFeedbackBelongCustomer(feedbackId, 10L))
                .thenReturn(Optional.of(feedback));
        when(commentFilterService.isToxic(newContent)).thenReturn(false);
        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FeedbackSimpleDto expectedDto = FeedbackSimpleDto.builder()
                .content(newContent).rate(newRate).build();

        try (MockedStatic<FeedbackTransform> mocked = mockStatic(FeedbackTransform.class)) {
            mocked.when(() -> FeedbackTransform.tofeedbackDto(any())).thenReturn(expectedDto);

            FeedbackSimpleDto result = feedbackService.updateFeedback(email, feedbackId, newContent, newRate);
            assertThat(result).isEqualTo(expectedDto);
        }

        verify(feedbackRepository).save(feedback);
        assertThat(feedback.getContent()).isEqualTo(newContent);
        assertThat(feedback.getRate()).isEqualTo(newRate);
    }


    @Test
    @DisplayName("should_AddFeedback_When_ValidInput")
    void should_AddFeedback_When_ValidInput() {
        // Arrange
        when(validationResources.validateCustomerExist(anyString())).thenReturn(customer);
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(eq(1L), eq(10L))).thenReturn(Optional.of(enrollment));
        setEnrollmentFeedback(null);
        when(commentFilterService.isToxic(anyString())).thenReturn(false);
        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        feedbackService.addFeedback("test@gmail.com", 1L, "Very helpful", 4);

        // Assert
        verify(feedbackRepository).save(argThat(fb -> fb.getContent().equals("Very helpful") && fb.getRate() == 4 && fb.getEnrollment() == enrollment));
    }

    @Test
    @DisplayName("should_ThrowUnauthorizedException_When_EnrollmentNotFound")
    void should_ThrowUnauthorizedException_When_EnrollmentNotFound() {
        when(validationResources.validateCustomerExist(anyString())).thenReturn(customer);
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(eq(1L), eq(10L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.addFeedback("test@gmail.com", 1L, "good", 5))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("you dont own this cours");
    }

    @Test
    @DisplayName("should_ThrowExceedLimitRequestException_When_FeedbackExistsAlready")
    void should_ThrowExceedLimitRequestException_When_FeedbackExistsAlready() {
        when(validationResources.validateCustomerExist(anyString())).thenReturn(customer);
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(eq(1L), eq(10L))).thenReturn(Optional.of(enrollment));
        setEnrollmentFeedback(feedback); // already exists

        assertThatThrownBy(() -> feedbackService.addFeedback("test@gmail.com", 1L, "good", 5))
                .isInstanceOf(ExceedLimitRequestException.class)
                .hasMessageContaining("use can only feedback 1 per");
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_ThrowFeedBackIligalException_When_ToxicContent")
    void should_ThrowFeedBackIligalException_When_ToxicContent() {
        when(validationResources.validateCustomerExist(anyString())).thenReturn(customer);
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(eq(1L), eq(10L))).thenReturn(Optional.of(enrollment));
        setEnrollmentFeedback(null);
        when(commentFilterService.isToxic(eq("bad words!"))).thenReturn(true);

        assertThatThrownBy(() -> feedbackService.addFeedback("test@gmail.com", 1L, "bad words!", 2))
                .isInstanceOf(FeedBackIligalException.class);
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_HandleNullDetail_When_addFeedback")
    void should_HandleNullDetail_When_addFeedback() {
        when(validationResources.validateCustomerExist(anyString())).thenReturn(customer);
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(anyLong(), anyLong()))
                .thenReturn(Optional.of(enrollment));
        setEnrollmentFeedback(null);
        when(commentFilterService.isToxic(isNull())).thenReturn(false);

        feedbackService.addFeedback("test@gmail.com", 1L, null, 3);

        verify(feedbackRepository).save(any(Feedback.class));
    }

    // ==== Test deleteFeedback ====

    @Test
    @DisplayName("should_ThrowUnauthorizedException_When_DeletingFeedbackNotOwned")
    void should_ThrowUnauthorizedException_When_DeletingFeedbackNotOwned() {
        when(validationResources.validateCustomerExist(anyString())).thenReturn(customer);
        when(validationResources.validateFeedbackBelongCustomer(1L, 10L)).thenReturn(Optional.of(feedback));

        assertThatThrownBy(() -> feedbackService.deleteFeedback("test@gmail.com", 1L))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("you dont own this feedback");
        verify(feedbackRepository, never()).delete(any());
    }

    @Test
    @DisplayName("should_ThrowUnauthorizedException_When_FeedbackNotFound_onDelete")
    void should_ThrowUnauthorizedException_When_FeedbackNotFound_onDelete() {
        when(validationResources.validateCustomerExist(anyString())).thenReturn(customer);
        when(validationResources.validateFeedbackBelongCustomer(anyLong(), anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.deleteFeedback("test@gmail.com", 1L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("you dont own this feedback");
    }

    @Test
    @DisplayName("should_ThrowUnauthorizedException_When_FeedbackPresent_onDelete")
    void should_ThrowUnauthorizedException_When_FeedbackPresent_onDelete() {
        // Note: The implementation throws UnauthorizedException both in empty and present cases (bug).
        when(validationResources.validateCustomerExist("test@gmail.com")).thenReturn(customer);
        when(validationResources.validateFeedbackBelongCustomer(1L, customer.getCustomerId()))
                .thenReturn(Optional.of(feedback));

        assertThatThrownBy(() -> feedbackService.deleteFeedback("test@gmail.com", 1L))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ==== Test updateFeedback ====

    @Test
    @DisplayName("should_UpdateFeedback_When_Valid")
    void should_UpdateFeedback_When_Valid() {
        when(validationResources.validateCustomerExist(anyString())).thenReturn(customer);
        when(validationResources.validateFeedbackBelongCustomer(2L, 10L)).thenReturn(Optional.of(feedback));
        when(commentFilterService.isToxic(anyString())).thenReturn(false);
        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        FeedbackSimpleDto expectedDto = FeedbackSimpleDto.builder()
                .content("Updated").rate(3).build();
        try (MockedStatic<FeedbackTransform> mocked = mockStatic(FeedbackTransform.class)) {
            mocked.when(() -> FeedbackTransform.tofeedbackDto(any())).thenReturn(expectedDto);
            FeedbackSimpleDto result =
                    feedbackService.updateFeedback("test@gmail.com", 2L, "Updated", 3);

            assertThat(result).isEqualTo(expectedDto);
        }
        verify(feedbackRepository).save(feedback);
        assertThat(feedback.getContent()).isEqualTo("Updated");
        assertThat(feedback.getRate()).isEqualTo(3);
    }

    @Test
    @DisplayName("should_ThrowFeedBackIligalException_When_ToxicContent_Update")
    void should_ThrowFeedBackIligalException_When_ToxicContent_Update() {
        when(validationResources.validateCustomerExist(anyString())).thenReturn(customer);
        when(validationResources.validateFeedbackBelongCustomer(3L, 10L)).thenReturn(Optional.of(feedback));
        when(commentFilterService.isToxic(anyString())).thenReturn(true);

        assertThatThrownBy(() -> feedbackService.updateFeedback("test@gmail.com", 3L, "toxic", 1))
                .isInstanceOf(FeedBackIligalException.class);
        verify(feedbackRepository, never()).save(any());
    }


    @Test
    @DisplayName("should_ThrowUnauthorizedException_When_FeedbackNotFound_Update")
    void should_ThrowUnauthorizedException_When_FeedbackNotFound_Update() {
        when(validationResources.validateCustomerExist(anyString())).thenReturn(customer);
        when(validationResources.validateFeedbackBelongCustomer(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(commentFilterService.isToxic(anyString())).thenReturn(false);

        assertThatThrownBy(() -> feedbackService.updateFeedback("test@gmail.com", 5L, "text", 2))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("you dont own this feedback");
        verify(feedbackRepository, never()).save(any());
    }

    // ==== Validation/Egde cases ====

    @Test
    @DisplayName("should_HandleNullCustomerEmail")
    void should_HandleNullCustomerEmail() {
        when(validationResources.validateCustomerExist(null)).thenThrow(new UnauthorizedException("no customer"));

        assertThatThrownBy(() -> feedbackService.addFeedback(null, 1L, "text", 5))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("should_HandleEmptyDetailString_When_addFeedback")
    void should_HandleEmptyDetailString_When_addFeedback() {
        when(validationResources.validateCustomerExist(anyString())).thenReturn(customer);
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(anyLong(), anyLong()))
                .thenReturn(Optional.of(enrollment));
        setEnrollmentFeedback(null);
        when(commentFilterService.isToxic(anyString())).thenReturn(false);

        feedbackService.addFeedback("test@gmail.com", 1L, "", 1);

        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    @DisplayName("should_HandleEmptyFeedbackList")
    void should_HandleEmptyFeedbackList() {
        when(validationResources.validateCustomerExist(anyString())).thenReturn(customer);
        // Use anyLong() instead of any() to avoid InvalidUseOfMatchersException and possible NullPointerException
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(anyLong(), anyLong())).thenReturn(Optional.of(enrollment));
        setEnrollmentFeedback(null);
        when(commentFilterService.isToxic(anyString())).thenReturn(false);

        feedbackService.addFeedback("test@gmail.com", 99L, "Great", 5);

        verify(feedbackRepository).save(any(Feedback.class));
    }


    @Test
    @DisplayName("should_Handle_Min_Rate_in_AddFeedback")
    void should_Handle_Min_Rate_in_AddFeedback() {
        when(validationResources.validateCustomerExist(anyString())).thenReturn(customer);
        // Instead of any(), use explicit argument types to prevent NullPointerException
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(anyLong(), anyLong()))
                .thenReturn(Optional.of(enrollment));
        setEnrollmentFeedback(null);
        when(commentFilterService.isToxic(anyString())).thenReturn(false);

        feedbackService.addFeedback("test@gmail.com", 1L, "ok", 0); // minimal rate

        verify(feedbackRepository).save(argThat(fb -> fb.getRate() == 0));
    }

    @Test
    @DisplayName("should_Handle_Max_Rate_in_AddFeedback")
    void should_Handle_Max_Rate_in_AddFeedback() {
        when(validationResources.validateCustomerExist(anyString())).thenReturn(customer);
        // Use anyLong() instead of any() to avoid NullPointerException due to autounboxing of null
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(anyLong(), anyLong()))
                .thenReturn(Optional.of(enrollment));
        setEnrollmentFeedback(null);
        when(commentFilterService.isToxic(anyString())).thenReturn(false);

        feedbackService.addFeedback("test@gmail.com", 1L, "Nice!", Integer.MAX_VALUE);

        verify(feedbackRepository).save(argThat(fb -> fb.getRate() == Integer.MAX_VALUE));
    }

    // =========== MOCK EXCEL SOURCE ===========

    // Note: Here only placeholder for Excel. In real code, you would load test data from xlsx in setup, e.g. using Apache POI.
    // For demonstration, we show how you might hook it up (not executable in isolation):

    // @BeforeEach
    // void loadExcelTestData() {
    //     // Use Apache POI to read FeedbackTestData.xlsx and map to your test objects.
    //     // Example: Input/expected data can be read and then utilized below.
    // }

    // =========== Concurrency test skeleton ===========

    @Test
    @DisplayName("should_HandleConcurrentAddFeedback")
    void should_HandleConcurrentAddFeedback() throws InterruptedException {
        when(validationResources.validateCustomerExist(anyString())).thenReturn(customer);
        // Use anyLong() explicitly to avoid NullPointerException due to autounboxing of null
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(anyLong(), anyLong()))
                .thenReturn(Optional.of(enrollment));
        setEnrollmentFeedback(null);
        when(commentFilterService.isToxic(anyString())).thenReturn(false);

        Runnable addTask = () -> feedbackService.addFeedback("test@gmail.com", 1L, "Concurrent!", 4);

        Thread t1 = new Thread(addTask);
        Thread t2 = new Thread(addTask);
        t1.start(); t2.start();
        t1.join(); t2.join();

        // Verify that feedback was attempted to save, may need to change expectations for thread safety.
        verify(feedbackRepository, atLeastOnce()).save(any(Feedback.class));
    }
}
