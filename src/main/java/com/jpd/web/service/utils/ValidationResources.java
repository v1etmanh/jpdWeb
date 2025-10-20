package com.jpd.web.service.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jpd.web.exception.ChapterNotFoundException;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.CreatorNotFoundException;
import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.repository.ChapterRepository;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
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
	            throw new IllegalArgumentException("Chapter does not belong to this course");
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
	            throw new IllegalArgumentException("Module does not belong to this chapter");
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
	 
}
