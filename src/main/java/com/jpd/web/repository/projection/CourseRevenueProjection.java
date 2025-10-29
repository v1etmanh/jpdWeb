package com.jpd.web.repository.projection;

public interface CourseRevenueProjection {
    Long getCourseId();

    String getCourseName();

    String getImageUrl();

    Double getTotalRevenue();

    Long getEnrollmentCount();
}
