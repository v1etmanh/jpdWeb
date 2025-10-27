package com.jpd.web.dto;

import java.time.LocalDate;

import com.jpd.web.model.AccessMode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Data
public class CourseCardDto {
/*
 * id: 1,
      name: "Public Course A",
      image: "https://via.placeholder.com/150",
      createdDate: "2024-09-20",
      studentCount: 120,
      reviewCount: 40,
      rating: 4.5,*/
	private long id ;
	private String name;
	private LocalDate createdDate;
	private int studentCount;
	private double reviewCount;
	private double totalRevenue;
	private double rating;
	private String image;
	private AccessMode type;
	private boolean isPublic;
	private String joinKey;
}
