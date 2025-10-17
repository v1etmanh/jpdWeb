package com.jpd.web.transform;

import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.model.RememberWord;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RememberTransform {
    RememberWord toRememberWord(RememberWordDto rememberWordDto);
    RememberWordDto toRememberWordDto(RememberWord rememberWord);
    void UpdateRememberWord(@MappingTarget RememberWord rememberWord,RememberWordDto rememberWordDto);
}
