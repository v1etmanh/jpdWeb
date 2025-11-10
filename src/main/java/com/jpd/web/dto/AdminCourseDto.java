package com.jpd.web.dto;

import java.util.List;

import com.jpd.web.model.Language;
import com.jpd.web.model.Report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCourseDto {
	private Long courseId;
	private String name;
	private String creatorName;
	private long creatorId;
	private boolean isBan;

	private double price;
	private Language language;

	private int  numberReports;
	private int numberStudent;
	
	private double avtRating;
}