package com.jpd.web.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.WishlistDto;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.WishlistExistException;
import com.jpd.web.model.Course;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Wishlist;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.WishlistRepository;
import com.jpd.web.transform.WishlistTransform;

@Service
public class WishlistService {
@Autowired
private WishlistRepository wishlistRepository;
@Autowired
private CustomerRepository customerRepository;
@Autowired
private CourseRepository courseRepository;
public void addWishlist(String email, long courseId) {
	//check wishlist is exist 
	//
	Customer cus=this.customerRepository.findByEmail(email).get();
	Optional<Course>course=this.courseRepository.findById(courseId);
	if(course.isEmpty())throw new CourseNotFoundException(courseId);
 Optional<Wishlist>e =this.wishlistRepository.findByCourse_CourseIdAndCustomer_CustomerId(courseId, cus.getCustomerId());
 if(e.isPresent())throw new WishlistExistException("you have added this course in your wishlish");
 
 Wishlist w=Wishlist.builder()
		 .customer(cus)
		 .course(course.get())
		 .build();
 this.wishlistRepository.save(w);
}
public List<WishlistDto> retrieveYourWishlist(String email){
	Customer cus=this.customerRepository.findByEmail(email).get();
	List<Wishlist> wishlists=this.wishlistRepository.findByCustomer(cus);
	return wishlists.stream().map(e->WishlistTransform.transformToWishlistDto(e))
			.collect(Collectors.toList());
			
}
}
