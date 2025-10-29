package com.jpd.web.dto;

import java.sql.Date;
<<<<<<< HEAD

import org.springframework.web.bind.annotation.RequestMapping;
=======
import java.time.LocalDateTime;
>>>>>>> jpdWeb6/master

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
<<<<<<< HEAD
private Date createDate;
=======
private LocalDateTime createDate;
>>>>>>> jpdWeb6/master
private String email;
private boolean isCreator;

	
}
