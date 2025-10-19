package com.jpd.web.exception;

public class EnrollmentExistException extends BusinessException {

	public EnrollmentExistException(String message) {
		 super("ENROLLMENT_ALREADY_EXISTS", message, 
	              "Bạn đã đăng ký khóa học này");
    }

}
