package com.jpd.web.dto;

import java.time.LocalDate;
import java.util.List;

import com.jpd.web.model.Chapter;
import com.jpd.web.model.Language;
import com.jpd.web.model.Report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCourseDetailDto extends CourseDescriptionDto {
private List<Report>reports;
public AdminCourseDetailDto(CourseDescriptionDto course, List<Report> rs) {
    super();
    // Copy tất cả fields từ course
    this.setCourseId(course.getCourseId());
    this.setName(course.getName());
    this.setDescription(course.getDescription());
    this.setLanguage(course.getLanguage());
    this.setTeachingLanguage(course.getTeachingLanguage());
    this.setPrice(course.getPrice());
    this.setUrlImg(course.getUrlImg());
    this.setCreatedAt(course.getCreatedAt());
    this.setLastUpdate(course.getLastUpdate());
    this.setPublic(course.isPublic());
    this.setBan(course.isBan());
    this.setAccessMode(course.getAccessMode());
    this.setLearningObject(course.getLearningObject());
    this.setRequirements(course.getRequirements());
    this.setTargetAudience(course.getTargetAudience());
    this.setCreator(course.getCreator());
    this.setChapters(course.getChapters());
    this.setTotalStudents(course.getTotalStudents());
    this.setTotalFeedbacks(course.getTotalFeedbacks());
    this.setAverageRating(course.getAverageRating());
    this.setTotalModules(course.getTotalModules());
    this.setFeedbacks(course.getFeedbacks());
    
    this.reports = rs;
}
}
