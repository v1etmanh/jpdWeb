package com.jpd.web.service;

import java.io.IOException;
import java.util.Optional;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.CreatorDto;
import com.jpd.web.dto.CreatorProfileDto;
import com.jpd.web.dto.UserInfoDto;
import com.jpd.web.exception.ApiException;
import com.jpd.web.exception.CreatorAlreadyExistsException;
import com.jpd.web.exception.CustomerNotFoundException;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.CustomerTransaction;
import com.jpd.web.model.Status;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.transform.CreatorTransform;
import com.jpd.web.transform.CustomerTransform;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CustomerService {
	@Autowired
	private CustomerRepository cusRe;
	@Autowired
	private CreatorRepository creatorRe;
	@Autowired
	private FireBaseService fireBaseService;

	private Customer createNewCustomer(Jwt jwt) {
		String email = jwt.getClaimAsString("email");
		String name = jwt.getClaimAsString("name");
		String givenName = jwt.getClaimAsString("given_name");
		String familyName = jwt.getClaimAsString("family_name");

		log.info("Creating new customer account for email: {}", email);

		Customer customer = Customer.builder().email(email).username(name != null ? name : email).givenName(givenName)
				.familyName(familyName).role("USER").build();

		Customer savedCustomer = cusRe.save(customer);

		log.info("New customer created with ID: {}", savedCustomer.getCustomerId());

		return savedCustomer;
	}

	@Transactional
	public UserInfoDto getOrCreateAccount(Jwt jwt) {
		String email = jwt.getClaimAsString("email");

		log.info("Getting or creating account for email: {}", email);

		// Find or create customer
		Customer customer = cusRe.findByEmail(email).orElseGet(() -> createNewCustomer(jwt));

		// Check if customer is a creator
		boolean isCreator = creatorRe.findByCustomer(customer).isPresent();

		log.info("Account retrieved for email: {}, isCreator: {}", email, isCreator);

		return CustomerTransform.transToUserInfor(customer, isCreator);
	}

	private String uploadProfileImage(CreatorProfileDto profileDto, String email) {
		try {
			log.debug("Uploading profile image for email: {}", email);

			String imageUrl = fireBaseService.uploadFile(profileDto.getProfileImage(), TypeOfFile.IMG);

			if (imageUrl == null || imageUrl.trim().isEmpty()) {
				throw new FileUploadException("Failed to upload profile image");
			}

			log.debug("Profile image uploaded successfully for email: {}", email);

			return imageUrl;

		} catch (IOException e) {
			log.error("Error uploading profile image for email: {}", email, e);
			throw new ApiException("Failed to upload profile image: " + e.getMessage());
		}
	}

	public CreatorDto uploadProfile(String email, CreatorProfileDto profileDto) {
		Customer customer = cusRe.findByEmail(email)
				.orElseThrow(() -> new CustomerNotFoundException("Customer not found with email: " + email));
		Optional<Creator> existingCreator = creatorRe.findByCustomer(customer);
		if (existingCreator.isPresent()) {
			log.warn("Creator profile already exists for email: {}", email);
			throw new CreatorAlreadyExistsException("You already have a creator profile");
		}

		// 3. Tạo mới Creator
		Creator creator = CreatorTransform.transformFromCreatorDto(profileDto);
		creator.setCustomer(customer);
		creator.setStatus(Status.PENDING);
		// 4. Upload ảnh nếu có
		if (profileDto.getProfileImage() != null && !profileDto.getProfileImage().isEmpty()) {
			String imageUrl = uploadProfileImage(profileDto, email);
			creator.setImageUrl(imageUrl);
		}

		// 5. Gán customer cho creator

		// 6. Lưu vào database
		Creator cr1= creatorRe.save(creator);
		return CreatorTransform.transToCreatorDto(cr1);
	}

}
