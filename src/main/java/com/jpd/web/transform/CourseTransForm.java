package com.jpd.web.transform;

import java.util.List;

import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.dto.CourseFormDto;
import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.ModuleContent;

public class CourseTransForm {
public static Course transformFromCourseFormDto(CourseFormDto courseFormDto) {
	Course c= Course.builder()
			.name(courseFormDto.getName())
			.description(courseFormDto.getDescription())
			.accessMode(courseFormDto.getAccessMode())
			.learningObject(courseFormDto.getLearningObject())
			.targetAudience(courseFormDto.getTargetAudience())
			.price(0)
			.requirements(courseFormDto.getRequirements()) // ❌ Thiếu field này
	        .language(courseFormDto.getLanguage()) 
			.isBan(false)
			.isPublic(false)
			.build();
	if(c.getAccessMode()==AccessMode.PAID) {
		c.setPrice(courseFormDto.getPrice());
		
	}
	return c;
}
public static CourseCardDto transformToCourseCardDto(Course course) {
	CourseCardDto c=new CourseCardDto();
	c.setCreatedDate(course.getCreatedAt());
	c.setId(course.getCourseId());
	c.setName(course.getName());
	double total=0;
	double i=0;
    for(Enrollment e:course.getEnrollments()) {
    	if(e.getFeedback()!=null) {
    	total+=e.getFeedback().getRate();
    	i++;
    	}
    }
    double rating=total/i;
		c.setRating(rating);
	
	c.setStudentCount(course.getEnrollments().size());
   c.setImage(course.getUrlImg());
   c.setType(course.getAccessMode());
	return c;
}
public static CourseContentDto transformToCourseContentDto(Course course) {
    CourseContentDto contentDto = new CourseContentDto();
    contentDto.setName(course.getName());
    contentDto.setPublic(course.isPublic());
    
    // Force load chapters và nested data
    List<Chapter> chapters = course.getChapters();
    if (chapters != null) {
        chapters.forEach(chapter -> {
            // Force load modules
            List<com.jpd.web.model.Module> modules = chapter.getModules();
            if (modules != null) {
                modules.forEach(module -> {
                    // Force load module contents - ĐÂY LÀ QUAN TRỌNG!
                    List<ModuleContent> contents = module.getModuleContent();
                    if (contents != null) {
                        contents.size(); // Trigger lazy loading
                    }
                });
            }
        });
    }
    
    contentDto.setChapters(chapters);
    return contentDto;
}
}
