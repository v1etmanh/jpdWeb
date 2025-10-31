package com.jpd.web.filter;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.repository.CustomerRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Component
@Order(1)
public class CreatorFilter extends OncePerRequestFilter {
@Autowired
private CustomerRepository customerRepository;
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
                
              
                    Creator creator = customer.getCreator();
                    
                    if (creator == null||creator.isBan()) {
                        sendError(response, 403, "Creator profile does not exist");
                        return;
                    }
                    
                    // Set Creator vào request để Service dùng
                    request.setAttribute("creatorId", creator.getCreatorId());
                
                
                // Set Customer và email vào request
               
                
                log.debug("User validated: {}", email);
            }
            
        } catch (Exception e) {
            log.error("Error in CreatorValidationFilter: {}", e.getMessage(), e);
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
// Override để chỉ áp dụng cho một số URL
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
	    String path = request.getRequestURI();
	    // Bỏ qua các request không bắt đầu bằng /api/creator/
	   boolean sh= path.startsWith("/api/creator/")||
	    		path.equals("/api/quiz/create");
	   return !sh;
	}

}
