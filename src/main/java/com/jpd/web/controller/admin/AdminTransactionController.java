package com.jpd.web.controller.admin;

import com.jpd.web.model.CustomerTransaction;
import com.jpd.web.service.TransactionManaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/transactions")
@Slf4j
public class AdminTransactionController {

    @Autowired
    private TransactionManaService transactionManaService;

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<List<CustomerTransaction>> getTransactionOfCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(transactionManaService.getTransactionOfCustomer(customerId));
    }

    @GetMapping("/courses/{courseId}/revenue")
    public ResponseEntity<Double> getTotalRevenue(@PathVariable Long courseId) {
        return ResponseEntity.ok(transactionManaService.getTotalRevenue(courseId));
    }
}
