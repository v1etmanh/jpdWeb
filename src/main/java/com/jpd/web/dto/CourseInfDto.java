package com.jpd.web.dto;

import com.jpd.web.model.Language;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseInfDto {
	/* id: 5,
     name: 'Japanese Conversation Mastery',
     img: 'https://dichthuattiengnhatban.com/wp-content/uploads/2024/04/App-hc-tieng-nhat-N3-1-300x300.jpg',
     numberStudent: 4500,
     rating: 4.8,
     instructor: 'Akiko Suzuki',
     price: 699000*/
	private long id;
	private String name;
	private String img;
	private int numberStudent;
	private double rating;
	private String instructor;
	private double price;
	private Language language;
}
