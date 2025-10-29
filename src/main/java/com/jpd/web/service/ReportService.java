package com.jpd.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.ReportForm;
import com.jpd.web.model.Course;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Report;
import com.jpd.web.repository.ReportRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.ReportTransform;

import io.swagger.v3.oas.annotations.servers.Server;

@Service
public class ReportService {
@Autowired
private ReportRepository reportRepository;
@Autowired
private ValidationResources validationResources;
public void saveReport(String email,ReportForm reportForm) {
	
	Customer cus=this.validationResources.validateCustomerExist(email);
Course c=	this.validationResources.validateCustomerWithCourse(email, reportForm.getCourseId());
	Report rep=ReportTransform.transToReport(reportForm, c, cus);
	this.reportRepository.save(rep);
}
}
