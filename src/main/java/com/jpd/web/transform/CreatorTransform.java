package com.jpd.web.transform;

import com.jpd.web.dto.CreatorDto;
import com.jpd.web.dto.CreatorProfileDto;
import com.jpd.web.dto.Response.CreatorDtoResponse;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;

public class CreatorTransform {
public static Creator transformFromCreatorDto(CreatorProfileDto creatorProfileDto) {
	Creator creator=Creator.builder().fullName(creatorProfileDto.getFullName())
			.titleSelf(creatorProfileDto.getBio())
			.mobiPhone(creatorProfileDto.getPhone())
			.build();
	return creator;
}
public static CreatorDto transToCreatorDto(Creator creator) {
	CreatorDto creatorDto=new CreatorDto();
	creatorDto.setBio(creator.getTitleSelf());
	creatorDto.setFullName(creator.getFullName());
	creatorDto.setImgUrl(creator.getImageUrl());
	creatorDto.setPaypalEmail(creator.getPaymentEmail());
	creatorDto.setPhone(creator.getMobiPhone());
	creatorDto.setStatus(creator.getStatus());
	
	return creatorDto;
}
public  static CreatorDtoResponse transToCreatorDtoResponse(Creator creator) {
    if(creator == null) return null;

 CreatorDtoResponse.CreatorDtoResponseBuilder builder=CreatorDtoResponse.builder();
 builder.name(creator.getFullName());
 builder.email(creator.getCustomer().getEmail());
 builder.avatar(creator.getImageUrl());
 builder.description(creator.getTitleSelf());
 builder.totalCourse(creator.getCourses().size());
 builder.totalStudents(countTotalStudent(creator));
 return builder.build();
}
private static int countTotalStudent(Creator creator) {
    int totalStudent =0;
    for (Course course : creator.getCourses()) {
        totalStudent+=course.getEnrollments().size();
    }
    return totalStudent;
}
}
