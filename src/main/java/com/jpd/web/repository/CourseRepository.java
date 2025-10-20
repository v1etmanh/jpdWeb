package com.jpd.web.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.jpd.web.model.Course;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository  extends CrudRepository<Course, Long> , JpaSpecificationExecutor<Course> {

    @Query("select c from Course c where " +
            "c.courseId=:courseId and c.isPublic = true and c.isBan = false ")
    Optional<Course> findById(@Param("courseId") long courseId);
    @Query("select c from Course c where c.isPublic = true and c.isBan = false")
    List<Course> findAll();
}
