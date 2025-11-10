package com.jpd.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Course;
import com.jpd.web.model.Language;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseRepository  extends JpaRepository<Course, Long>{
List<Course> findByAccessMode(AccessMode accessMode);
@Query("SELECT DISTINCT c.language FROM Course c")
List<Language> findDistinctLanguages();
List<Course> findByLanguage(Language language);
@Query(value = """
SELECT c.* FROM course c
LEFT JOIN creator cr ON c.creator_id = cr.creator_id
WHERE c.ispublic = true
AND (
    LOWER(c.name) LIKE LOWER(CONCAT('%', :searchKey, '%'))
    OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchKey, '%'))
    OR LOWER(c.learning_object) LIKE LOWER(CONCAT('%', :searchKey, '%'))
    OR LOWER(cr.full_name) LIKE LOWER(CONCAT('%', :searchKey, '%'))
    OR LOWER(c.language) LIKE LOWER(CONCAT('%', :searchKey, '%'))
)
""", 
countQuery = """
SELECT COUNT(c.course_id) FROM course c
LEFT JOIN creator cr ON c.creator_id = cr.creator_id
WHERE c.ispublic = true
AND (
    LOWER(c.name) LIKE LOWER(CONCAT('%', :searchKey, '%'))
    OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchKey, '%'))
    OR LOWER(c.learning_object) LIKE LOWER(CONCAT('%', :searchKey, '%'))
    OR LOWER(cr.full_name) LIKE LOWER(CONCAT('%', :searchKey, '%'))
    OR LOWER(c.language) LIKE LOWER(CONCAT('%', :searchKey, '%'))
)
""",
nativeQuery = true)
Page<Course> searchByKey(@Param("searchKey") String searchKey, Pageable pageable);
@Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.courseId = :courseId")
int countEnrollmentsByCourseId(@Param("courseId") Long courseId);

@Query("SELECT COUNT(f) FROM Feedback f " +
       "JOIN f.enrollment e " +
       "WHERE e.course.courseId = :courseId")
int countFeedbacksByCourseId(@Param("courseId") Long courseId);

@Query("SELECT AVG(f.rate) FROM Feedback f " +
       "JOIN f.enrollment e " +
       "WHERE e.course.courseId = :courseId")
Double getAverageRatingByCourseId(@Param("courseId") Long courseId);

Page<Course> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        String name, String description, Pageable pageable
);
}
