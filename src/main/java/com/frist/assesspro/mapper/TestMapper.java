package com.frist.assesspro.mapper;


import com.frist.assesspro.dto.TestDTO;
import com.frist.assesspro.entity.Test;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {CategoryMapper.class, QuestionMapper.class}, // если понадобятся вложенные мапперы
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TestMapper {

    @Mapping(target = "published",source = "isPublished")
    @Mapping(target = "questionCount", expression = "java((long) test.getQuestionCount())")
    @Mapping(target = "categoryId",source = "category.id")
    @Mapping(target = "categoryName",source = "category.name")
    @Mapping(target = "createdAt",source = "createdAt")
    @Mapping(target = "retryCooldownHours", source = "retryCooldownHours")
    @Mapping(target = "retryCooldownDays", source = "retryCooldownDays")
    @Mapping(target = "timeLimitMinutes", source = "timeLimitMinutes")
    TestDTO toDto(Test test);

    List<TestDTO> toDtoList(List<Test> tests);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "isPublished", constant = "false")
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "attempts", ignore = true)
    Test toEntity(TestDTO dto);
}
