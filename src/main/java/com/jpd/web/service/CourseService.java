package com.jpd.web.service;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.dto.CourseFormDto;

import com.jpd.web.dto.PopularCourseDTO;
import com.jpd.web.exception.ApiException;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.CreatorNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.PendingImage;
import com.jpd.web.model.Status;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.repository.ChapterRepository;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.repository.ModuleContentRepository;

import com.jpd.web.service.utils.CodeGenerator;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.CourseTransForm;
import com.jpd.web.transform.CreatorTransform;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CourseService {
	@Autowired
	private FireBaseService fireBaseService;
	@Autowired
	private EnrollmentRepository enrollmentRepository;

	@Autowired
	private CourseRepository courseRepository;
	@Autowired
	private ModuleContentRepository moduleContentRepository;
	@Autowired
	private CreatorRepository creatorRepository;
	@Autowired
	private ValidationResources resourceValidator;
	@Autowired
	private CodeGenerator codeGenerator;

	private void validatePaidCourseRequirements(Creator creator, CourseFormDto courseFormDto) {
		// Check creator status
		
		if (creator.getStatus() != Status.SUCCESS) {
			log.warn("Creator {} attempted to create paid course without verified status", creator.getCreatorId());
			throw new UnauthorizedException(
					"You must verify your payment information and certificate before creating paid courses");
		}

		// Check price
		if (courseFormDto.getPrice() <= 0) {
			throw new IllegalArgumentException("Price must be greater than 0 for paid courses");
		}
	}

	private String uploadCourseImage(MultipartFile imgFile) {
		try {
			String imgUrl = fireBaseService.uploadFile(imgFile, TypeOfFile.IMG);

			if (imgUrl == null || imgUrl.trim().isEmpty()) {
				throw new FileUploadException("Failed to upload course image");
			}

			return imgUrl;
		} catch (Exception e) {
			log.error("Error uploading course image", e);
			throw new ApiException("Failed to upload course image: " + e.getMessage());
		}
	}

	@Transactional
	public Course createCourse(CourseFormDto courseFormDto, Long creatorId) {
		log.info("Creating course '{}' for creator {}", courseFormDto.getName(), creatorId);

		// Validate creator exists
		Creator creator = resourceValidator.validateCreatorExists(creatorId);

		// Transform DTO to entity
		Course course = CourseTransForm.transformFromCourseFormDto(courseFormDto);

		// Validate paid course requirements
		if (course.getAccessMode() == AccessMode.PAID) {
			validatePaidCourseRequirements(creator, courseFormDto);
		}

		// Upload course image
		String imgUrl = uploadCourseImage(courseFormDto.getImgFile());
		course.setUrlImg(imgUrl);

		// Set creator
		course.setCreator(creator);

		// Generate join key for private courses
		
			course.setJoinKey(codeGenerator.generate6DigitCode());
		
       
		Course savedCourse = courseRepository.save(course);
		Enrollment e=Enrollment.builder()
				.course(savedCourse)
				.customer(creator.getCustomer())
				.build();
		this.enrollmentRepository.save(e);
		log.info("Successfully created course {} for creator {}", savedCourse.getCourseId(), creatorId);

		return savedCourse;
	}

	public List<CourseCardDto> retrieveCourseByemail(long creatorId)  {
		Optional<Creator> c = this.creatorRepository.findById(creatorId);

		List<Course> courses = c.get().getCourses();
		return courses.stream().map(e -> CourseTransForm.transformToCourseCardDto(e)).collect(Collectors.toList());
	}

	@Transactional
	public CourseContentDto getCourseById(long courseId, long creatorId)  {

		log.info("Retrieving course {} for creator {}", courseId, creatorId);

		// Validate course exists and creator owns it
		Course course = resourceValidator.validateCourseOwnership(courseId, creatorId);

		course.getChapters().forEach(chapter -> {
			chapter.getModules();
			/*.forEach(module -> {

				List<ModuleContent> contents = this.moduleContentRepository.findByModule(module);

				module.setModuleContent(contents);
			});*/
		});
		CourseContentDto cdto = CourseTransForm.transformToCourseContentDto(course);
		return cdto;
	}
  public List<PopularCourseDTO > retrieveCCourse(long creatorId){
	  Creator creator=this.resourceValidator.validateCreatorExists(creatorId);
	  List<Course> paidCourses = creator.getCourses();
	  List<PopularCourseDTO>ppc=paidCourses.stream().map(e->CreatorTransform.transform(e)).collect(Collectors.toList());
     return ppc;
  }
  public void changeCourseSatus(long courseId, long creatorId) {
	  Course c=this.resourceValidator.validateCourseOwnership(courseId, creatorId);
	  c.setPublic(!c.isPublic());
	  courseRepository.save(c);
	  return;
  }

}
