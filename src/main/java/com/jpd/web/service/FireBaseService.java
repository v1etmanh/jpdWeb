package com.jpd.web.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfFile;
//service to handle storage img or pdf 
@Service
public class FireBaseService {
	 @Autowired
	    private StorageClient storageClient;
	    
	    public String uploadFile(MultipartFile file,TypeOfFile moduleContent) throws IOException {
	        try {
	        	System.out.print("hello22");
	            // Lấy bucket từ StorageClient
	            Bucket bucket = storageClient.bucket();
	            
	            // Tạo tên file unique
	            String fileName = moduleContent.toString()+"/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
	            
	            // Upload file
	            Blob blob = bucket.create(fileName, file.getBytes(), file.getContentType());
	            
	            // Tạo public URL
	            String downloadUrl = String.format(
	                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
	                bucket.getName(),
	                fileName.replace("/", "%2F")
	            );
	            System.out.print(downloadUrl);
	            return downloadUrl;
	            
	        } catch (Exception e) {
	            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
	        }
	    }
}
