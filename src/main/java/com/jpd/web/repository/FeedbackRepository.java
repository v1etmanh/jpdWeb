package com.jpd.web.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;
import java.util.List;


@Repository
public interface FeedbackRepository extends CrudRepository<Feedback,Long> {
	List<Feedback> findByEnrollment(Enrollment enrollment);

}
