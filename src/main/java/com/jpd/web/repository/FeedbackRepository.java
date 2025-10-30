package com.jpd.web.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;

import java.util.Optional;


@Repository
public interface FeedbackRepository extends CrudRepository<Feedback,Long> {
	Optional<Feedback> findByEnrollment(Enrollment enrollment);
    @Query("SELECT f FROM Feedback f WHERE f.enrollment.enrollId = :enrollmentId")
    Optional<Feedback> findFeedbackByEnrollmentId(@Param("enrollmentId") Long enrollmentId);
}
