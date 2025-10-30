package com.jpd.web.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.jpd.web.dto.*;
import com.jpd.web.model.*;
import com.jpd.web.model.Module;
import org.hibernate.annotations.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CustomerModuleContentRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.CourseTransForm;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CourseInfService {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private ValidationResources validationResources;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private CreatorRepository creatorRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private CustomerModuleContentRepository contentRepository;

    public List<CourseInfDto> getRecommendCourses() {
        List<Language> distinctLanguages = courseRepository.findDistinctLanguages();
        List<CourseInfDto> recommendCourses = new ArrayList<>();

        for (Language lang : distinctLanguages) {
            List<Course> courses = courseRepository.findByLanguage(lang).stream().filter(e -> e.isPublic() == true)
                    .collect(Collectors.toList());

            // Sắp xếp theo điểm đánh giá tổng hợp
            courses.sort((a, b) -> {
                RatingInfo infoA = calculateAvtRatingAndNumberStudent(a);
                RatingInfo infoB = calculateAvtRatingAndNumberStudent(b);
                double scoreA = countWeightOfCourse(infoA.avgRating(), infoA.numStudent());
                double scoreB = countWeightOfCourse(infoB.avgRating(), infoB.numStudent());
                return Double.compare(scoreB, scoreA);
            });

            // Lấy top 3 khóa học nổi bật nhất cho mỗi ngôn ngữ
            int limit = Math.min(3, courses.size());
            for (int i = 0; i < limit; i++) {
                Course c = courses.get(i);
                RatingInfo info = calculateAvtRatingAndNumberStudent(c);
                recommendCourses.add(CourseTransForm.transformToCourseInfDto(c, info.numStudent(), info.avgRating()));
            }
        }

        return recommendCourses;
    }

    private double countWeightOfCourse(double avgRating, int numberStudent) {
        double normalizedStudent = Math.log10(numberStudent + 1); // tránh chênh lệch lớn
        return 0.7 * avgRating + 0.3 * normalizedStudent;
    }

    // record để lưu tạm thông tin rating và số học viên
    private record RatingInfo(double avgRating, int numStudent) {
    }

    private RatingInfo calculateAvtRatingAndNumberStudent(Course course) {
        List<Enrollment> enrollments = enrollmentRepository.findByCourse(course);

        if (enrollments.isEmpty())
            return new RatingInfo(0, 0);

        int total = 0;
        double sumRating = 0;

        for (Enrollment e : enrollments) {
            if (e.getFeedback() != null) {
                total++;
                sumRating += e.getFeedback().getRate();
            }
        }

        double avgRating = total > 0 ? sumRating / total : 0;
        return new RatingInfo(avgRating, enrollments.size());
    }

    // tìm kiếm theo name + language+ creatorName+description
    public List<CourseInfDto> searchByKey(String searchKey) {
        if (searchKey.trim() == null)
            return null;
        List<Course> courses = this.courseRepository.searchByKey(searchKey.trim());
        List<CourseInfDto> result = new ArrayList<>();
        for (Course c : courses) {
            RatingInfo info = calculateAvtRatingAndNumberStudent(c);
            result.add(CourseTransForm.transformToCourseInfDto(c, info.numStudent(), info.avgRating()));
        }
        return result;
    }

    // tìm kiếm theo name + language+ creatorName+description and paging
    public Page<CourseSearchDto> searchAndPagination(String searchKey, Pageable pageable) {
        if (searchKey.trim() == null)
            return null;

        Page<CourseSearchDto> coursePage = courseRepository.searchAndCalculate(searchKey, pageable);

        return coursePage;
    }

    @Transactional()
    public CourseDescriptionDto getCourseDescription(long courseId) {
        log.info("Fetching course description for courseId: {}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));
        if (course.isPublic() == false)
            throw new UnauthorizedException("this course is not exist");
        return mapToCourseDescriptionDto(course);
    }

    private CourseDescriptionDto mapToCourseDescriptionDto(Course course) {
        // Statistics
        int totalStudents = courseRepository.countEnrollmentsByCourseId(course.getCourseId());
        int totalFeedbacks = courseRepository.countFeedbacksByCourseId(course.getCourseId());
        Double avgRating = courseRepository.getAverageRatingByCourseId(course.getCourseId());

        // Creator info
        Creator creator = course.getCreator();
        CreatorSimpleDto creatorDto = mapCreatorToDto(creator);

        // Chapters
        List<Chapter> chaptersDto = course.getChapters();

        // Calculate total modules
        int totalModules = course.getChapters() != null ? course.getChapters().stream()
                .mapToInt(ch -> ch.getModules() != null ? ch.getModules().size() : 0).sum() : 0;

        // Feedbacks
        List<FeedbackSimpleDto> feedbacksDto = getFeedbacksForCourse(course);

        return CourseDescriptionDto.builder().courseId(course.getCourseId()).name(course.getName())
                .description(course.getDescription()).language(course.getLanguage())
                .teachingLanguage(course.getTeachingLanguage()).price(course.getPrice()).urlImg(course.getUrlImg())
                .createdAt(course.getCreatedAt()).lastUpdate(course.getLastUpdate()).isPublic(course.isPublic())
                .isBan(course.isBan()).accessMode(course.getAccessMode() != null ? course.getAccessMode().name() : null)
                .learningObject(course.getLearningObject()).requirements(course.getRequirements())
                .targetAudience(course.getTargetAudience()).creator(creatorDto).chapters(chaptersDto)
                .totalStudents(totalStudents).totalFeedbacks(totalFeedbacks)
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0).totalModules(totalModules)
                .feedbacks(feedbacksDto).build();
    }

    private CreatorSimpleDto mapCreatorToDto(Creator creator) {
        if (creator == null)
            return null;

        int totalStudents = creatorRepository.countTotalStudentsByCreatorId(creator.getCreatorId());
        Double avgRating = creatorRepository.getAverageRatingByCreatorId(creator.getCreatorId());
        int totalCourses = creator.getCourses() != null ? creator.getCourses().size() : 0;

        return CreatorSimpleDto.builder().creatorId(creator.getCreatorId()).fullName(creator.getFullName())
                .titleSelf(creator.getTitleSelf()).imageUrl(creator.getImageUrl())
                .paymentEmail(creator.getPaymentEmail()).totalCourses(totalCourses).totalStudents(totalStudents)
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0).build();
    }

    private List<FeedbackSimpleDto> getFeedbacksForCourse(Course course) {
        List<Enrollment> enrollments = enrollmentRepository.findByCourse(course);

        return enrollments.stream().filter(e -> e.getFeedback() != null && e.getCustomer() != null).map(enrollment -> {
            Feedback feedback = enrollment.getFeedback();
            Customer customer = enrollment.getCustomer();

            return FeedbackSimpleDto.builder().feedbackId(feedback.getFeedbackId()).content(feedback.getContent())
                    .rate(feedback.getRate()).createDate(enrollment.getCreateDate().toLocalDate())
                    .customer(CustomerSimpleDto.builder().customerId(customer.getCustomerId())
                            .fullName(customer.getGivenName())

                            .build())
                    .build();
        }).collect(Collectors.toList());
    }

