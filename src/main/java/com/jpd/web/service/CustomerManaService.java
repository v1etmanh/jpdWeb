package com.jpd.web.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.api.Page;
import com.google.api.gax.rpc.NotFoundException;
import com.jpd.web.dto.*;
import com.jpd.web.model.*;
import com.jpd.web.repository.*;
import jakarta.transaction.Transaction;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.jpd.web.exception.ApiException;
import com.jpd.web.exception.CreatorAlreadyExistsException;
import com.jpd.web.exception.CustomerNotFoundException;
import com.jpd.web.transform.CreatorTransform;
import com.jpd.web.transform.CustomerTransform;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CustomerManaService {
    @Autowired
    private CustomerRepository cusRe;
    @Autowired
    private CreatorRepository creatorRe;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private FeedbackRepository  feedbackRepository;
    @Autowired
    private CustomerTransactionRepository customerTransactionRepository;
    @Autowired
    private CustomerManaRepository customerManaRepository;

    //tìm kiếm customer = email
    public CustomerSearchDto getCustomerByEmail(String email) {
        log.info("Getting customer by email: {}", email);
        Customer customer = cusRe.findByEmail(email)
                .orElseThrow(() -> new CustomerNotFoundException("Email not found: " + email));

        return new CustomerSearchDto(
                customer.getCustomerId(),
                customer.getEmail(),
                customer.getUsername(),
                customer.getCreateDate()
        );
    }

    // get customer profile
    public UserInfoDto getCustomerProfile(Long customerId) {
        log.info("Getting customer profile for ID: {}", customerId);
        Customer customer = cusRe.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        //check neu user đó là creator hay k
        boolean isCreator = creatorRe.findByCustomer(customer).isPresent();

        return CustomerTransform.transToUserInfor(customer, isCreator);

    }
    //cập nhật customer profile
    @Transactional
    public UserInfoDto updateCustomerProfile(Long customerId, UserInfoDto userInfoDto) {
        log.info("Updating customer profile for ID: {}", customerId);
        Customer customer = cusRe.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        customer.setUsername(userInfoDto.getUserName());
        customer.setGivenName(userInfoDto.getGivenName());
        customer.setFamilyName(userInfoDto.getFamilyName());

        cusRe.save(customer);

        boolean isCreator = creatorRe.findByCustomer(customer).isPresent();

        return CustomerTransform.transToUserInfor(customer, isCreator);
    }


    public List<Customer> getAllCustomers() {
        log.info("Getting all customers");
        return customerManaRepository.findAll();
    }

    public Optional<Customer> getCustomerById(Long customerId) {
        log.info("Getting customer by ID: {}", customerId);
        return customerManaRepository.findById(customerId);
    }

    // lấy thông tin cần thiết của khóa học
    public List<CourseBriefDto> getEnrolledCourses(Long customerId) {
        log.info("Getting enrolled courses for ID: {}", customerId);
        Customer customer = cusRe.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        List<Enrollment> enrollments = customer.getEnrollments();

        // Trả về list rỗng nếu chưa đăng ký gì
        if (enrollments == null || enrollments.isEmpty()) {
            return Collections.emptyList();
        }

        return enrollments.stream()
                .map(enrollment -> {
                    Course course = enrollment.getCourse();

                    // Lấy feedback từ enrollment
                    double avgRating = 0.0;
                    Feedback feedback = enrollment.getFeedback();
                    if (feedback != null) {
                        avgRating = feedback.getRate();
                    }

                    return new CourseBriefDto(
                            course.getCourseId(),
                            course.getName(),
                            course.getUrlImg(),
                            course.getCreator() != null
                                    ? course.getCreator().getCustomer().getUsername()
                                    : "Unknown Instructor",
                            course.getPrice(),
                            enrollment.getCreateDate(),
                            avgRating
                    );
                })
                .collect(Collectors.toList());
    }


    // Lịch sử đăng ký khóa học của 1 customer
    public List<Enrollment> getEnrollmentHistory(Long customerId) {
        log.info("Getting enrollment history for ID: {}", customerId);
        return enrollmentRepository.findByCustomer_CustomerId(customerId);
    }

    // Lịch sử tất cả customer đã đăng ký 1 khóa học
    public List<Enrollment> getCourseEnrollmentHistory(Long courseId) {
        log.info("Getting all customers enrolled in course ID: {}", courseId);
        return enrollmentRepository.findByCourse_CourseId(courseId);
    }

    // khóa học đề xuất
    public List <CourseBriefDto> getRecommendedCourses() {
        log.info("Getting top 3 recommended courses (highest average feedback score");
        List<Object[]>results=feedbackRepository.getCoursesOrderByAverageRatingDesc(PageRequest.of(0, 3));
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }
        return results.stream().map(row -> {
            Course course = (Course) row[0];
            Double avgRating = (Double) row[1];

            return new CourseBriefDto(
                    course.getCourseId(),
                    course.getName(),
                    course.getUrlImg(),
                    course.getCreator() != null
                            ? course.getCreator().getCustomer().getUsername()
                            : "Unknown Instructor",
                    course.getPrice(),
                    null,
                    avgRating
            );
        }).collect(Collectors.toList());
    }


}

