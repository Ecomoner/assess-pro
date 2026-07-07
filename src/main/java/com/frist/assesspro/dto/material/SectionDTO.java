package com.frist.assesspro.dto.material;

import com.frist.assesspro.dto.TestDTO;
import lombok.Data;

import java.util.List;

@Data
public class SectionDTO {
    private Long id;
    private String title;
    private String description;
    private int orderIndex;
    private boolean active;
    private List<MaterialDTO> materials;
    private List<TestDTO> tests;
    private boolean hasPdf;
}
