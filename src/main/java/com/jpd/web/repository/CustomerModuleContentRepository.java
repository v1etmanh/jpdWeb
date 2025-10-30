package com.jpd.web.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.jpd.web.model.CustomerModuleContent;
import com.jpd.web.model.Enrollment;

import java.util.List;
import java.util.Set;

import com.jpd.web.model.TypeOfContent;
import com.jpd.web.model.Module;
import org.springframework.data.repository.query.Param;


public interface CustomerModuleContentRepository extends JpaRepository<CustomerModuleContent, Long> {
    long countByEnrollment(Enrollment enrollment);

    Optional<CustomerModuleContent> findByEnrollmentAndModule(Enrollment enrollment, Module module);


    /**
     * Lấy tất cả CustomerModuleContent của một enrollment
     * Dùng để check progress chi tiết
     */
    List<CustomerModuleContent> findByEnrollment(Enrollment enrollment);


    /**
     * Đếm số module đã có progress (có ít nhất 1 typeOfContent)
     * cho một enrollment
     */
    @Query("SELECT COUNT(cmc) FROM CustomerModuleContent cmc " +
            "WHERE cmc.enrollment = :enrollment " +
            "AND SIZE(cmc.typeOfContent) > 0")
    long countCompletedModulesByEnrollment(@Param("enrollment") Enrollment enrollment);






}
