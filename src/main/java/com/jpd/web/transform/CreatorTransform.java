package com.jpd.web.transform;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.jpd.web.dto.CreatorDashboardDTO;
import com.jpd.web.dto.CreatorDto;
import com.jpd.web.dto.CreatorProfileDto;
import com.jpd.web.dto.PopularCourseDTO;
import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.MonthlyCreatorBalance;

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
	creatorDto.setCertificateUrl(creator.getCertificateUrl());
	return creatorDto;
}
/* totalRevenue: 15750000,
totalStudents: 2847,//
totalCourses: 12,
avgRating: 4.8,

completionRate: 87,
newEnrollments: 156,
totalReviews: 1205*/
public static CreatorDashboardDTO transformFromCreator(Creator creator) {
    List<Course> paidCourses = creator.getCourses().stream()
            .filter(c -> c.getAccessMode() == AccessMode.PAID)
            .toList();
List<PopularCourseDTO>ppc=paidCourses.stream().limit(4).map(e->transform(e)).collect(Collectors.toList());
    int totalCourses = paidCourses.size();

    List<Enrollment> allEnrollments = paidCourses.stream()
            .flatMap(c -> c.getEnrollments().stream())
            .toList();

    int totalStudents = allEnrollments.size();
    int completed = (int) allEnrollments.stream().filter(Enrollment::isFinish).count();
    int newEnrollments = (int) allEnrollments.stream()
            .filter(e -> e.getCreateDate().isAfter(LocalDateTime.now().minusMonths(3)))
            .count();

    List<Integer> ratings = allEnrollments.stream()
            .filter(e -> e.getFeedback() != null)
            .map(e -> e.getFeedback().getRate())
            .toList();

    double avgRating = ratings.isEmpty()
            ? 0
            : ratings.stream().mapToInt(Integer::intValue).average().orElse(0);

    double completionRate = totalStudents == 0 ? 0 : (double) completed / totalStudents * 100;

    int totalReviews = ratings.size();

    return CreatorDashboardDTO.builder()
            .totalRevenue(creator.getBalance())
            .totalStudents(totalStudents)
            .totalCourses(totalCourses)
            .avgRating(avgRating)
            .completionRate(completionRate)
            .newEnrollments(newEnrollments)
            .totalReviews(totalReviews)
            .ppc(ppc)
            .build();
}
public static PopularCourseDTO transform(Course course) {
    if (course == null) {
        return null;
    }

    long students = 0;
    double totalRating = 0;
    int totalReviews = 0;

    if (course.getEnrollments() != null && !course.getEnrollments().isEmpty()) {
        students = course.getEnrollments().size();

        for (Enrollment e : course.getEnrollments()) {
            if (e.getFeedback() != null) {
                totalRating += e.getFeedback().getRate();
                totalReviews++;
            }
        }
    }

    double avgRating = totalReviews == 0 ? 0 : totalRating / totalReviews;
    double revenue = course.getPrice() * students;

    return PopularCourseDTO.builder()
            .courseId(course.getCourseId())
            .title(course.getName())
            .students(students)
            .revenue(revenue)
            .rating(avgRating)
            .urlImg(course.getUrlImg())
            .price(course.getPrice())
            .build();
}
public static CreatorDashboardDTO transformFromMonthlyBalance(MonthlyCreatorBalance balance,List<PopularCourseDTO> popularCourseDTOs) {
    if (balance == null) {
        return null;
    }

    // Lấy thông tin popular courses


    return CreatorDashboardDTO.builder()
        .totalRevenue(balance.getTotalRevenue())
        .totalStudents(balance.getTotalStudents())
        .totalCourses(balance.getTotalCourses())
        .avgRating(balance.getAvgRating())
        .completionRate(balance.getCompletionRate())
        .newEnrollments(balance.getNewEnrollments())
        .totalReviews(balance.getTotalReviews())
        .ppc(popularCourseDTOs)
        .build();
}
}
