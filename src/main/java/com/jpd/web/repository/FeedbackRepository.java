package com.jpd.web.repository;

<<<<<<< HEAD
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
=======
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // Lấy list feedbacks của 1 course thông qua enrollment
    List<Feedback> findByEnrollment_Course_CourseId(Long courseId);

    // Lấy list feedbacks của 1 customer
    List<Feedback> findByEnrollment_Customer_CustomerId(Long customerId);

    // Lấy rating trung bình của 1 khóa học
    @Query("""
        SELECT AVG(e.feedback.rate)
        FROM Enrollment e
        WHERE e.course.courseId = :courseId
          AND e.feedback IS NOT NULL
    """)
    Double getAverageRatingByCourseId(@Param("courseId") Long courseId);

    // Lấy danh sách khóa học theo rating trung bình giảm dần (top rating trước) với Pageable
    @Query("""
        SELECT e.course.courseId, AVG(e.feedback.rate) AS avgRating
        FROM Enrollment e
        WHERE e.feedback IS NOT NULL
        GROUP BY e.course.courseId
        ORDER BY avgRating DESC
    """)
    List<Object[]> getCoursesOrderByAverageRatingDesc(org.springframework.data.domain.Pageable pageable);

    // Tìm feedback theo enrollment
    Optional<Feedback> findByEnrollment(Enrollment enrollment);
}

>>>>>>> jpdWeb6/master
