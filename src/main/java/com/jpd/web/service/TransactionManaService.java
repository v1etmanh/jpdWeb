package com.jpd.web.service;

import com.jpd.web.model.CustomerTransaction;
import com.jpd.web.repository.CustomerTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class TransactionManaService {
    @Autowired
    private CustomerTransactionRepository customerTransactionRepository;

    // list all transaction of a customer
    public List<CustomerTransaction> getTransactionOfCustomer(Long customerId) {
        log.info("Getting transactions for ID: {}", customerId);
        return customerTransactionRepository.findByEnrollment_Customer_CustomerId(customerId);
    }

    //total revenue of 1 course
    public double getTotalRevenue(Long courseId){
        log.info("Getting transactions for courseId: {}", courseId);
        return customerTransactionRepository.getTotalRevenueByCourseId(courseId);
    }
}
