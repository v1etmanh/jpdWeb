package com.jpd.web.repository;

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
}
