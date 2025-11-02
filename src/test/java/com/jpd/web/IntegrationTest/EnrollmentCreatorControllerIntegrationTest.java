package com.jpd.web.IntegrationTest;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;

import com.jpd.web.controller.common.GlobalExceptionHandler;
import com.jpd.web.controller.creator.EnrollmentCreatorController;
import com.jpd.web.model.Course;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Enrollment;
import com.jpd.web.service.EnrollmentService;
import com.jpd.web.service.utils.RequestAttributeExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EnrollmentCreatorController.class)
@ContextConfiguration(classes = {EnrollmentCreatorController.class, GlobalExceptionHandler.class})
class EnrollmentCreatorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnrollmentService enrollmentService;


    @Test
    @DisplayName("GET /{courseId} - retrieve enrollments for a course")
    @WithMockUser(username = "creator1", roles = {"CREATOR"})
    void retrieveByCourse_Success() throws Exception {
        // Prepare test data
        Course course = new Course();
        course.setCourseId(1L);

        Customer customer = new Customer();
        customer.setCustomerId(1L);

        Enrollment enrollment = Enrollment.builder()
                .enrollId(100L)
                .course(course)
                .customer(customer)
                .isFinish(false)
                .build();

        // Mock static method
        try (MockedStatic<RequestAttributeExtractor> mocked = mockStatic(RequestAttributeExtractor.class)) {
            mocked.when(() -> RequestAttributeExtractor.extractCreatorId(any()))
                    .thenReturn(10L);

            // Mock service method
            when(enrollmentService.findByCourseId(1L, 10L))
                    .thenReturn(Collections.singletonList(enrollment));

            // Perform request and verify
            mockMvc.perform(get("/api/creator/enrollment/1"))
                    .andDo(print()) // in ra JSON response
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].finish").value(false));

        }
    }
}
