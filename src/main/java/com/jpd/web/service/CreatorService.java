package com.jpd.web.service;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.jpd.web.exception.WithdrawException;
import com.jpd.web.model.*;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.WithdrawRepository;

import org.apache.hc.client5.http.entity.mime.MultipartPart;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.dto.CreatorDashboardDTO;
import com.jpd.web.dto.CreatorDto;
import com.jpd.web.dto.PopularCourseDTO;
import com.jpd.web.exception.PaymentEmailAlreadyExistsException;
import com.jpd.web.exception.PayoutLimitExceededException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.service.utils.CodeGenerator;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.CreatorTransform;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/*C:\Users\Admin>hookdeck listen 9090 paypal-webhook --path /webhook/paypal

Dashboard
👉 Inspect and replay events: https://dashboard.hookdeck.com?team_id=tm_jpXk88lTVQQb
 * UNCLAIMED*/
@Service
@Slf4j
public class CreatorService {
	@Autowired
	 private  MonthlyBalanceService monthlyBalanceService;
	@Autowired
	private PayPalPayoutServiceV2 palPayoutServiceV2;
    @Value("${creator.withdraw.minimize_amount}")  // Có dấu $

    private double minimizeAmount;
    @Autowired 
    private CodeGenerator codeGenerator;
	@Autowired
	private ValidationResources validationResources;
	private static final double VERIFICATION_AMOUNT = 1.00;
	private static final String VERIFICATION_CURRENCY = "USD";
    @Autowired
    private WithdrawRepository withdrawRepository;
    @Autowired
    private FireBaseService fireBaseService;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CreatorRepository creatorRepository;
	@Transactional()
	public CreatorDto getAccount(Long creatorId) {
		log.info("Retrieving account information for creator {}", creatorId);

		Creator creator = validationResources.validateCreatorExists(creatorId);

		return CreatorTransform.transToCreatorDto(creator);
	}
	public double getblance(Long creatorId) {
		log.info("Retrieving account information for creator {}", creatorId);

		Creator creator = validationResources.validateCreatorExists(creatorId);

		return creator.getBalance();
	}
	// upload paypalEmail

	public void sendMoneyToVerify(long creatorId, String paypalEmail) {
		Creator creator = validationResources.validateCreatorExists(creatorId);

		if (creator.getPaymentEmail() != null) {
			throw new PaymentEmailAlreadyExistsException("Creator already has a payment email");
		}

		// Kiểm tra số lượng gửi trong hôm nay
		if (palPayoutServiceV2.isMax(creator)) {
			throw new PayoutLimitExceededException("Exceeded payout requests for today");
		}

		// Sinh mã ngẫu nhiên 6 chữ số để gửi kèm note
		String verifyCode = codeGenerator.generate6DigitCode();

		// Gửi 1 USD (nên dùng BigDecimal thay vì double)
		try {
			palPayoutServiceV2.createSinglePayout(paypalEmail, VERIFICATION_AMOUNT, // 1 USD
					VERIFICATION_CURRENCY, verifyCode, creator,TargetPayout.VERIFY_EMAIL );
		} catch (Exception e) {
			log.error("Failed to send verification payment for creator {}", creatorId, e);
			e.printStackTrace();
		}
	}
   public Withdraw createWithdraw(long creatorId,double amount) {
        Creator creator = validationResources.validateCreatorExists(creatorId);
        //check valid
       if(!isValidToWithdraw(creator,amount)) {
           throw new WithdrawException("Withdraw amount is invalid");
       }

       try{
           String note=LocalDateTime.now()+" "+creator.getCreatorId()+" with paypalEmail: "+creator.getPaymentEmail()+"amount: "+amount;
         PayoutTracking p= palPayoutServiceV2.createSinglePayout(creator.getPaymentEmail(),amount,VERIFICATION_CURRENCY,note,creator,TargetPayout.WITHDRAW);
           Withdraw w = new Withdraw();
           w.setAmount(amount);
           w.setStatus(Status.PENDING);
           w.setContent(note);
           w.setCurrency(VERIFICATION_CURRENCY);
           w.setPayoutBatchId(p.getPayoutBatchId());
           w.setCreator(creator);
           double b=creator.getBalance();
           creator.setBalance(b-amount);
          return this.withdrawRepository.save(w);

       }
       catch (Exception e) {
           log.error("Failed to send verification payment for creator {}", creatorId, e);

           throw new WithdrawException("Failed to process withdrawal request"+ e);
       }
       // valid Include: payment method ton tai, certificate ton tai, so tien lon hon> a
       //dung paypal chuyen khoan do toi email paypal cua nguoi dung
   }
   boolean isValidToWithdraw(Creator creator, double amount) {
	    // Check creator status
	    if (creator.getStatus() != Status.SUCCESS) {
	        return false;
	    }
	    
	    // Check payment email exists
	    if (creator.getPaymentEmail() == null || creator.getPaymentEmail().isEmpty()) {
	        return false;
	    }
	    
	    // Check certificate exists (nếu bắt buộc phải có certificate)
	    if (creator.getCertificateUrl() == null || creator.getCertificateUrl().isEmpty()) {
	        return false;
	    }
	    
	    // Check balance sufficient
	    if (creator.getBalance() < minimizeAmount) {
	        return false;
	    }
	    
	    // Check withdraw amount valid
	    if (amount <= 0 || amount > creator.getBalance()) {
	        return false;
	    }
	    
	    return true;
	}
   public CreatorDashboardDTO retrieveStatictisInfo(long creatorId) {
	   Creator c=validationResources.validateCreatorExists(creatorId);
	   if(c.getStatus()!=Status.SUCCESS) {
		   System.out.print(c.getStatus());
		   throw new UnauthorizedException("error to fget");
}
	   
	   MonthlyCreatorBalance currentMonthBalance = 
	            monthlyBalanceService.getCurrentMonthDashboard(creatorId);
	   List<PopularCourseDTO> popularCourseDTOs = currentMonthBalance.getPopularCourses().stream()
	            .map(courseId -> {
	                Course course = courseRepository.findById(courseId).orElse(null);
	                return course != null ?CreatorTransform.transform(course) : null;
	            })
	            .filter(dto -> dto != null)
	            .collect(Collectors.toList());
	        // Transform sang DTO
	        return CreatorTransform.transformFromMonthlyBalance(currentMonthBalance,popularCourseDTOs);
	   
   }
   public void upLoadCertificate(long creatorId,MultipartFile multipartFile) throws FileUploadException {	   
	   Creator c=validationResources.validateCreatorExists(creatorId);
	   try {
	        String url =fireBaseService.uploadFile(multipartFile, TypeOfFile.CERTIFICATE);
	        c.getCertificateUrl().add(url);
	        c.setStatus(Status.PENDING);
	        this.creatorRepository.save(c);
	    } catch (IOException e) {
	        throw new FileUploadException("Error uploading certificate", e);
	        // ✅ Giữ lại stacktrace để debug
	    }
   }
   public List<Withdraw> historyTransaction(long creatorId){
	   
	   Creator c=this.validationResources.validateCreatorExists(creatorId);
	   if(c.getStatus()==Status.SUCCESS)
	   return this.withdrawRepository.findByCreator(c);
	   else 
		   throw new UnauthorizedException("you dont have role to do this task");
   }
}
