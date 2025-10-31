package com.jpd.web.controller.customer;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.dto.ReportForm;
import com.jpd.web.service.ReportService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/customer/report")
public class ReportController {
	@Autowired
	private ReportService reportService;
@PostMapping()
public ResponseEntity<?> createNewReport(@RequestBody ReportForm entity,@AuthenticationPrincipal Jwt jwt) {
    //TODO: process POST request
	String email=jwt.getClaimAsString("email");
    this.reportService.saveReport(email, entity);
    return null;
}

}
