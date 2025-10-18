package com.jpd.web.service;

import com.jpd.web.dto.*;
import com.jpd.web.exception.ApiException;
import com.jpd.web.exception.CreatorAlreadyExistsException;
import com.jpd.web.exception.CustomerNotFoundException;
import com.jpd.web.model.*;
import com.jpd.web.repository.*;
import com.jpd.web.transform.CourseMapper;
import com.jpd.web.transform.CreatorTransform;
import com.jpd.web.transform.CustomerTransform;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
    private ModuleRepository moduleRepository;
    // tính theo %
    private final long completeThreshold = 80L;


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
        //tu customer id -> lay ra cac khoa hoc ma no da enroll
        Customer customer = customerRepository.findById(customerId).orElseThrow(() -> new CustomerNotFoundException(
                "Customer not found" + customerId));
        //wishlist
        List<CourseProgressDto> wishListCourses = customer.getWishlists().stream().map(wishlist ->
                CourseMapper.INSTANCE.courseToCourseProgressDto(wishlist.getCourse())).collect(Collectors.toList());

        //lấy các enrollment của customer id đó -> các course mà người đó đã tham gia
        List<Enrollment> myEnrollments = customer.getEnrollments();
        //No enrollment from customer
        if (myEnrollments == null || myEnrollments.isEmpty()) {
            return MyLearningDto.builder()
                    .wishListCourses(wishListCourses)
                    .myLearningCourse(null)
                    .myCourses(null).build();
        }
        /*
         * calculate progress in each course
         */
        //đi qua từng enrollment -> đại diện cho từng course mà customer đó đã enroll
        List<Course> enrolledCourses = myEnrollments.stream().map(Enrollment::getCourse).collect(Collectors.toList());

        //tính tổng số mục con cho mỗi hoạt động
        List<ActivityCountDto> totalActivities = moduleContentRepository.countItemsInActivitiesByCourses(enrolledCourses);

        //tính tổng số mục con đã hoàn thành cho mỗi hoạt động
        List<ActivityCountDto> completedActivities = customerModuleContentRepository.countCompletedItemsInActivities(customer, enrolledCourses);

        //chuyển List hoàn thành -> Map hoàn thành
        //Key: "moduleId:typeOfContent", Value: số mục con đã hoàn thành
        Map<String, Long> completedMap = completedActivities.stream()
                .collect(Collectors.toMap(
                        dto -> dto.getModuleId() + ":" + dto.getTypeOfContent().name(),
                        ActivityCountDto::getItemCount
                ));


        //Map lưu tổng số hoạt động của mỗi khóa học
        Map<Long, Integer> courseTotalActivities = new HashMap<>();
        //Map lưu tổng số hoạt động hoàn thành của mỗi khóa học
        Map<Long, Integer> courseCompletedActivities = new HashMap<>();


        for (ActivityCountDto item : totalActivities) {
            long courseId = item.getCourseId();
            long totalItems = item.getItemCount();
            String activityKey = item.getModuleId() + ":" + item.getTypeOfContent().name();
            //Tăng tổng số hoạt động của course lên 1
            courseTotalActivities.merge(courseId, 1, Integer::sum);

            //Lấy số mục con đã hoàn thành từ Map
            long completedItems = completedMap.getOrDefault(activityKey, 0L);

            //Kiểm tra điều kiện để được tính là đã hoàn thành 1 Activity
            if (totalItems > 0 && (completedItems * 100) / totalItems >= completeThreshold) {
                //Tăng số Activity đã hoàn thành của khóa học lên 1
                courseCompletedActivities.merge(courseId, 1, Integer::sum);
            }
        }

        List<CourseProgressDto> myLearningCourse = new ArrayList<>();
        List<CourseProgressDto> myCourses = new ArrayList<>();

        for (Course course : enrolledCourses) {
            long courseId = course.getCourseId();
            int total = courseTotalActivities.getOrDefault(courseId, 0);
            int completed = courseCompletedActivities.getOrDefault(courseId, 0);

            log.info("Total of {}: {}", courseId, total);
            log.info("Completed of {}: {}", courseId, completed);
            int progress = (total > 0) ? (completed * 100) / total : 0;

            CourseProgressDto dto = CourseMapper.INSTANCE.courseToCourseProgressDto(course);
            dto.setProgress(progress);

            if (progress > 0) {
                myLearningCourse.add(dto);
            } else {
                myCourses.add(dto);
            }
        }
        return MyLearningDto.builder()
                .myLearningCourse(myLearningCourse)
                .wishListCourses(wishListCourses)
                .myCourses(myCourses)
                .build();
    }


}
