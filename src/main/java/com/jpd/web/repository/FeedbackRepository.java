package com.jpd.web.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;
import java.util.List;
import java.util.Optional;


@Repository
public interface FeedbackRepository extends CrudRepository<Feedback,Long> {
	Optional<Feedback> findByEnrollment(Enrollment enrollment);
     
}
