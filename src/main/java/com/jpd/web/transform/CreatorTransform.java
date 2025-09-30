package com.jpd.web.transform;

import com.jpd.web.dto.CreatorDto;
import com.jpd.web.dto.CreatorProfileDto;
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
}
