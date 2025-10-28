package com.jpd.web.repository;

import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.repository.CrudRepository;

import com.jpd.web.model.Report;

public interface ReportRepository extends CrudRepository<Report, Long>{

}
