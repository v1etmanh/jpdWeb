package com.jpd.web.dto;

import java.sql.Date;
import java.util.List;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@RequiredArgsConstructor
public class LearningListDto {
private List<CourseLearningCardDto>cardDtos;
private List<WishlistDto>wishlistDtos;
}
