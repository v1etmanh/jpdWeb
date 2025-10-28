package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CourseLearningCardDto {
	/* courseId: 1,
     course_name: "Complete React Developer Course",
     course_img: "https://img-c.udemycdn.com/course/240x135/851712_fc61_6.jpg",
     progress: 65*/
	private long courseId;
	private String course_name;
	private String course_img;
	private double progress;
}
