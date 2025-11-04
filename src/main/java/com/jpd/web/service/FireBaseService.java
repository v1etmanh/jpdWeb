package com.jpd.web.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.PendingImage;
import com.jpd.web.model.Status;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.repository.PendingImgRepository;
//service to handle storage img or pdf 
@Service
public class FireBaseService {
	 @Autowired
	    private StorageClient storageClient;
	    
	    public String uploadFile(MultipartFile file,TypeOfFile moduleContent) throws IOException {
	        try {
	      
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
	           
	            return downloadUrl;
	            
	        } catch (Exception e) {
	            throw new FileUploadException("Failed to upload file: " + e.getMessage(), e);
	        }
	    }
	    public String deleteImgByUrl(String url) {
	        try {
	            // 1️⃣ Lấy bucket hiện tại
	            Bucket bucket = storageClient.bucket();

	            // 2️⃣ Từ URL public -> tách ra đường dẫn file trong bucket
	            // URL dạng: https://firebasestorage.googleapis.com/v0/b/your-bucket-name/o/FOLDER%2FfileName.png?alt=media
	            // Ta cần lấy "FOLDER/fileName.png"
	            String prefix = "https://firebasestorage.googleapis.com/v0/b/" + bucket.getName() + "/o/";
	            String suffix = "?alt=media";
	            String objectName = url.substring(prefix.length(), url.indexOf(suffix));
	            objectName = objectName.replace("%2F", "/"); // decode lại

	            // 3️⃣ Lấy đối tượng Blob (file) từ bucket
	            Blob blob = bucket.get(objectName);
	            if (blob == null) {
	                return "❌ File not found in Firebase Storage.";
	            }

	            // 4️⃣ Xóa file
	            boolean deleted = blob.delete();
	            if (deleted) {
	                return "✅ File deleted successfully.";
	            } else {
	                return "⚠️ File could not be deleted (might not exist).";
	            }

	        } catch (Exception e) {
	            e.printStackTrace();
	            return "❌ Error deleting file: " + e.getMessage();
	        }
	    }
	    @Autowired
	    private PendingImgRepository pendingImageRepository;
	    @Scheduled(cron = "0 0 3 * * ?")
	    public void cleanPendingImages() {
	        List<PendingImage> oldImages = pendingImageRepository.findByStatus(Status.PENDING);
	        for (PendingImage img : oldImages) {
	            deleteImgByUrl(img.getUrl());
	            pendingImageRepository.delete(img);
	        }
	         }
	    
	    public byte[] getFileFromUrl(String url) throws IOException {
	        try {
	            if (!StringUtils.hasText(url)) {
	                throw new IllegalArgumentException("URL không hợp lệ");
	            }
	            
	            Bucket bucket = storageClient.bucket();
	            
	            // Tách path file từ URL
	            String prefix = "https://firebasestorage.googleapis.com/v0/b/" + bucket.getName() + "/o/";
	            
	            if (!url.contains(prefix)) {
	                throw new IllegalArgumentException("URL không phải từ Firebase Storage");
	            }
	            
	            String objectName = url.substring(prefix.length());
	            
	            // Xóa query parameter
	            if (objectName.contains("?")) {
	                objectName = objectName.substring(0, objectName.indexOf("?"));
	            }
	            
	            // Decode URL encoding
	            objectName = objectName.replace("%2F", "/");
	            
	            // Lấy file từ Firebase
	            Blob blob = bucket.get(objectName);
	            
	            if (blob == null) {
	                throw new IOException("File không tồn tại: " + objectName);
	            }
	            
	            return blob.getContent();
	            
	        } catch (Exception e) {
	            throw new IOException("Lỗi tải file: " + e.getMessage(), e);
	        }
	    }
}
