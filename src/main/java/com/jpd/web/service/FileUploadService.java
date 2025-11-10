package com.jpd.web.service;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.exception.ApiException;
import com.jpd.web.exception.FileUploadException;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.PendingImage;
import com.jpd.web.model.Status;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.PendingImgRepository;
import com.jpd.web.service.utils.ValidationResources;

import jakarta.transaction.Transactional;

@Service
public class FileUploadService {

	@Autowired
	private PendingImgRepository pendingImgRepository; 
	@Autowired
	private ValidationResources validationResources;
	@Autowired
	private FireBaseService fireBaseService;
	public String saveImgIntoFirebase(long creatorId,MultipartFile img,TypeOfFile type) throws IllegalAccessException {
		    
		Creator creator=	validationResources.validateCreatorExists(creatorId);
		try {
			String url= this.fireBaseService.uploadFile(img, type);
			PendingImage p=new PendingImage();
			p.setCreatorId(creator.getCreatorId());
			p.setStatus(Status.PENDING);
			p.setUrl(url);
			this.pendingImgRepository.save(p);
			return url;
		}
		catch (Exception e) {
			// TODO: handle exception
			throw new ApiException("error to save file");
		}
		
	}
	@Transactional
	public void deleteFileByUrl(String url,long creatorId) 
	{
		Creator creator=	validationResources.validateCreatorExists(creatorId);
		Optional< PendingImage> pe=this.pendingImgRepository.deleteByCreatorIdAndUrl(creatorId, url);
		if(pe.isEmpty())throw new FileUploadException("url này ko tồn tại");
			this.fireBaseService.deleteImgByUrl(url);
	}
	
}
