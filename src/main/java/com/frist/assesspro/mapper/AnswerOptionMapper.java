package com.frist.assesspro.mapper;

import com.frist.assesspro.dto.AnswerOptionDTO;
import com.frist.assesspro.entity.AnswerOption;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AnswerOptionMapper {

    AnswerOptionDTO toDto(AnswerOption option);

    List<AnswerOptionDTO> toDtoList(List<AnswerOption> options);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "question", ignore = true)
    AnswerOption toEntity(AnswerOptionDTO dto);
}