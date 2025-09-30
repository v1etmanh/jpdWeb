package com.jpd.web.transform;

import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.dto.CourseFormDto;
import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;

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
}
