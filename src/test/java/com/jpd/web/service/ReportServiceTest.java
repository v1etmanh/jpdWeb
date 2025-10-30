package com.jpd.web.service;

import com.jpd.web.dto.ReportForm;
import com.jpd.web.model.*;
import com.jpd.web.repository.ReportRepository;
import com.jpd.web.service.utils.ValidationResources;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    ReportRepository reportRepository;

    @Mock
    ValidationResources validationResources;

    @InjectMocks
    ReportService reportService;

    Customer mockCustomer;
    Course mockCourse;
    ReportForm.ReportFormBuilder reportFormBuilder;

    @BeforeEach
    void setUp() {
        mockCustomer = Customer.builder().customerId(100L).email("foo@bar.com").build();
        mockCourse = Course.builder().courseId(200L).name("Test Course").build();

        reportFormBuilder = ReportForm.builder()
                .type(ReportType.INAPPROPRIATE_CONTENT)
                .detail("Nội dung không phù hợp")
                .courseId(200L);
    }

    @Test
    @DisplayName("should_SaveReport_When_ReportFormValid")
    void should_SaveReport_When_ReportFormValid() {
        // Given
        ReportForm form = reportFormBuilder.build();

        when(validationResources.validateCustomerExist("foo@bar.com")).thenReturn(mockCustomer);
        when(validationResources.validateCustomerWithCourse("foo@bar.com", 200L)).thenReturn(mockCourse);

        // When
        reportService.saveReport("foo@bar.com", form);

        // Then
        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(reportCaptor.capture());

        Report savedReport = reportCaptor.getValue();
        assertThat(savedReport.getCustomer()).isEqualTo(mockCustomer);
        assertThat(savedReport.getCourse()).isEqualTo(mockCourse);
        assertThat(savedReport.getDetail()).isEqualTo("Nội dung không phù hợp");
        assertThat(savedReport.getType()).isEqualTo(ReportType.INAPPROPRIATE_CONTENT);
    }

    @Test
    @DisplayName("should_ThrowException_When_CustomerNotExist")
    void should_ThrowException_When_CustomerNotExist() {
        // Given
        ReportForm form = reportFormBuilder.build();
        when(validationResources.validateCustomerExist(anyString()))
                .thenThrow(new RuntimeException("Customer not found"));

        // When/Then
        assertThatThrownBy(() -> reportService.saveReport("nonexistent@foo.com", form))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Customer not found");

        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_ThrowException_When_CourseNotRelatedToCustomer")
    void should_ThrowException_When_CourseNotRelatedToCustomer() {
        // Given
        ReportForm form = reportFormBuilder.build();

        when(validationResources.validateCustomerExist("foo@bar.com")).thenReturn(mockCustomer);
        when(validationResources.validateCustomerWithCourse("foo@bar.com", 200L))
                .thenThrow(new RuntimeException("No relation"));

        // When/Then
        assertThatThrownBy(() -> reportService.saveReport("foo@bar.com", form))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No relation");

        verify(reportRepository, never()).save(any());
    }



    @Test
    @DisplayName("should_success_When_DetailNull")
    void should_success_When_DetailNull() {
        // Given
        ReportForm form = reportFormBuilder.detail(null).build();
        when(validationResources.validateCustomerExist(anyString())).thenReturn(mockCustomer);
        when(validationResources.validateCustomerWithCourse(anyString(), anyLong())).thenReturn(mockCourse);

        // When
        reportService.saveReport("foo@bar.com", form);

        // Then
        verify(reportRepository).save(argThat(r -> r.getDetail() == null));
    }

    @Test
    @DisplayName("should_ThrowException_When_CourseIdInvalid")
    void should_ThrowException_When_CourseIdInvalid() {
        // Given
        ReportForm form = reportFormBuilder.courseId(-1L).build();
        when(validationResources.validateCustomerExist(anyString())).thenReturn(mockCustomer);
        when(validationResources.validateCustomerWithCourse(anyString(), eq(-1L)))
                .thenThrow(new RuntimeException("Invalid course"));

        // When/Then
        assertThatThrownBy(() -> reportService.saveReport("foo@bar.com", form))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid course");

        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_ThrowException_When_EmailNull")
    void should_ThrowException_When_EmailNull() {
        // Given
        ReportForm form = reportFormBuilder.build();
        when(validationResources.validateCustomerExist(null)).thenThrow(new IllegalArgumentException("email required"));

        // When/Then
        assertThatThrownBy(() -> reportService.saveReport(null, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email required");

        verifyNoInteractions(reportRepository);
    }

    @Test
    @DisplayName("should_SaveReport_When_DetailEmpty")
    void should_SaveReport_When_DetailEmpty() {
        // Given
        ReportForm form = reportFormBuilder.detail("").build();
        when(validationResources.validateCustomerExist("foo@bar.com")).thenReturn(mockCustomer);
        when(validationResources.validateCustomerWithCourse("foo@bar.com", 200L)).thenReturn(mockCourse);

        // When
        reportService.saveReport("foo@bar.com", form);

        // Then
        verify(reportRepository).save(argThat(r -> "".equals(r.getDetail())));
    }

    @Test
    @DisplayName("should_SaveReport_When_DetailLong")
    void should_SaveReport_When_DetailLong() {
        // Given
        String longDetail =
                "A".repeat(1000);
        ReportForm form = reportFormBuilder.detail(longDetail).build();

        when(validationResources.validateCustomerExist(anyString())).thenReturn(mockCustomer);
        when(validationResources.validateCustomerWithCourse(anyString(), anyLong())).thenReturn(mockCourse);

        // When
        reportService.saveReport("foo@bar.com", form);

        // Then
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getDetail()).isEqualTo(longDetail);
    }

    @Test
    @DisplayName("should_SaveReport_When_Enum_Edge_Cases")
    void should_SaveReport_When_Enum_Edge_Cases() {
        // Given/When/Then for all ReportType enums
        for (ReportType type : ReportType.values()) {
            ReportForm form = reportFormBuilder.type(type).build();
            when(validationResources.validateCustomerExist(anyString())).thenReturn(mockCustomer);
            when(validationResources.validateCustomerWithCourse(anyString(), anyLong())).thenReturn(mockCourse);

            reportService.saveReport("foo@bar.com", form);

            verify(reportRepository).save(argThat(r -> r.getType() == type));
            reset(reportRepository); // Reset for next loop
        }
    }

    @Test
    @DisplayName("should_ThrowException_When_RepositoryFails")
    void should_ThrowException_When_RepositoryFails() {
        // Given
        ReportForm form = reportFormBuilder.build();
        when(validationResources.validateCustomerExist(anyString())).thenReturn(mockCustomer);
        when(validationResources.validateCustomerWithCourse(anyString(), anyLong())).thenReturn(mockCourse);

        doThrow(new RuntimeException("DB down")).when(reportRepository).save(any());

        // When/Then
        assertThatThrownBy(() -> reportService.saveReport("foo@bar.com", form))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB down");
    }
    @Test
    @DisplayName("should_ThrowException_When_ReportFormIsNull")
    void should_ThrowException_When_ReportFormIsNull() {
        // Given/When/Then
        assertThatThrownBy(() -> reportService.saveReport("foo@bar.com", null))
                .isInstanceOf(NullPointerException.class);

        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_Handle_Concurrency_Scenarios")
    void should_Handle_Concurrency_Scenarios() throws InterruptedException {
        // Given
        when(validationResources.validateCustomerExist("foo@bar.com")).thenReturn(mockCustomer);
        when(validationResources.validateCustomerWithCourse("foo@bar.com", 200L)).thenReturn(mockCourse);

        ReportForm concurrentForm = reportFormBuilder.build();

        Runnable reportTask = () -> reportService.saveReport("foo@bar.com", concurrentForm);
        Thread t1 = new Thread(reportTask);
        Thread t2 = new Thread(reportTask);

        // When
        t1.start(); t2.start();
        t1.join(); t2.join();

        // Then
        verify(reportRepository, atLeast(2)).save(any());
    }

    @Test
    @DisplayName("should_ThrowException_When_ReportForm_CourseIdZero")
    void should_ThrowException_When_ReportForm_CourseIdZero() {
        // Given
        ReportForm form = reportFormBuilder.courseId(0L).build();
        when(validationResources.validateCustomerExist(anyString())).thenReturn(mockCustomer);
        when(validationResources.validateCustomerWithCourse(anyString(), eq(0L))).thenThrow(new RuntimeException("Invalid courseId"));

        // When/Then
        assertThatThrownBy(() -> reportService.saveReport("foo@bar.com", form))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid courseId");

        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_ThrowException_When_EmailEmptyString")
    void should_ThrowException_When_EmailEmptyString() {
        // Given
        ReportForm form = reportFormBuilder.build();
        when(validationResources.validateCustomerExist("")).thenThrow(new IllegalArgumentException("email required"));

        // When/Then
        assertThatThrownBy(() -> reportService.saveReport("", form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email required");

        verify(reportRepository, never()).save(any());
    }
}
