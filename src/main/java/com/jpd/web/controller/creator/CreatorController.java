package com.jpd.web.controller.creator;

import java.util.List;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.dto.CreatorDashboardDTO;
import com.jpd.web.dto.CreatorDto;
import com.jpd.web.dto.CreatorProfileDto;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Withdraw;
import com.jpd.web.service.CreatorService;
import com.jpd.web.service.utils.RequestAttributeExtractor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@Slf4j
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
@PostMapping("/createWithdraw")
    public ResponseEntity<?> createWithdraw(@RequestParam("amount") double amount,HttpServletRequest request){
    long creatorId=RequestAttributeExtractor.extractCreatorId(request);

    return  ResponseEntity.status(HttpStatus.CREATED) .body(this.creatorService.createWithdraw(creatorId, amount));
}
@GetMapping("/getStatisticInfor")
public  ResponseEntity< CreatorDashboardDTO >getStatisticInfor(HttpServletRequest request) {
	long creatorId=RequestAttributeExtractor.extractCreatorId(request);
    return ResponseEntity.ok(creatorService.retrieveStatictisInfo(creatorId));
    		}
@PostMapping("/upade_certificate") // Nên sửa thành /update_certificate
public ResponseEntity<?> updateCertificate(
        @RequestParam("certificateFile") MultipartFile[] files, // Đổi thành array
        HttpServletRequest request) throws FileUploadException {
    
    long creatorId = RequestAttributeExtractor.extractCreatorId(request);
    
    log.info("Uploading {} certificates for creator {}", files.length, creatorId);
    
    // Validate và upload từng file
    for (MultipartFile file : files) {
        if (file.isEmpty()) {
            continue;
        }
        this.creatorService.upLoadCertificate(creatorId, file);
    }
    
    return ResponseEntity.status(HttpStatus.CREATED).build();
}
@GetMapping("/history_transaction")
public ResponseEntity<List<Withdraw>> HistoryTransaction(HttpServletRequest request){
	long creatorId= RequestAttributeExtractor.extractCreatorId(request);
	List<Withdraw> ls=this.creatorService.historyTransaction(creatorId);
	return ResponseEntity.ok(ls);
}
@GetMapping("/getBalance")
public ResponseEntity<Double> getBalance(HttpServletRequest request){
	long creatorId= RequestAttributeExtractor.extractCreatorId(request);

	double balance=this.creatorService.getblance(creatorId);
	return ResponseEntity.ok(balance);
}

@PostMapping("/code-paypal")
public ResponseEntity<?> createCode(HttpServletRequest request)
{// gui 1 ma toi email
	//  nguoi dung nhan dc thon bao va yeu cau nhap ma
	// 
	long creatorId= RequestAttributeExtractor.extractCreatorId(request);
    this.creatorService.createCodeToEmail(creatorId);
    return ResponseEntity.noContent().build();
	
	}
@PutMapping("/change-ppEmail")
public ResponseEntity<?>changePaypalEmail(@RequestParam("newPaypal")String paypalEmail,@RequestParam("code")String code,HttpServletRequest request)
{long creatorId= RequestAttributeExtractor.extractCreatorId(request);
	this.creatorService.changePaypalEmail(creatorId, code, paypalEmail);
	return ResponseEntity.noContent().build();
	}
}
