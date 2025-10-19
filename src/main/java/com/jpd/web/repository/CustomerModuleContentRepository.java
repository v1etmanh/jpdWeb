package com.jpd.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import com.jpd.web.model.CustomerModuleContent;
import com.jpd.web.model.Enrollment;


public interface CustomerModuleContentRepository  extends JpaRepository<CustomerModuleContent,Long>{
 long countByEnrollment(Enrollment enrollment);
}
