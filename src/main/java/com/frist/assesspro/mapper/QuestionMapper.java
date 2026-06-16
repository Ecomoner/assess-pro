package com.frist.assesspro.mapper;

import com.frist.assesspro.dto.QuestionDTO;
import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.entity.Category;
import com.frist.assesspro.entity.Question;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {AnswerOptionMapper.class})
public interface QuestionMapper {

    @Mapping(target = "answerOptions", source = "answerOptions")
    QuestionDTO toDto(Question question);

    List<QuestionDTO> toDtoList(List<Question> questions);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "test", ignore = true)
    Question toEntity(QuestionDTO dto);
}