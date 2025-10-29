package com.jpd.web.controller.admin;

import com.jpd.web.dto.CustomerSearchDto;
import com.jpd.web.dto.UserInfoDto;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Enrollment;
import com.jpd.web.service.CustomerManaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/customers")
@Slf4j
public class AdminCustomerController {

    @Autowired
    private CustomerManaService customerManaService;

    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerManaService.getAllCustomers());
    }

    @GetMapping("/search")
    public ResponseEntity<CustomerSearchDto> getCustomerByEmail(@RequestParam String email) {
        return ResponseEntity.ok(customerManaService.getCustomerByEmail(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserInfoDto> getCustomerDetail(@PathVariable Long id) {
        return ResponseEntity.ok(customerManaService.getCustomerProfile(id));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<UserInfoDto> updateProfile(@PathVariable Long id,
                                                     @RequestBody UserInfoDto userInfoDto) {
        return ResponseEntity.ok(customerManaService.updateCustomerProfile(id, userInfoDto));
    }

    @GetMapping("/{id}/enrollment-history")
    public ResponseEntity<List<Enrollment>> getEnrollmentHistory(@PathVariable Long id) {
        return ResponseEntity.ok(customerManaService.getEnrollmentHistory(id));
    }
}
