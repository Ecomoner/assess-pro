package com.frist.assesspro.mapper;


import com.frist.assesspro.dto.material.MaterialDTO;
import com.frist.assesspro.entity.Material;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring")
public interface MaterialMapper {

    @Mapping(target = "type", expression = "java(material.getType().name())")
    @Mapping(target = "hasVideo", expression = "java(material.getType() == Material.MaterialType.VIDEO_FILE)")
    MaterialDTO toDto(Material material);

}
