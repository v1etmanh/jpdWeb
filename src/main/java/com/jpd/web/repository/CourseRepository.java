package com.jpd.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Course;
import com.jpd.web.model.Language;

import java.util.List;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository  extends JpaRepository<Course, Long> , JpaSpecificationExecutor<Course>{
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
)
""", nativeQuery = true)
List<Course> searchByKey(@Param("searchKey") String searchKey);
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
    @Query("select c from Course c where " +
            "c.courseId=:courseId and c.isPublic = true and c.isBan = false ")
    Optional<Course> findById(@Param("courseId") long courseId);
    @Query("select c from Course c where c.isPublic = true and c.isBan = false")
    List<Course> findAll();
}
