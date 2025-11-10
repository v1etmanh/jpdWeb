package com.jpd.web.transform;

import java.awt.desktop.UserSessionEvent.Reason;

import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices.RememberMeTokenAlgorithm;

import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.model.Language;
import com.jpd.web.model.RememberWord;

public class RememberTransform {
public static RememberWordDto toRememberWordDto(RememberWord re)
{
return	RememberWordDto.builder().
	rwId(re.getId())
	.word(re.getWord())
	.description(re.getDescription())
	.meaning(re.getMeaning())
	.example(re.getExample())
	.synonyms(re.getSynonyms())
	.voteCount(re.getVote().size())
	.language(re.getLanguage())
	.build();
	}

public static RememberWord toRememberWord(RememberWordDto rememberWordDto) {
	// TODO Auto-generated method stub
	return RememberWord.builder()
	.meaning(rememberWordDto.getMeaning())
	.word(rememberWordDto.getWord())
	.description(rememberWordDto.getDescription())
	.example(rememberWordDto.getExample())
	.synonyms(rememberWordDto.getSynonyms())
	.language(rememberWordDto.getLanguage())
	.build();

}
}
