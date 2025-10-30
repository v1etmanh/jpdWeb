package com.jpd.web.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;

import jakarta.annotation.PostConstruct;

@Configuration
public class FireBaseConfig {
	  @Value("${firebase.config.path:firebase-service-account.json}")
	    private String firebaseConfigPath;
	  
	    @PostConstruct
	    public void init() throws IOException {
	    	System.out.print(firebaseConfigPath);
	        // Sử dụng ClassPathResource thay vì FileInputStream
	        ClassPathResource resource = new ClassPathResource(firebaseConfigPath);
	        
	        FirebaseOptions options = FirebaseOptions.builder()
	                .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
	                .setStorageBucket("jpdweb-9d3d3.firebasestorage.app") // Từ application.properties
	                .build();

	        if (FirebaseApp.getApps().isEmpty()) {
	            FirebaseApp.initializeApp(options);
	        }
	    }
	    @Bean
	    public StorageClient firebaseStorageClient() {
	        // Đảm bảo FirebaseApp đã được khởi tạo
	        // StorageClient.getInstance() sẽ lấy instance của FirebaseApp mặc định.
	        return StorageClient.getInstance();
	    }
}
