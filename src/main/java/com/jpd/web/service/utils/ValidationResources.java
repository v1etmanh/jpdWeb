package com.jpd.web.service.utils;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jpd.web.exception.ChapterNotBelongsToCourseException;
import com.jpd.web.exception.ChapterNotFoundException;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.CreatorNotFoundException;
import com.jpd.web.exception.CustomerNotFoundException;
import com.jpd.web.exception.KahootNotFoundException;
import com.jpd.web.exception.ModuleNotBelongsToChapterException;
import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.KahootListFunction;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.repository.ChapterRepository;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.repository.KahootRepository;
import com.jpd.web.repository.ModuleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationResources {
	   private final CreatorRepository creatorRepository;
	    private final CourseRepository courseRepository;
	    private final ChapterRepository chapterRepository;
	    private final  ModuleRepository moduleRepository;
	    private final EnrollmentRepository enrollmentRepository;
	    private final CustomerRepository customerRepository;
	    private final KahootRepository kahootRepository;
	    public  KahootListFunction validateKahootOwnership(Long kahootId, Long creatorId) {
	        log.debug("Validating course {} ownership for creator {}", kahootId, creatorId);

	        // Validate creator exists
	        Creator creator = creatorRepository.findById(creatorId)
	                .orElseThrow(() -> new CreatorNotFoundException(creatorId));

	        // Validate course exists
	        KahootListFunction kh = kahootRepository.findById(kahootId)
	                .orElseThrow(() -> new KahootNotFoundException(kahootId));

	        // Validate ownership
	        if (kh.getCreator().getCreatorId()!=(creatorId)) {
	            log.warn("Creator {} attempted to access course {} owned by creator {}",
	                    creatorId,kahootId);
	            throw new UnauthorizedException("You don't have permission to modify this course");
	        }

	        log.debug("Course {} ownership validated successfully for creator {}", kahootId, creatorId);
	        return kh;
	    }
	public  Course validateCourseOwnership(Long courseId, Long creatorId) {
        log.debug("Validating course {} ownership for creator {}", courseId, creatorId);
        
        // Validate creator exists
        Creator creator = creatorRepository.findById(creatorId)
                .orElseThrow(() -> new CreatorNotFoundException(creatorId));
        
        // Validate course exists
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        
        // Validate ownership
        if (course.getCreator().getCreatorId()!=(creatorId)) {
            log.warn("Creator {} attempted to access course {} owned by creator {}", 
                    creatorId, courseId, course.getCreator().getCreatorId());
            throw new UnauthorizedException("You don't have permission to modify this course");
        }
        
        log.debug("Course {} ownership validated successfully for creator {}", courseId, creatorId);
        return course;
    }
	  public Chapter validateChapterBelongsToCourse(Long chapterId, Long courseId) {
	        log.debug("Validating chapter {} belongs to course {}", chapterId, courseId);
	        
	        Chapter chapter = chapterRepository.findById(chapterId)
	                .orElseThrow(() -> new ChapterNotFoundException(chapterId));
	        
	        if (chapter.getCourse().getCourseId()!=(courseId)) {
	            log.warn("Chapter {} does not belong to course {}", chapterId, courseId);
	            throw new ChapterNotBelongsToCourseException(chapterId,courseId);
	        }
	        
	        log.debug("Chapter {} validated successfully for course {}", chapterId, courseId);
	        return chapter;
	    }
	  public Course validateCourseExists(Long courseId) {
	        return courseRepository.findById(courseId)
	                .orElseThrow(() -> new CourseNotFoundException(courseId));
	    }
	  public Chapter validateChapterExists(Long chapterId) {
	        return chapterRepository.findById(chapterId)
	                .orElseThrow(() -> new ChapterNotFoundException(chapterId));
	    }
	  public Creator validateCreatorExists(Long creatorId) {
	        return creatorRepository.findById(creatorId)
	                .orElseThrow(() -> new CreatorNotFoundException(creatorId));
	    }
	  public com.jpd.web.model.Module validateModuleBelongsToChapter(Long moduleId, Long chapterId) {
	        log.debug("Validating module {} belongs to chapter {}", moduleId, chapterId);
	        
	        com.jpd.web.model.Module module = moduleRepository.findById(moduleId)
	                .orElseThrow(() -> new ModuleNotFoundException(moduleId));
	        
	        if (module.getChapter().getChapterId()!=(chapterId)) {
	            log.warn("Module {} does not belong to chapter {}", moduleId, chapterId);
	            throw new ModuleNotBelongsToChapterException(moduleId, chapterId);
	        }
	        
	        log.debug("Module {} validated successfully for chapter {}", moduleId, chapterId);
	        return module;
	    }
	  public com.jpd.web.model.Module validateCompleteOwnership(Long moduleId, Long chapterId, Long courseId, Long creatorId) {
		    log.debug("Validating complete ownership chain for module {}", moduleId);
		    
		    // Validate course ownership (includes creator & course validation)
		    Course course = validateCourseOwnership(courseId, creatorId);
		    
		    // Validate chapter belongs to course
		    Chapter chapter = validateChapterBelongsToCourse(chapterId, courseId);
		    
		    // Validate module belongs to chapter
		    com.jpd.web.model.Module module = validateModuleBelongsToChapter(moduleId, chapterId);
		    
		    log.debug("Complete ownership chain validated successfully");
		    return module;
		}
	  public Customer validateCustomerExist(String email) {
		  Optional<Customer>customer=this.customerRepository.findByEmail(email);
		  if(customer.isEmpty())throw new CustomerNotFoundException(email);
		  return customer.get();
	  }
	  public Course validateCustomerWithCourse(String email, long courseId) {
		    Course course = validateCourseExists(courseId);
		    Customer customer = validateCustomerExist(email);
		    Creator creator = customer.getCreator();
		    
		    // Check nếu là Creator của course
		    if (creator != null && course.getCreator().getCreatorId() == creator.getCreatorId()) {
		        return course;
		    }
		    
		    // Check enrollment cho student
		    Optional<Enrollment> enr = enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(
		        courseId, customer.getCustomerId()
		    );
		    
		    if (enr.isEmpty()) {
		        throw new UnauthorizedException("You must enroll before learning");
		    }
		    
		    return course;
		}
	 public Enrollment validateCustomerWithCourseGetE(String email , long courseId) {
		 Course course=validateCourseExists(courseId);
		 
		 Customer customer=validateCustomerExist(email);
		Creator c1=customer.getCreator();
		if(c1!=null) 
		{
			 if(course.getCreator().getCreatorId()==c1.getCreatorId())
				 return null;
		}
		Optional< Enrollment> enr=this.enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(courseId, customer.getCustomerId());
		 if(enr.isEmpty())throw new UnauthorizedException("you must enroll after learning");
		 return enr.get();
	 }
	 public com.jpd.web.model.Module validateModuleContentOwnerShip(
			    Long moduleId, Long chapterId, Long courseId, String email) {
			    
			    log.debug("Validating module content ownership for module {}", moduleId);
			    
			    Course course = validateCourseExists(courseId);
			    Customer customer = validateCustomerExist(email);
			    Creator creator = customer.getCreator();
			    
			    // Check nếu KHÔNG phải Creator hoặc KHÔNG phải Creator của course này
			    if (creator == null || course.getCreator().getCreatorId() != creator.getCreatorId()) {
			        // Validate enrollment cho student
			        course = validateCustomerWithCourse(email, courseId);
			    }
			    
			    // Validate chapter và module
			    Chapter chapter = validateChapterBelongsToCourse(chapterId, courseId);
			    com.jpd.web.model.Module module = validateModuleBelongsToChapter(moduleId, chapterId);
			    
			    log.debug("Module content ownership validated successfully");
			    return module;
			}
}
