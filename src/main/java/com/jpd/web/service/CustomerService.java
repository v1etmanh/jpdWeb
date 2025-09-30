package com.jpd.web.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.UserInfoDto;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.CustomerTransaction;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.transform.CustomerTransform;

@Service
public class CustomerService {
	@Autowired
	private CustomerRepository cusRe;
	@Autowired
	private CreatorRepository creatorRe;
public UserInfoDto getAccountInf(Jwt jwt) {
	String email=jwt.getClaimAsString("email");
	Optional<Customer>cus=this.cusRe.findByEmail(email);
	Customer c=null;
	boolean isCreator=false;
	if(cus.isEmpty()) {
		
	        String name =jwt.getClaimAsString("name");
	        String givenName=jwt.getClaimAsString("given_name");
	        String familyName=jwt.getClaimAsString("family_name");
	    
	        
		 c=Customer.builder().familyName(familyName)
				.givenName(givenName)
				.email(email)
				.username(name)
				.role("USER")
				.build();
		
	c =this.cusRe.save(c);
		
	}else {
		
	c=cus.get();
	Optional<Creator> ce= this.creatorRe.findByCustomer(c);
	if(ce.isPresent()) {isCreator=true;}
	
		
	}
	
	return CustomerTransform.transToUserInfor(c, isCreator);
}
}
