package com.jpd.web.service;

import java.io.IOException;
import java.util.Optional;

import com.jpd.web.dto.ModerationResponse;
import com.jpd.web.exception.BusinessException;
import com.jpd.web.exception.ModerateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.exception.ApiException;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.PendingImage;
import com.jpd.web.model.Status;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.PendingImgRepository;
import com.jpd.web.service.utils.ValidationResources;

@Service
public class FileUploadService {

	@Autowired
	private PendingImgRepository pendingImgRepository; 
	@Autowired
	private ValidationResources validationResources;
	@Autowired
	private FireBaseService fireBaseService;
    @Autowired
    private  ModerationClient moderationClient;

	public String saveImgIntoFirebase(long creatorId,MultipartFile img,TypeOfFile type) throws IllegalAccessException, IOException {
		    
		Creator creator=	validationResources.validateCreatorExists(creatorId);

		try {
            if( type.name().equalsIgnoreCase("IMG") ) {
                moderateImg(img);
            }
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
    public void  moderateImg(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        ModerationResponse res = moderationClient.check(bytes);
        if ("BLOCKED".equalsIgnoreCase(res.getDecision())) {
           throw new ModerateException() ;
        }
    }
}
