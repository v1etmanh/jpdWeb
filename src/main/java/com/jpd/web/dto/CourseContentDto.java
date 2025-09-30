package com.jpd.web.dto;

import java.util.List;

import com.jpd.web.model.Chapter;

public class CourseContentDto {
/* course name 
 * 
 * list chapterdto
 * name 
 * ispublic
 * List<Chapter>
 * */
	private String name;
	private boolean isPublic;
	private List<Chapter>chapters;
}
