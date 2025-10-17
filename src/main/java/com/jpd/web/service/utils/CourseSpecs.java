package com.jpd.web.service.utils;

import com.jpd.web.model.Course;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
public class CourseSpecs {
    // Dùng 1 spec cho "nhìn thấy được"
    public static Specification<Course> isVisible() {
        return (root, cq, cb) -> cb.and(
                cb.notEqual(root.get("accessMode"), "PRIVATE"),
                cb.isTrue(root.get("isPublic"))
        );
    }

    // Tìm kiếm tự do trên name, creator.fullName, language
    public static Specification<Course> freeText(String q) {
        return (root, cq, cb) -> {
            if (q == null || q.isBlank()) {
                return cb.conjunction(); // không lọc theo text
            }

            String pattern = "%" + escapeLike(q) + "%";

            // name
            var nameLike = cb.like(cb.lower(root.get("name")), pattern);

            // creator.fullName (LEFT JOIN vì có thể null)
            var creator = root.join("creator", JoinType.LEFT);
            var creatorLike = cb.like(cb.lower(creator.get("fullName")), pattern);

            // language cũng LIKE để "1 ô search" tự lọc theo ngôn ngữ
            var languageLike = cb.like(cb.lower(root.get("language")), pattern);

            //  description, tags... ở đây
             var descLike = cb.like(cb.lower(root.get("description")), pattern);

            return cb.or(nameLike, creatorLike, languageLike ,descLike);
        };
    }

    // Escape ký tự đặc biệt cho LIKE (%, _)
    private static String escapeLike(String input) {
        // tuỳ DB Dialect, đây là cách phổ thông
        return input.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
//    public static Specification<Course> searchCourseByNameOrCreator(String q){
//        return  (root, criteriaQuery, criteriaBuilder) -> {
//            if (q==null||q.isBlank()) return criteriaBuilder.conjunction();
//            Predicate accessMode = criteriaBuilder.notEqual(root.get("accessMode"),"PRIVATE") ;
//            Predicate isPublic = criteriaBuilder.isTrue(root.get("isPublic"));
//            Predicate basePredicate = criteriaBuilder.and(accessMode,isPublic);
//            String pattern = "%"+ q.trim().toLowerCase()+"%";
//            Join<Course, Creator> creator = root.join("creator", JoinType.LEFT);
//            return criteriaBuilder.and(criteriaBuilder.or(
//                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),pattern),
//                    criteriaBuilder.like(creator.get("fullName"),pattern)
//            ),basePredicate);
//        };
//    }
//
//
//    public  static Specification<Course> searchCourseByLanguageEqual(String language){
//        return (root, query, criteriaBuilder) -> {
//            if(language==null||language.isBlank()) return criteriaBuilder.conjunction();
//            Predicate accessMode = criteriaBuilder.notEqual(root.get("accessMode"),"PRIVATE") ;
//            Predicate isPublic = criteriaBuilder.isTrue(root.get("isPublic"));
//            Predicate basePredicate = criteriaBuilder.and(accessMode,isPublic);
//            return criteriaBuilder.and(criteriaBuilder.equal(criteriaBuilder.lower(root.get("language")),language.trim().toLowerCase()),basePredicate) ;
//        };
//    }
}
