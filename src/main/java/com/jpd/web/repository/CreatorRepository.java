package com.jpd.web.repository;

import org.springframework.data.repository.CrudRepository;

import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;

import java.util.List;
import java.util.Optional;


public interface CreatorRepository extends CrudRepository<Creator, Long> {
  Optional<Creator> findByCustomer(Customer customer);
}
