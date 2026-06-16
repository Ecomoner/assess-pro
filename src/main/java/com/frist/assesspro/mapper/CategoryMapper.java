package com.frist.assesspro.mapper;

import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "testsCount", expression = "java(category.getTests() != null ? (long) category.getTests().size() : 0L)")
    @Mapping(target = "publishedTestsCount", expression = "java(category.getTests() != null ? " +
            "category.getTests().stream().filter(t -> Boolean.TRUE.equals(t.getIsPublished())).count() : 0L)")
    CategoryDTO toDto(Category category);

    List<CategoryDTO> toDtoList(List<Category> categories);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "tests", ignore = true)
    Category toEntity(CategoryDTO dto);
}