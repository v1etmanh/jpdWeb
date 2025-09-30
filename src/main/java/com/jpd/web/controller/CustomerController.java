package com.jpd.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.dto.UserInfoDto;
import com.jpd.web.model.Customer;
import com.jpd.web.service.CustomerService;

@RestController
@RequestMapping("api/customer")
public class CustomerController {
@Autowired
private CustomerService customerSer;
@GetMapping("/account_infor")
public ResponseEntity<UserInfoDto> getCustomerAccountInf(@AuthenticationPrincipal Jwt jwt){
	UserInfoDto c=this.customerSer.getAccountInf(jwt);
	System.out.print("recieve");
	return ResponseEntity.status(HttpStatus.OK).body(c);
}
}
