package com.jpd.web.specification;

import com.jpd.web.dto.TransactionFilterDto;
import com.jpd.web.model.Course;
import com.jpd.web.model.CustomerTransaction;
import com.jpd.web.model.Enrollment;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TransactionSpecification {

    public static Specification<CustomerTransaction> withFilters(TransactionFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by status
            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            // Filter by date range
            if (filter.getStartDate() != null && filter.getEndDate() != null) {
                predicates.add(criteriaBuilder.between(
                        root.get("createdAt"),
                        filter.getStartDate(),
                        filter.getEndDate()));
            } else if (filter.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        filter.getStartDate()));
            } else if (filter.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdAt"),
                        filter.getEndDate()));
            }

            // Filter by amount range
            if (filter.getMinAmount() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("amount"),
                        filter.getMinAmount()));
            }

            if (filter.getMaxAmount() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("amount"),
                        filter.getMaxAmount()));
            }

            // Filter by customer ID
            if (filter.getCustomerId() != null) {
                Join<CustomerTransaction, Enrollment> enrollment = root.join("enrollment");
                predicates.add(criteriaBuilder.equal(
                        enrollment.get("customer").get("customerId"),
                        filter.getCustomerId()));
            }

            // Filter by course ID
            if (filter.getCourseId() != null) {
                Join<CustomerTransaction, Enrollment> enrollment = root.join("enrollment");
                predicates.add(criteriaBuilder.equal(
                        enrollment.get("course").get("courseId"),
                        filter.getCourseId()));
            }

            // Filter by creator ID
            if (filter.getCreatorId() != null) {
                Join<CustomerTransaction, Enrollment> enrollment = root.join("enrollment");
                Join<Enrollment, Course> course = enrollment.join("course");
                predicates.add(criteriaBuilder.equal(
                        course.get("creator").get("creatorId"),
                        filter.getCreatorId()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
