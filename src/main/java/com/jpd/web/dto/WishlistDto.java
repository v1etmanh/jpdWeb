package com.jpd.web.dto;




import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@RequiredArgsConstructor
public class WishlistDto {
private long courseId;
private String course_name;
private String course_img;
private double course_price;
}
