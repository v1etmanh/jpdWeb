package com.jpd.web.repository;

import com.jpd.web.dto.ActivityCountDto;
import com.jpd.web.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface CustomerModuleContentRepository extends JpaRepository<CustomerModuleContent, Long> {


    List<CustomerModuleContent> findByEnrollment(Enrollment enrollment);


    @Query("SELECT e.course.courseId, COUNT(cmc.cqId) " +
            "FROM CustomerModuleContent cmc " +
            "JOIN cmc.enrollment e " +
            "WHERE e.customer = :customer AND e.course IN :courses " +
            "GROUP BY e.course.courseId")
    Map<Long, Long> countCompletedModuleContentByCustomerAndCourses(
            @Param("customer") Customer customer,
            @Param("courses") List<Course> courses
    );


    /**
     * Đếm số mục con đã hoàn thành bởi Customer cho mỗi "Hoạt động học tập".
     * Áp dụng cho một danh sách các khóa học.
     */
    @Query("SELECT new com.jpd.web.dto.ActivityCountDto(c.courseId, m.moduleId, mc.typeOfContent, COUNT(cmc.cqId)) " +
            "FROM CustomerModuleContent cmc " +
            "JOIN cmc.moduleContent mc " +
            "JOIN mc.module m " +
            "JOIN m.chapter ch " +
            "JOIN ch.course c " +
            "WHERE cmc.enrollment.customer = :customer AND c IN :courses " +
            "GROUP BY c.courseId, m.moduleId, mc.typeOfContent")
    List<ActivityCountDto> countCompletedItemsInActivities(
            @Param("customer") Customer customer,
            @Param("courses") List<Course> courses
    );


}
