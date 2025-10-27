package com.jpd.web.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.jpd.web.model.Customer;

public interface CustomerRepository extends CrudRepository<Customer, Long> {
Optional<Customer> findByEmail(String email);
}
