package com.jpd.web.repository;

import com.jpd.web.dto.ModuleContentGroupDto;
import com.jpd.web.model.CustomerModuleContent;
import com.jpd.web.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerModuleContentRepository extends JpaRepository<CustomerModuleContent, Long> {


    @Query("SELECT new com.jpd.web.dto.ModuleContentGroupDto(cmc.moduleContent, COUNT(cmc)) " +
            "FROM CustomerModuleContent cmc " +
            "WHERE cmc.enrollment = :enrollment " + // <-- Thêm điều kiện WHERE ở đây
            "GROUP BY cmc.moduleContent")
    List<ModuleContentGroupDto> findGroupedByModuleContentForEnrollment(@Param("enrollment") Enrollment enrollment);


}