//	public List<CourseLearningCardDto> retrieveYourCourse(String email) {
//		Customer cus = this.customerRepository.findByEmail(email).get();
//		List<CourseLearningCardDto> res = new ArrayList<>();
//		List<Enrollment> enrs = cus.getEnrollments();
//		enrs.forEach(e -> {
//			Course ce = e.getCourse();
//			long finish = this.contentRepository.countByEnrollment(e);
//			res.add(CourseTransForm.transformToCourseLearningCardDto(ce, finish));
//		});
//		return res;
//	}



    /**
     * Lấy danh sách khóa học đang học với progress tính theo content
     */
    @Transactional
    public List<CourseLearningCardDto> retrieveYourCourse(String email) {
        log.info("Retrieving learning courses with content-based progress for email: {}", email);

        Customer customer = this.customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found with email: " + email));

        List<CourseLearningCardDto> result = new ArrayList<>();
        List<Enrollment> enrollments = customer.getEnrollments();

        if (enrollments == null || enrollments.isEmpty()) {
            log.info("No enrollments found for email: {}", email);
            return result;
        }

        // Xử lý từng enrollment
        for (Enrollment enrollment : enrollments) {
            try {
                Course course = enrollment.getCourse();

                // Tính progress dựa trên content
                ContentProgressInfo progressInfo = calculateContentProgress(course, enrollment);

                // Tính phần trăm
                double progress = progressInfo.totalContents > 0
                        ? Math.round((progressInfo.completedContents * 100.0 / progressInfo.totalContents) * 10.0) / 10.0
                        : 0.0;

                log.debug("Course '{}': {}/{} contents completed ({}%)",
                        course.getName(),
                        progressInfo.completedContents,
                        progressInfo.totalContents,
                        progress);

                CourseLearningCardDto dto = CourseTransForm.transformToCourseLearningCardDto(course);
                dto.setProgress(progress);
                result.add(dto);

            } catch (Exception e) {
                log.error("Error calculating progress for enrollment {}: {}",
                        enrollment.getEnrollId(), e.getMessage(), e);
            }
        }

        log.info("Retrieved {} courses with progress for email: {}", result.size(), email);
        return result;
    }

    /**
     * Record để lưu thông tin progress theo content
     */
    private record ContentProgressInfo(int totalContents, int completedContents) {
    }

    /**
     * Tính số content đã hoàn thành của 1 enrollment
     * <p>
     * Logic:
     * 1. Đếm tổng số UNIQUE TypeOfContent trong toàn bộ course
     * 2. Đếm số TypeOfContent user đã hoàn thành (từ tất cả CustomerModuleContent)
     * 3. Progress = (completed / total) * 100
     * <p>
     * Ví dụ:
     * - Course có: Module1{VIDEO, QUIZ}, Module2{VIDEO, FLASHCARD}, Module3{READING}
     * - Tổng unique types: 4 (VIDEO, QUIZ, FLASHCARD, READING)
     * - User đã làm: {VIDEO, QUIZ, FLASHCARD} từ các module khác nhau
     * - Completed: 3/4 = 75%
     *
     * @param course     Course cần tính
     * @param enrollment Enrollment của user
     * @return ContentProgressInfo chứa tổng và số đã hoàn thành
     */
    private ContentProgressInfo calculateContentProgress(Course course, Enrollment enrollment) {
        // 1. Đếm tổng số unique TypeOfContent trong course
        Map<TypeOfContent, Integer> totalContentsByType = countTotalContentsByType(course);
        int totalContents = totalContentsByType.values().stream().mapToInt(Integer::intValue).sum();

        if (totalContents == 0) {
            log.debug("Course '{}' has no contents", course.getName());
            return new ContentProgressInfo(0, 0);
        }

        // 2. Lấy tất cả TypeOfContent user đã hoàn thành
        Map<TypeOfContent, Integer> completedContentsByType = countCompletedContentsByType(enrollment);

        // 3. Tính số content đã hoàn thành (tối đa = số content có trong course)
        int completedContents = 0;
        for (Map.Entry<TypeOfContent, Integer> entry : completedContentsByType.entrySet()) {
            TypeOfContent type = entry.getKey();
            int completedCount = entry.getValue();
            int totalCount = totalContentsByType.getOrDefault(type, 0);

            // Lấy min để tránh TH user làm nhiều hơn số content có trong course
            int actualCompleted = Math.min(completedCount, totalCount);
            completedContents += actualCompleted;

            log.trace("Type {}: {}/{} completed", type, actualCompleted, totalCount);
        }

        log.debug("Progress calculated: {}/{} contents completed", completedContents, totalContents);

        return new ContentProgressInfo(totalContents, completedContents);
    }

    /**
     * Đếm tổng số content theo từng TypeOfContent trong course
     * <p>
     * Ví dụ return: {VIDEO=5, QUIZ=3, FLASHCARD=10, READING=2}
     * Có thể có nhiều ModuleContent cùng type
     */
    private Map<TypeOfContent, Integer> countTotalContentsByType(Course course) {
        Map<TypeOfContent, Integer> contentCount = new HashMap<>();

        if (course.getChapters() == null) {
            return contentCount;
        }

        // Duyệt qua tất cả chapters -> modules -> module contents
        for (Chapter chapter : course.getChapters()) {
            if (chapter.getModules() == null) continue;

            for (Module module : chapter.getModules()) {
                if (module.getModuleContent() == null) continue;

                for (ModuleContent content : module.getModuleContent()) {
                    TypeOfContent type = content.getTypeOfContent();
                    if (type != null) {
                        contentCount.put(type, contentCount.getOrDefault(type, 0) + 1);
                    }
                }
            }
        }

        log.debug("Total contents in course: {}", contentCount);
        return contentCount;
    }

    /**
     * Đếm số content đã hoàn thành theo từng TypeOfContent
     * <p>
     * Logic:
     * - Lấy tất cả CustomerModuleContent của enrollment
     * - Mỗi CustomerModuleContent có Set<TypeOfContent> đã hoàn thành
     * - Đếm tổng số lần xuất hiện của mỗi type
     * <p>
     * Ví dụ:
     * - Module 1: user làm {VIDEO, QUIZ}
     * - Module 2: user làm {VIDEO, FLASHCARD}
     * - Module 3: user làm {VIDEO, READING}
     * Return: {VIDEO=3, QUIZ=1, FLASHCARD=1, READING=1}
     */
    private Map<TypeOfContent, Integer> countCompletedContentsByType(Enrollment enrollment) {
        Map<TypeOfContent, Integer> completedCount = new HashMap<>();

        List<CustomerModuleContent> customerProgress = contentRepository.findByEnrollment(enrollment);

        for (CustomerModuleContent cmc : customerProgress) {
            if (cmc.getTypeOfContent() == null) continue;

            // Mỗi type trong set = 1 content đã hoàn thành trong module đó
            for (TypeOfContent type : cmc.getTypeOfContent()) {
                completedCount.put(type, completedCount.getOrDefault(type, 0) + 1);
            }
        }

        log.debug("Completed contents: {}", completedCount);
        return completedCount;
    }
}
