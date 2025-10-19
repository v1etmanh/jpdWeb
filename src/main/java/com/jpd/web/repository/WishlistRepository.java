package com.jpd.web.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.jpd.web.model.Wishlist;
import com.jpd.web.model.Customer;


@Repository
public interface WishlistRepository extends CrudRepository<Wishlist,Long> {
Optional<Wishlist>findByCourse_CourseIdAndCustomer_CustomerId(long courseId,long customerId);
List<Wishlist> findByCustomer(Customer customer);
}
