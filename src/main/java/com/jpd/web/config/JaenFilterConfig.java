package com.jpd.web.config;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jpd.web.filter.CreatorFilter;

@Configuration
public class JaenFilterConfig {
//	@Bean
//	public FilterRegistrationBean<CreatorFilter> loggingFilter(){
//	    FilterRegistrationBean<CreatorFilter> registrationBean 
//	      = new FilterRegistrationBean<>();
//	        
//	    registrationBean.setFilter(new CreatorFilter());
//	    registrationBean.addUrlPatterns("/api/creator/*");
//	    registrationBean.setOrder(2);
//	        
//	    return registrationBean;    
//	}
}
