package com.jpd.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.dto.CreatorDto;
import com.jpd.web.dto.CreatorProfileDto;
import com.jpd.web.model.Creator;
import com.jpd.web.service.CreatorService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("api/creator/")
public class CreatorController {
@Autowired
private CreatorService creatorService;
@PostMapping(value="/upload_profile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<?> postMethodName( @Valid @ModelAttribute CreatorProfileDto creatorProfileDto,@AuthenticationPrincipal Jwt jwt) {
    //TODO: process POST request
	String email=jwt.getClaimAsString("email");
  try {  this.creatorService.uploadProfile(email, creatorProfileDto);
  return ResponseEntity.status(HttpStatus.OK).build();
  }
  catch (Exception e) {
	// TODO: handle exception
	  System.out.print(e);
	  return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
}
    
    
}
@GetMapping("/getAccount")
public ResponseEntity<CreatorDto> getAccount(@AuthenticationPrincipal Jwt jwt) {
	String email=jwt.getClaimAsString("email");
    CreatorDto c=this.creatorService.getAccount(email);
    return ResponseEntity.status(HttpStatus.OK).body(c);
}
@PostMapping("/upload/paypalEmail")
public ResponseEntity<?> uploadPaypalEmail(@RequestParam("pEmail") String  paypalemail,
		@AuthenticationPrincipal Jwt jwt){
	String email=jwt.getClaimAsString("email");
    //TODO: process POST request
    try {
    	int length=paypalemail.length();
		this.creatorService.sendMoneyToVerify(email, paypalemail.substring(0,length-1));
		 return  ResponseEntity.status(HttpStatus.OK).build();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		 return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	}
   
}



}
