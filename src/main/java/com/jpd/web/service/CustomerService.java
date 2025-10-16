package com.jpd.web.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.jpd.web.dto.*;
import com.jpd.web.mapper.CourseMapper;
import com.jpd.web.model.*;
import com.jpd.web.repository.*;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
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
public class CustomerService {
    @Autowired
    private CustomerRepository cusRe;
    @Autowired
    private CreatorRepository creatorRe;
    @Autowired
    private FireBaseService fireBaseService;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CustomerModuleContentRepository customerModuleContentRepository;
    @Autowired
    private ModuleContentRepository moduleContentRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ModuleContentService moduleContentService;
    private final int completeThreshold = 80;


    private Customer createNewCustomer(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");

        log.info("Creating new customer account for email: {}", email);

        Customer customer = Customer.builder().email(email).username(name != null ? name : email).givenName(givenName)
                .familyName(familyName).role("USER").build();

        Customer savedCustomer = cusRe.save(customer);

        log.info("New customer created with ID: {}", savedCustomer.getCustomerId());

        return savedCustomer;
    }

    @Transactional
    public UserInfoDto getOrCreateAccount(Jwt jwt) {
        String email = jwt.getClaimAsString("email");

        log.info("Getting or creating account for email: {}", email);

        // Find or create customer
        Customer customer = cusRe.findByEmail(email).orElseGet(() -> createNewCustomer(jwt));

        // Check if customer is a creator
        boolean isCreator = creatorRe.findByCustomer(customer).isPresent();

        log.info("Account retrieved for email: {}, isCreator: {}", email, isCreator);

        return CustomerTransform.transToUserInfor(customer, isCreator);
    }

    private String uploadProfileImage(CreatorProfileDto profileDto, String email) {
        try {
            log.debug("Uploading profile image for email: {}", email);

            String imageUrl = fireBaseService.uploadFile(profileDto.getProfileImage(), TypeOfFile.IMG);

            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                throw new FileUploadException("Failed to upload profile image");
            }

            log.debug("Profile image uploaded successfully for email: {}", email);

            return imageUrl;

        } catch (IOException e) {
            log.error("Error uploading profile image for email: {}", email, e);
            throw new ApiException("Failed to upload profile image: " + e.getMessage());
        }
    }

    public CreatorDto uploadProfile(String email, CreatorProfileDto profileDto) {
        Customer customer = cusRe.findByEmail(email)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with email: " + email));
        Optional<Creator> existingCreator = creatorRe.findByCustomer(customer);
        if (existingCreator.isPresent()) {
            log.warn("Creator profile already exists for email: {}", email);
            throw new CreatorAlreadyExistsException("You already have a creator profile");
        }

        // 3. Tạo mới Creator
        Creator creator = CreatorTransform.transformFromCreatorDto(profileDto);
        creator.setCustomer(customer);
        creator.setStatus(Status.PENDING);
        // 4. Upload ảnh nếu có
        if (profileDto.getProfileImage() != null && !profileDto.getProfileImage().isEmpty()) {
            String imageUrl = uploadProfileImage(profileDto, email);
            creator.setImageUrl(imageUrl);
        }

        // 5. Gán customer cho creator

        // 6. Lưu vào database
        Creator cr1 = creatorRe.save(creator);
        return CreatorTransform.transToCreatorDto(cr1);
    }


    public MyLearningDto getLearningCourse(Long customerId) {
        List<CourseProgressDto> wishListCourses = new ArrayList<>();
        List<CourseProgressDto> myLearningCourse = new ArrayList<>();
        List<CourseProgressDto> myCourses = new ArrayList<>();
        //tu customer id -> lay ra cac khoa hoc ma no da enroll
        Customer customer = customerRepository.findById(customerId).orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
        customer.getWishlists().forEach(wishlist -> {
            //dung mapper
            wishListCourses.add(CourseMapper.INSTANCE.courseToCourseProgressDto(wishlist.getCourse()));
        });
        List<Enrollment> myEnrollments = customer.getEnrollments();
        /*
        * calculate progress in each course
        */
        //đi qua từng enrollment -> đại diện cho từng course mà customer đó đã enroll
        for (Enrollment enrollment : myEnrollments) {
            int totalOfModuleContentInCourse = courseRepository.countModuleContentByCourse(enrollment.getCourse());
            int totalOfModuleContentDone = 0;
            List<ModuleContentGroupDto> ls = customerModuleContentRepository.findGroupedByModuleContentForEnrollment(enrollment);
            for (ModuleContentGroupDto moduleContentGroupDto : ls) {
                int numberOfContentDone = (int) moduleContentGroupDto.getCount();
                int totalOfContent = moduleContentService.countContentByType(moduleContentGroupDto.getModuleContent());
                //dieu kien hoan thanh 1 module content
                if (numberOfContentDone >= completeThreshold * totalOfContent / 100 &&
                        numberOfContentDone <= totalOfContent) {
                    totalOfModuleContentDone++;
                }
            }
            int progress = 0;
            if (totalOfModuleContentDone != 0 && totalOfModuleContentInCourse != 0) {
                progress = totalOfModuleContentDone / totalOfModuleContentInCourse;
            }
            CourseProgressDto courseProgressDto =
                    CourseMapper.INSTANCE.courseToCourseProgressDto(enrollment.getCourse());
            courseProgressDto.setProgress(progress);
            if (progress != 0) {
                //TH: customer da hoc chi chi do
                myLearningCourse.add(courseProgressDto);
            } else {
                //TH: customer chua hoc cai j het
                myCourses.add(courseProgressDto);
            }
        }
        return MyLearningDto.builder()
                .myLearningCourse(myLearningCourse)
                .wishListCourses(wishListCourses)
                .myCourses(myCourses)
                .build();
    }


}
