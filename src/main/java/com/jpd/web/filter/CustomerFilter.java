package com.jpd.web.filter;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Enrollment;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.EnrollmentRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

public class CustomerFilter extends OncePerRequestFilter {
	@Autowired
	private CustomerRepository customerRepository;
	@Autowired
	private EnrollmentRepository enrollmentRepository;
		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
				throws ServletException, IOException {
			// TODO Auto-generated method stub
			try {
	            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	            
	            if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
	                Jwt jwt = (Jwt) authentication.getPrincipal();
	                String email = jwt.getClaimAsString("email");
	                
	                if (email == null || email.isEmpty()) {
	                    sendError(response, 401, "Email not found in token");
	                    return;
	                }
	                
	                // Validate Customer
	                Optional<Customer> customerOpt = customerRepository.findByEmail(email);
	                if (customerOpt.isEmpty()) {
	                    sendError(response, 401, "This account does not exist");
	                    return;
	                }
	                
	                Customer customer = customerOpt.get();
	                
	              
	                String uri = request.getRequestURI(); // /api/customer/learning/42
	                String[] parts = uri.split("/");
	                String courseId = parts[4];
	                long courseid=Long.parseLong(courseId);
	                Optional<Enrollment>enr=this.enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(courseid, customer.getCustomerId());
	                
	                if(enr.isEmpty())throw new UnauthorizedException("you have not enrolled");
	                // Set Customer và email vào request
	               
	               request.setAttribute("customerId", customer.getCustomerId());
	               request.setAttribute("courseId", courseId);
	               
	            }
	            
	        } catch (Exception e) {
	           
	            sendError(response, 500, "Internal server error");
	            return;
	        }
	        
	        filterChain.doFilter(request, response);
		}
		
	//
		private void sendError(HttpServletResponse response, int status, String message) 
	            throws IOException {
	        response.setStatus(status);
	        response.setContentType("application/json;charset=UTF-8");
	        response.getWriter().write(
	            String.format("{\"error\": \"%s\", \"status\": %d}", message, status)
	        );
	    }
		@Override
		protected boolean shouldNotFilter(HttpServletRequest request) {
		    String path = request.getRequestURI();
		    // Bỏ qua các request không bắt đầu bằng /api/creator/
		    return !path.startsWith("/api/customer/learning/");
		}
}
