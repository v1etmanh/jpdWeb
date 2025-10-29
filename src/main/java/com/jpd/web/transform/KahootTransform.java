package com.jpd.web.transform;

import com.jpd.web.dto.KahootDto;
import com.jpd.web.model.KahootListFunction;

public class KahootTransform {
public static KahootDto transformToKahootDto(KahootListFunction kh)
{int numberQuestion=kh.getModuleContent()==null?0:kh.getModuleContent().size();
  KahootDto k=KahootDto.builder()
		  .createDate(kh.getCreateDate())
		  .title(kh.getTitle())
		  .id(kh.getKahootId())
		  .numberQuestion(numberQuestion)
		  .build();
  return k;
	
	}
}
