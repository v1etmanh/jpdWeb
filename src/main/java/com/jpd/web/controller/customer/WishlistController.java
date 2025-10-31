package com.jpd.web.controller.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.service.WishlistService;

import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {
@Autowired
private WishlistService wishlistService;
@PostMapping("/{courseId}")
public ResponseEntity<?> addToYourWishlist(@AuthenticationPrincipal Jwt jwt,
		@PathVariable("courseId")long courseId)
{
  this.wishlistService.addWishlist(jwt.getClaimAsString("email"), courseId);	
  return ResponseEntity.status(HttpStatus.CREATED).build();
}
}
