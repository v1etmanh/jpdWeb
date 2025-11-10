package com.jpd.web.transform;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.dto.CourseFormDto;
import com.jpd.web.dto.CourseInfDto;
import com.jpd.web.dto.CourseLearningCardDto;
import com.jpd.web.model.*;
import com.jpd.web.model.Module;

public class CourseTransForm {
	public static Course transformFromCourseFormDto(CourseFormDto courseFormDto) {
		Course c = Course.builder().name(courseFormDto.getName()).description(courseFormDto.getDescription())
				.accessMode(courseFormDto.getAccessMode()).learningObject(courseFormDto.getLearningObject())
				.targetAudience(courseFormDto.getTargetAudience()).price(0)
				.requirements(courseFormDto.getRequirements()).language(courseFormDto.getLanguage()).isBan(false)
				.isPublic(false).teachingLanguage(courseFormDto.getTeachingLanguage()).build();
		if (c.getAccessMode() == AccessMode.PAID) {
			c.setPrice(courseFormDto.getPrice());

		}
		return c;
	}

	public static CourseCardDto transformToCourseCardDto(Course course) {
		CourseCardDto c = new CourseCardDto();
		c.setCreatedDate(course.getCreatedAt());
		c.setId(course.getCourseId());
		c.setName(course.getName());
		double total = 0;
		double i = 0;
		if (course.getEnrollments() != null) {
			for (Enrollment e : course.getEnrollments()) {
				if (e.getFeedback() != null) {
					total += e.getFeedback().getRate();
					i++;
				}
			}

			double rating = total / i;
			c.setRating(rating);

			c.setStudentCount(course.getEnrollments().size());
		}
		c.setImage(course.getUrlImg());
		c.setType(course.getAccessMode());
		c.setPublic(course.isPublic());
		c.setJoinKey(course.getJoinKey());
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

	public static CourseInfDto transformToCourseInfDto(Course course, int numberS, double avtR) {
		return CourseInfDto.builder().id(course.getCourseId()).img(course.getUrlImg())
				.instructor(course.getCreator().getFullName()).name(course.getName()).numberStudent(numberS)
				.price(course.getPrice()).rating(avtR).language(course.getLanguage()).build();
	}

	public static CourseLearningCardDto transformToCourseLearningCardDto(Course course, long numerberFinishContent) {
		int total = 0;

		for (int i = 0; i < course.getChapters().size(); i++) {
			List<Chapter> chapters = course.getChapters();
			for (int j = 0; j < chapters.get(i).getModules().size(); j++) {
				Module md = chapters.get(i).getModules().get(j);
				total += md.getContentTypes().size();
			}
		}

		double progress = (double) numerberFinishContent / total * 100;
		System.out.print("numb" + numerberFinishContent + "d" + total);
		return CourseLearningCardDto.builder().course_img(course.getUrlImg()).course_name(course.getName())
				.courseId(course.getCourseId()).progress(progress).build();
	}
}
