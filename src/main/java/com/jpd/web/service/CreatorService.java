package com.jpd.web.service;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.CreatorDto;
import com.jpd.web.dto.CreatorProfileDto;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Status;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.transform.CreatorTransform;

import lombok.extern.slf4j.Slf4j;
/*C:\Users\Admin>hookdeck listen 9090 paypal-webhook --path /webhook/paypal

Dashboard
👉 Inspect and replay events: https://dashboard.hookdeck.com?team_id=tm_jpXk88lTVQQb
 * UNCLAIMED*/
@Service
@Slf4j
public class CreatorService {

    @Autowired
    private CreatorRepository creatorRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private FireBaseService fireBaseService;
    @Autowired
    private PayPalPayoutServiceV2 palPayoutServiceV2;
    private static final SecureRandom secureRandom = new SecureRandom();
    /**
     * Upload profile cho creator.
     * @param email email của customer
     * @param profileDto thông tin profile
     */
    public void uploadProfile(String email, CreatorProfileDto profileDto) {
    	if (!profileDto.getAgreedToTerms()) {
    	    throw new IllegalArgumentException("User must accept terms and conditions");
    	}
        // 1. Check customer tồn tại
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Customer with email " + email + " not found"));

        // 2. Check creator đã tồn tại chưa
        Optional<Creator> existingCreatorOpt = creatorRepository.findByCustomer(customer);
        if (existingCreatorOpt.isPresent()) {
            log.warn("Creator profile already exists for customer {}", email);
            return; // Hoặc throw exception nếu muốn chặn
        }

        // 3. Tạo mới Creator
        Creator creator = CreatorTransform.transformFromCreatorDto(profileDto);
           creator.setStatus(Status.PENDING);
        // 4. Upload ảnh nếu có
        if (profileDto.getProfileImage() != null && !profileDto.getProfileImage().isEmpty()) {
            try {
                String imgUrl = fireBaseService.uploadFile(profileDto.getProfileImage(), TypeOfFile.IMG);
                creator.setImageUrl(imgUrl);
            } catch (IOException e) {
                log.error("Error uploading profile image for customer {}", email, e);
                throw new RuntimeException("Failed to upload profile image", e);
            }
        }

        // 5. Gán customer cho creator
        creator.setCustomer(customer);

        // 6. Lưu vào database
        creatorRepository.save(creator);
        log.info("Creator profile created successfully for customer {}", email);
    }
    public CreatorDto getAccount(String email) {
        Creator c= customerRepository.findByEmail(email)
                .map(Customer::getCreator)  // lấy creator từ customer
                .orElseThrow(() -> new RuntimeException("Customer not found with email: " + email));
        return CreatorTransform.transToCreatorDto(c);
    }
   
    public static String generate6DigitCode() {
        int number = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(number);
    }
    //upload paypalEmail

  private Creator getCreatorByEmail(String email) {
  Customer customer = customerRepository.findByEmail(email)
          .orElseThrow(() -> new IllegalArgumentException("Customer not found with email: " + email));

  Creator creator = customer.getCreator();
  if (creator == null) {
      throw new IllegalStateException("Customer has no creator profile");
  }
  return creator;
  }
    public void sendMoneyToVerify(String email, String paypalEmail) throws Exception {
       Creator creator=getCreatorByEmail(email);

        if (creator.getPaymentEmail() != null) {
            throw new IllegalStateException("Creator already has a payment email");
        }

        // Kiểm tra số lượng gửi trong hôm nay
        if (palPayoutServiceV2.isMax(creator)) {
            throw new IllegalStateException("Exceeded payout requests for today");
        }

        // Sinh mã ngẫu nhiên 6 chữ số để gửi kèm note
        String verifyCode =  generate6DigitCode();

        // Gửi 1 USD (nên dùng BigDecimal thay vì double)
        palPayoutServiceV2.createSinglePayout(
                paypalEmail,
                 10.00   , // 1 USD
                "USD",
                verifyCode,
                creator
        );
    }


    	
    	
    	
    
   
    
    

}
