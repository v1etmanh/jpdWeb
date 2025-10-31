package com.jpd.web.UnitTest;

import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Course;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Enrollment;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.service.EnrollmentService;
import com.jpd.web.service.utils.ValidationResources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EnrollmentCreatorServiceTest {

    @Mock
    private ValidationResources validationResources;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/data/enrollment_handlePrivateCourse.csv", numLinesToSkip = 1)
    void testHandlePrivateCourseDDT(long courseId, String email, String code, String accessMode,
                                    boolean existingEnrollment, String expectedResult, String expectedException) {
        // mock dữ liệu
        Course c = new Course();
        c.setCourseId(courseId);
        c.setAccessMode(AccessMode.valueOf(accessMode));
        c.setJoinKey("join123");

        Customer customer = new Customer();
        customer.setCustomerId(100L);
        customer.setEmail(email);

        Enrollment existing = existingEnrollment ? new Enrollment() : null;

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(c));
        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));
        when(enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(courseId, customer.getCustomerId()))
                .thenReturn(Optional.ofNullable(existing));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));

        try {
            boolean result = enrollmentService.handlePrivateCourse(courseId, code, email);
            if (!expectedResult.isEmpty()) {
                assertEquals(Boolean.parseBoolean(expectedResult), result);
            }
        } catch (Exception e) {
            if (!expectedException.isEmpty()) {
                assertEquals(expectedException, e.getClass().getSimpleName());
            } else {
                fail("Unexpected exception: " + e.getMessage());
            }
        }

        verify(courseRepository, times(1)).findById(courseId);
        verify(customerRepository, times(1)).findByEmail(email);
    }
}
