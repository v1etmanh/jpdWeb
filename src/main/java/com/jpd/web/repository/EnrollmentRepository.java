package com.jpd.web.repository;

<<<<<<< HEAD
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long>{
 List<Enrollment> findByCourse(Course course);
 Optional<Enrollment> findByCourse_CourseIdAndCustomer_CustomerId(long courseId, long customerId);
=======
import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    //lấy list enrollment của 1 customer_id
    List<Enrollment> findByCustomer_CustomerId(Long customerId);

    // lấy list customer của 1 course_id
    List<Enrollment> findByCourse_CourseId(Long courseId);

    List<Enrollment> findByCourse(Course course);
    Optional<Enrollment> findByCourse_CourseIdAndCustomer_CustomerId(long courseId, long customerId);
>>>>>>> jpdWeb6/master
}
