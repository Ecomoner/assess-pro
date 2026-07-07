package com.frist.assesspro.mapper;


import com.frist.assesspro.dto.material.MaterialDTO;
import com.frist.assesspro.entity.Material;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MaterialMapper {

    MaterialDTO  materialToMaterialDTO(Material material);
    Material materialDTOToMaterial(MaterialDTO materialDTO);
}
