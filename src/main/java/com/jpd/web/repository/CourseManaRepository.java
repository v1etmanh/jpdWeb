package com.jpd.web.repository;

import com.jpd.web.model.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CourseManaRepository extends JpaRepository<Course,Long> {

    @Modifying
    @Query("UPDATE Course c SET c.isBan = true WHERE c.courseId = :courseId")
    void banCourse(@Param("courseId") Long courseId);

    @Modifying
    @Query("UPDATE Course c SET c.isBan = false WHERE c.courseId = :courseId")
    void unbanCourse(@Param("courseId") Long courseId);

    // Đếm số lượng người đã đăng ký khóa học
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.courseId = :courseId")
    long countEnrollmentCourse(@Param("courseId") Long courseId);

}
