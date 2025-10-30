package com.jpd.web.transform;

import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.model.RememberWord;

public class RememberTransform {
public static RememberWordDto toRememberWordDto(RememberWord re)
{
return	RememberWordDto.builder().
	rwId(re.getId())
	.word(re.getWord())
	.description(re.getDescription())
	.meaning(re.getMeaning())
	.build();
	}

public static RememberWord toRememberWord(RememberWordDto rememberWordDto) {
	// TODO Auto-generated method stub
	return RememberWord.builder()
	.meaning(rememberWordDto.getMeaning())
	.word(rememberWordDto.getWord())
	.description(rememberWordDto.getDescription()).build();

}
}
