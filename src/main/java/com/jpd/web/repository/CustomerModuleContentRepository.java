package com.jpd.web.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import com.jpd.web.model.CustomerModuleContent;
import com.jpd.web.model.Enrollment;
import java.util.List;
import java.util.Set;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.model.Module;





public interface CustomerModuleContentRepository  extends JpaRepository<CustomerModuleContent,Long>{
 long countByEnrollment(Enrollment enrollment);
 Optional<CustomerModuleContent> findByEnrollmentAndModule(Enrollment enrollment, Module module);
}
