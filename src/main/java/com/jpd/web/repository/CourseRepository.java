package com.jpd.web.repository;

import com.jpd.web.dto.CourseInfDto;
import com.jpd.web.dto.CourseSearchDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Course;
import com.jpd.web.model.Language;

import java.util.List;


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


    /**
     * Truy vấn tùy chỉnh để tìm kiếm Course, đồng thời tính toán rating và số học viên.
     * Các alias 'rating' và 'numberStudent' khớp với tham số 'sort' từ frontend.
     */
    @Query("SELECT new com.jpd.web.dto.CourseSearchDto(" +
            "c.courseId, " +                  // 1. id (long)
            "c.name, " +                      // 2. name (String)
            "c.urlImg, " +                    // 3. img (String)
            "COUNT(DISTINCT e.customer) as numberStudent, " + // 4. numberStudent (long)
            "COALESCE(AVG(f.rate), 0.0) as rating, " +      // 5. rating (double)
            "creator.fullName, " +                // 6. instructor (String) - 📍 3. Giả sử Creator có trường 'name'
            "c.price, " +                     // 7. price (double)
            "c.language" +                    // 8. language (Language)
            ") " +
            "FROM Course c " +
            "LEFT JOIN c.creator creator " + // Join để lấy tên creator (instructor)
            "LEFT JOIN c.enrollments e " + // Join để đếm học viên
            "LEFT JOIN e.feedback f " + // Join để tính rating
            "WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "AND c.isPublic = true AND c.isBan = false " + // Chỉ tìm khóa học public
            "GROUP BY c.courseId, c.name, c.urlImg, creator.fullName, c.price, c.language")
    // 📍 4. Sửa lại GROUP BY
    Page<CourseSearchDto> searchAndCalculate(
            @Param("name") String name,
            Pageable pageable
    );
}
