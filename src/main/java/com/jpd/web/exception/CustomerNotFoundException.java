package com.jpd.web.exception;

<<<<<<< HEAD
public class CustomerNotFoundException  extends BusinessException{
	 public CustomerNotFoundException(String message) {
	        super("CUSTOMER_NOT_FOUND", message,
	              "Khách hàng không được tìm thấy");
	    }
=======
public class CustomerNotFoundException extends BusinessException {
    public CustomerNotFoundException(Long customerId) {
        super(
                "CUSTOMER_NOT_FOUND",
                "Customer ID " + customerId + " not found",
                "Khách hàng với ID " + customerId + " không được tìm thấy"
        );
    }

    public CustomerNotFoundException(String email) {
        super(
                "CUSTOMER_NOT_FOUND",
                "Customer with email " + email + " not found",
                "Khách hàng với email " + email + " không được tìm thấy"
        );
    }
>>>>>>> jpdWeb6/master
}
