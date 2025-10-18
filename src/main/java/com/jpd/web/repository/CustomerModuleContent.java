package com.jpd.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerModuleContent extends JpaRepository<CustomerModuleContent, String> {
    @Query("select cm from CustomerModuleContent cm join Enrollment e on cm.enrollment.enrollId=e.enrollId where e.enrollId=: enrollmentId ")
    List<com.jpd.web.model.CustomerModuleContent> findCustomerModuleContentByEnrollment(@Param("enrollmentId") String enrollmentId);
}
