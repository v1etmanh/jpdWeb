package com.jpd.web.dto;

import java.sql.Date;

import org.springframework.web.bind.annotation.RequestMapping;

import com.google.auto.value.AutoValue.Builder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@RequiredArgsConstructor
public class UserInfoDto {
private String userName;
private String familyName;
private String Role;
private String givenName;
private Date createDate;
private String email;
private boolean isCreator;

	
}
