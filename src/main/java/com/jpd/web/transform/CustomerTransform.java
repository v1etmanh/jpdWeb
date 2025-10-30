package com.jpd.web.transform;

import com.jpd.web.dto.CreatorDto;
import com.jpd.web.dto.UserInfoDto;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;

public class CustomerTransform {
public static UserInfoDto transToUserInfor(Customer customer,boolean isCreator) {
   UserInfoDto userInfoDto=new UserInfoDto();
   userInfoDto.setFamilyName(customer.getFamilyName());
   userInfoDto.setUserName(customer.getUsername());
   userInfoDto.setGivenName(userInfoDto.getGivenName());
   userInfoDto.setCreateDate(customer.getCreateDate());
   userInfoDto.setRole(customer.getRole());
   userInfoDto.setEmail(customer.getEmail());
   userInfoDto.setCreator(isCreator);
   return userInfoDto;
   
}

}
