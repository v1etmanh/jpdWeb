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
import com.jpd.web.service.utils.RequestAttributeExtractor;

import jakarta.servlet.http.HttpServletRequest;
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

@GetMapping("/getAccount")
public ResponseEntity<CreatorDto> getAccount(HttpServletRequest request) {
    long creatorId=RequestAttributeExtractor.extractCreatorId(request);
    CreatorDto c=this.creatorService.getAccount(creatorId);
    return ResponseEntity.status(HttpStatus.OK).body(c);
}
@PostMapping("/upload/paypalEmail")
public ResponseEntity<?> uploadPaypalEmail(@RequestParam("pEmail") String  paypalemail
	,HttpServletRequest request	){
	
    //TODO: process POST request
        long creatorId=RequestAttributeExtractor.extractCreatorId(request);
    	int length=paypalemail.length();
		this.creatorService.sendMoneyToVerify(creatorId, paypalemail.substring(0,length-1));
		
		 return  ResponseEntity.status(HttpStatus.CREATED).build();
	
		// TODO Auto-generated catch block
	
   
}



}
