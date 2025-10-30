package com.jpd.web;

import com.jpd.web.dto.EnrollmentDto;
import com.jpd.web.exception.*;
import com.jpd.web.model.*;
import com.jpd.web.repository.*;
import com.jpd.web.service.EnrollmentService;
import com.jpd.web.service.utils.ValidationResources;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EnrollmentServiceTest {

    @Mock ValidationResources validationResources;
    @Mock CourseRepository courseRepository;
    @Mock EnrollmentRepository enrollmentRepository;
    @Mock CustomerRepository customerRepository;

    @InjectMocks
    EnrollmentService enrollmentService;

    Course course;
    Customer customer;

    @BeforeEach
    void setUp() {
        // Tạo data mẫu cho test
        course = Course.builder().courseId(1L).accessMode(AccessMode.PUBLIC).joinKey("code123").build();
        customer = Customer.builder().customerId(2L).email("test@gmail.com").build();
    }

    @Test
    @DisplayName("should_ReturnEnrollments_When_CourseExistsAndOwnershipValid")
    void should_ReturnEnrollments_When_CourseExistsAndOwnershipValid() {
        List<Enrollment> mockEnrollments = Collections.singletonList(Enrollment.builder().build());

        when(validationResources.validateCourseExists(1L)).thenReturn(course);
        // validateCourseOwnership is assumed to be void, so just use 'doNothing' without assigning it
        // But since doNothing() is default for void methods in Mockito, this line can be omitted
        // doNothing().when(validationResources).validateCourseOwnership(1L, 2L);
        when(enrollmentRepository.findByCourse(course)).thenReturn(mockEnrollments);

        List<Enrollment> result = enrollmentService.findByCourseId(1L, 2L);

        assertThat(result).hasSize(1);
        verify(validationResources).validateCourseExists(1L);
        verify(validationResources).validateCourseOwnership(1L, 2L);
        verify(enrollmentRepository).findByCourse(course);
    }
    @Test
    @DisplayName("should_ThrowException_When_CourseDoesNotExist")
    void should_ThrowException_When_CourseDoesNotExist() {
        when(validationResources.validateCourseExists(1L)).thenThrow(new CourseNotFoundException(1L));

        assertThatThrownBy(() -> enrollmentService.findByCourseId(1L, 2L))
            .isInstanceOf(CourseNotFoundException.class);

        verify(validationResources).validateCourseExists(1L);
    }
    @Test
    @DisplayName("should_ThrowException_When_OwnerInvalid")
    void should_ThrowException_When_OwnerInvalid() {
        when(validationResources.validateCourseExists(1L)).thenReturn(course);
        doThrow(new RuntimeException("Ownership invalid")).when(validationResources).validateCourseOwnership(1L, 2L);

        assertThatThrownBy(() -> enrollmentService.findByCourseId(1L, 2L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Ownership invalid");

        verify(validationResources).validateCourseOwnership(1L, 2L);
    }
    @Test
    @DisplayName("should_EnrollCustomer_When_PrivateCourseAndCorrectCode")
    void should_EnrollCustomer_When_PrivateCourseAndCorrectCode() {
        course.setAccessMode(AccessMode.PRIVATE);
        course.setJoinKey("key123");

        when(customerRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(customer));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(1L, 2L)).thenReturn(Optional.empty());

        boolean result = enrollmentService.handlePrivateCourse(1L, "key123", "test@gmail.com");

        assertThat(result).isTrue();
        verify(enrollmentRepository).save(any(Enrollment.class));
    }
    @Test
    @DisplayName("should_ReturnFalse_When_PrivateCourseAndWrongKey")
    void should_ReturnFalse_When_PrivateCourseAndWrongKey() {
        course.setAccessMode(AccessMode.PRIVATE);
        course.setJoinKey("key123");

        when(customerRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(customer));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(1L, 2L)).thenReturn(Optional.empty());

        boolean result = enrollmentService.handlePrivateCourse(1L, "wrong-key", "test@gmail.com");

        assertThat(result).isFalse();
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }
    @Test
    @DisplayName("should_ThrowEnrollmentExistException_When_AlreadyEnrolled")
    void should_ThrowEnrollmentExistException_When_AlreadyEnrolled() {
        when(customerRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(customer));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(1L, 2L))
            .thenReturn(Optional.of(new Enrollment()));

        assertThatThrownBy(() -> enrollmentService.handlePrivateCourse(1L, "key123", "test@gmail.com"))
            .isInstanceOf(EnrollmentExistException.class);
    }
    @Test
    @DisplayName("should_ThrowException_When_CourseIsPaid")
    void should_ThrowException_When_CourseIsPaid() {
        course.setAccessMode(AccessMode.PAID);

        when(customerRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(customer));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.handlePrivateCourse(1L, "key123", "test@gmail.com"))
            .isInstanceOf(CourseNotFoundException.class);
    }
    @Test
    @DisplayName("should_ThrowCourseNotFoundException_When_CourseIdNotFound")
    void should_ThrowCourseNotFoundException_When_CourseIdNotFound() {
        when(customerRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(customer));
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.handlePrivateCourse(1L, "code", "test@gmail.com"))
            .isInstanceOf(CourseNotFoundException.class);
    }
    @Test
    @DisplayName("should_ThrowCourseNotFoundException_When_CustomerNotFound")
    void should_ThrowCourseNotFoundException_When_CustomerNotFound() {
        when(customerRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.handlePrivateCourse(1L, "code", "test@gmail.com"))
            .isInstanceOf(CourseNotFoundException.class);
    }
    @Test
    @DisplayName("should_EnrollCustomer_When_CourseFree")
    void should_EnrollCustomer_When_CourseFree() {
        course.setAccessMode(AccessMode.PUBLIC);

        when(customerRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(customer));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(1L, 2L)).thenReturn(Optional.empty());

        boolean result = enrollmentService.handlePrivateCourse(1L, "any", "test@gmail.com");

        assertThat(result).isTrue();
        verify(enrollmentRepository).save(any(Enrollment.class));
    }
}
