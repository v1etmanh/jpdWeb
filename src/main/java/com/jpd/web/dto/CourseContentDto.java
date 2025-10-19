package com.jpd.web.dto;

import java.util.List;

import com.jpd.web.model.Chapter;
import com.jpd.web.model.Language;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
@AllArgsConstructor
@RequiredArgsConstructor
@Data
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
	private Language language;
	private Language teachingLanguage;
	private List<Chapter>chapters;
}
