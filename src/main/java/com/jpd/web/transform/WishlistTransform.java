package com.jpd.web.transform;

import com.jpd.web.dto.WishlistDto;
import com.jpd.web.model.Course;
import com.jpd.web.model.Wishlist;

public class WishlistTransform {
public static WishlistDto transformToWishlistDto(Wishlist wishlist)
{Course course=wishlist.getCourse();
	WishlistDto s=WishlistDto.builder()
		.course_img(course.getUrlImg())
		.course_name(course.getName())
		.courseId(course.getCourseId())
		.course_price(course.getPrice())
		.build();
return s;
	}
}
