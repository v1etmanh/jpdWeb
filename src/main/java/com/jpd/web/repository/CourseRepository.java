package com.jpd.web.repository;

import com.jpd.web.dto.CourseSearchDto;
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

public interface CourseRepository extends JpaRepository<Course, Long> {
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


    /**
     * Truy vấn tùy chỉnh để tìm kiếm Course, đồng thời tính toán rating và số học viên.
     * Các alias 'rating' và 'numberStudent' khớp với tham số 'sort' từ frontend.
     */
    @Query("""
    SELECT new com.jpd.web.dto.CourseSearchDto(
        c.courseId,
        c.name,
        c.urlImg,
        COUNT(DISTINCT e.customer) as numberStudent,
        COALESCE(AVG(f.rate), 0.0) as rating,
        creator.fullName,
        c.price,
        c.language
    )
    FROM Course c
    LEFT JOIN c.creator creator
    LEFT JOIN c.enrollments e
    LEFT JOIN e.feedback f
    WHERE
        c.isPublic = true
        AND c.isBan = false
        AND (
            LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(creator.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(FUNCTION('str', c.language)) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
    GROUP BY
        c.courseId,
        c.name,
        c.urlImg,
        creator.fullName,
        c.price,
        c.language
""")
    Page<CourseSearchDto> searchAndCalculate(
            @Param("keyword") String keyword,
            Pageable pageable
    );


}
