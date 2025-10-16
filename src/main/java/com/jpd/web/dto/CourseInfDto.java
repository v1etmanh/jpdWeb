package com.jpd.web.dto;


import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseInfDto {

//    id: 2,
//    name: 'JLPT N5 Preparation',
//    img: 'https://hvcgroup.edu.vn/uploads/details/2021/04/images/hoc-tieng-nhat-co-ban.jpg',
//    numberStudent: 12500,
//    rating: 4.7,
//    instructor: 'Yuki Tanaka',
//    price: 399000

    long id;
    String name;
    String img;
    long numberOfStudent;
    double rating;
    String language;
    String instructor;
    double price;

}
