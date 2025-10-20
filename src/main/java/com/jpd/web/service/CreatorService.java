package com.jpd.web.service;


import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.CreatorDto;

import com.jpd.web.exception.PaymentEmailAlreadyExistsException;
import com.jpd.web.exception.PayoutLimitExceededException;
import com.jpd.web.model.Creator;

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
	private PayPalPayoutServiceV2 palPayoutServiceV2;

	@Autowired
	private ValidationResources validationResources;
	private static final double VERIFICATION_AMOUNT = 1.00;
	private static final String VERIFICATION_CURRENCY = "USD";

	@Transactional()
	public CreatorDto getAccount(Long creatorId) {
		log.info("Retrieving account information for creator {}", creatorId);

		Creator creator = validationResources.validateCreatorExists(creatorId);

		return CreatorTransform.transToCreatorDto(creator);
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
		String verifyCode = LocalDateTime.now() + "";

		// Gửi 1 USD (nên dùng BigDecimal thay vì double)
		try {
			palPayoutServiceV2.createSinglePayout(paypalEmail, VERIFICATION_AMOUNT, // 1 USD
					VERIFICATION_CURRENCY, verifyCode, creator);
		} catch (Exception e) {
			log.error("Failed to send verification payment for creator {}", creatorId, e);
			e.printStackTrace();
		}
	}

}
