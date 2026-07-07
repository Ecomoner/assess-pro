package com.frist.assesspro.mapper;

import com.frist.assesspro.dto.material.MaterialDTO;
import com.frist.assesspro.dto.material.SectionDTO;
import com.frist.assesspro.dto.material.TestLinkDTO;
import com.frist.assesspro.entity.Material;
import com.frist.assesspro.entity.Section;
import com.frist.assesspro.entity.Test;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",uses = {TestMapper.class})
public interface SectionMapper {

    @Mapping(target = "tests", source = "tests")
    @Mapping(target = "materials", source = "materials")
    SectionDTO toDto(Section section);

    MaterialDTO toDto(Material material);
    TestLinkDTO toTestLinkDto(Test test);
}
