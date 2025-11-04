package com.jpd.web.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.KahootDto;
import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Creator;
import com.jpd.web.model.KahootListFunction;
import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.repository.KahootRepository;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.repository.ModuleRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.KahootTransform;

import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.transaction.Transactional;

@Service
public class KahootService {
	@Autowired
	private KahootRepository kahootRepository;
	@Autowired
	private ValidationResources validationResources;
	@Autowired
	private ModuleContentRepository moduleContentRepository;
	public List<KahootDto> retrieveAll(long creatorId){
		Creator c=this.validationResources.validateCreatorExists(creatorId);
		
		return c.getKahootListFunctions().stream().map(e->KahootTransform.transformToKahootDto(e))
				.collect(Collectors.toList());
		
	}
	public KahootListFunction createKahoot(String kahootTitle, long creatorId) {
	Creator c=	validationResources.validateCreatorExists( creatorId);
      
		KahootListFunction kh = KahootListFunction.builder()
            .creator(c)
		.title(kahootTitle)
		.build();
		

		return this.kahootRepository.save(kh);
	}

	@Transactional
	public void deleteKahoot(long creatorId,  long KahootId)
		 {
		
	KahootListFunction  khl=validationResources.validateKahootOwnership(KahootId, creatorId);
		
		moduleContentRepository.deleteByKahootId(KahootId);

		kahootRepository.deleteById(KahootId);
	}
	@Transactional
	public void updateKahootTitle(long  id, long kahootId,String title) {
	   KahootListFunction c=  validationResources.validateKahootOwnership(kahootId,id);
	   c.setTitle(title);
	   this.kahootRepository.save(c);
	}
	public List<ModuleContent> retrieveData(long creatorId,long kahootId)
	{
		KahootListFunction k=this.validationResources.validateKahootOwnership(kahootId, creatorId);
		return k.getModuleContent();
	}
}
