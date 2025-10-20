package com.jpd.web.transform;

import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.model.RememberWord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.lang.annotation.Target;

@Mapper(componentModel = "spring")
public interface RememberTransform {
    @Mapping(source = "rwId",target = "id")
    RememberWord toRememberWord(RememberWordDto rememberWordDto);
    @Mapping(source = "id",target = "rwId")
    RememberWordDto toRememberWordDto(RememberWord rememberWord);
    @Mapping(source = "rwId",target = "id")
    void UpdateRememberWord(@MappingTarget RememberWord rememberWord,RememberWordDto rememberWordDto);
}
