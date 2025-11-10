package com.jpd.web.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.annotations.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.CourseDescriptionDto;
import com.jpd.web.dto.CourseInfDto;
import com.jpd.web.dto.CourseLearningCardDto;
import com.jpd.web.dto.CreatorSimpleDto;
import com.jpd.web.dto.CustomerSimpleDto;
import com.jpd.web.dto.FeedbackSimpleDto;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;
import com.jpd.web.model.Language;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CustomerModuleContentRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.CourseTransForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
	public Page<CourseInfDto> searchByKey(String searchKey, int page, int size) {
	   

	    Pageable pageable = PageRequest.of(page, size); // ❌ Không sort nữa
       
	    Page<Course> coursesPage = this.courseRepository.searchByKey(searchKey.trim(), pageable);

	    List<CourseInfDto> dtoList = coursesPage.getContent().stream()
	        .map(course -> {
	            RatingInfo info = calculateAvtRatingAndNumberStudent(course);
	            return CourseTransForm.transformToCourseInfDto(course, info.numStudent(), info.avgRating());
	        })
	        .collect(Collectors.toList());

	    return new PageImpl<>(dtoList, pageable, coursesPage.getTotalElements());
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

	public CourseDescriptionDto mapToCourseDescriptionDto(Course course) {
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

	public List<CourseLearningCardDto> retrieveYourCourse(String email) {
		Customer cus = this.customerRepository.findByEmail(email).get();
		List<CourseLearningCardDto> res = new ArrayList<>();
		List<Enrollment> enrs = cus.getEnrollments();
		enrs.forEach(e -> {

			Course ce = e.getCourse();
			long finish = e.getCustomerModuleContents().size();
			res.add(CourseTransForm.transformToCourseLearningCardDto(ce, finish));
		});
		return res;
	}
}
