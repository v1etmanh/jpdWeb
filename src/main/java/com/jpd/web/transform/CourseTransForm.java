package com.jpd.web.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.dto.CourseFormDto;
import com.jpd.web.dto.Response.CourseDetailResponse;
import com.jpd.web.dto.Response.CreatorDtoResponse;
import com.jpd.web.dto.Response.FeedbackDtoResponse;
import com.jpd.web.dto.CourseInfDto;
import com.jpd.web.dto.CourseLearningCardDto;
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
			.requirements(courseFormDto.getRequirements())
	        .language(courseFormDto.getLanguage()) 
			.isBan(false)
			.isPublic(false)
			.teachingLanguage(courseFormDto.getTeachingLanguage())
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
	if(course.getEnrollments()!=null) {
    for(Enrollment e:course.getEnrollments()) {
    	if(e.getFeedback()!=null) {
    	total+=e.getFeedback().getRate();
    	i++;
    	}
    }
	
    double rating=total/i;
		c.setRating(rating);
	
	c.setStudentCount(course.getEnrollments().size());
	}
   c.setImage(course.getUrlImg());
   c.setType(course.getAccessMode());
   c.setPublic(course.isPublic());
	return c;
}
public static CourseContentDto transformToCourseContentDto(Course course) {
    CourseContentDto contentDto = new CourseContentDto();
    contentDto.setName(course.getName());
    contentDto.setPublic(course.isPublic());
    contentDto.setLanguage(course.getLanguage());
    contentDto.setTeachingLanguage(course.getTeachingLanguage());
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
    public static CourseDetailResponse courseToCourseDetailResponse(Course course) {
        if (course == null) return null;

        // 1) numberstudent = tổng enrollments (không có status)
        int numberStudent = course.getEnrollments() == null ? 0 : course.getEnrollments().size();

        // 2) totalLectures = tổng số module của tất cả chapter
        int totalLectures = 0;
        if (course.getChapters() != null) {
            for (Chapter ch : course.getChapters()) {
                if (ch != null && ch.getModules() != null) {
                    totalLectures += ch.getModules().size();
                }
            }
        }

        // 3) curriculum = danh sách ChapterDtoResponse { section = chapter.name, lectures = chapter.module.size }
        List<Chapter> curriculum =
                course.getChapters() == null ? Collections.emptyList() : course.getChapters();

        // 4) creator: map các trường cơ bản; các số liệu tổng hợp để 0 theo yêu cầu
        CreatorDtoResponse creatorDto = null;
        if (course.getCreator() != null) {
          creatorDto=  CreatorTransform.transToCreatorDtoResponse(course.getCreator());
        }

        // 5) feedback & rating: mặc định theo yêu cầu (feedback nằm trong enrollments)
        List<FeedbackDtoResponse> feedback = new ArrayList<>();
        for (Enrollment e : course.getEnrollments()) {
            feedback.add(FeedbackTransForm.transformFeedbackDtoResponse(e));
        }
       double totalRatings= feedback.isEmpty() ? 0 : feedback.size();
        double rating = 0.0;
        for (FeedbackDtoResponse f : feedback) {
            rating+=  f.getRating() /totalRatings;
        }


        // 6) giá/khuyến mãi: chưa có cơ chế giảm giá
        double price = course.getPrice();
        double originalPrice = price;
        double discount = 0.0;

        return CourseDetailResponse.builder()
                .name(course.getName())
                .description(course.getDescription())
                .language(course.getLanguage())
                .img(course.getUrlImg())
                .price(price)
                .originalPrice(originalPrice)
                .discount(discount)
                .lastUpdated(course.getLastUpdate())
                .learningOutcomes(course.getLearningObject())
                .requirements(course.getRequirements())
                .targetAudience(course.getTargetAudience())
                .numberstudent(numberStudent)
                .totalLectures(totalLectures)
                .curriculum(curriculum)
                .creator(creatorDto)
                .feedback(feedback)
                .rating(rating)
                .totalRatings((int)totalRatings)
                .build();
    }
public static CourseInfDto transformToCourseInfDto(Course course, int numberS, double avtR) {
	return CourseInfDto.builder()
			.id(course.getCourseId())
			.img(course.getUrlImg())
			.instructor(course.getCreator().getFullName())
			.name(course.getName())
			.numberStudent(numberS)
			.price(course.getPrice())
			.rating(avtR)
			.language(course.getLanguage())
			.build();
}
public static CourseLearningCardDto transformToCourseLearningCardDto(Course course,long numerberFinishContent) {
	 AtomicInteger total = new AtomicInteger(0);

	    course.getChapters().forEach(c -> {
	        c.getModules().forEach(m -> {
	            total.addAndGet(m.getContentTypes().size());
	        });
	    });

	    double progress = total.get() == 0 ? 0 : (double) numerberFinishContent / total.get();
return CourseLearningCardDto.builder()
.course_img(course.getUrlImg())
.course_name(course.getName())
.courseId(course.getCourseId())
.progress(progress)
.build();
}
}
