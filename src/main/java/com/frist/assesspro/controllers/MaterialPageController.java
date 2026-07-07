package com.frist.assesspro.controllers;


import com.frist.assesspro.dto.material.SectionDTO;
import com.frist.assesspro.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.InputStream;
import java.util.List;

@Controller
@RequestMapping("/materials")
@RequiredArgsConstructor
public class MaterialPageController {

    private final MaterialService materialService;

    @GetMapping
    public String viewMaterials(Model model) {
        List<SectionDTO> sections = materialService.getAllActiveSections();
        model.addAttribute("sections", sections);
        return "materials/public-page";
    }

    // endpoint для получения PDF (через поток)
    @GetMapping("/pdf/{materialId}")
    public ResponseEntity<InputStreamResource> getPdf(@PathVariable Long materialId) throws Exception {
        InputStream pdfStream = materialService.getPdfInputStream(materialId);
        // materialService.getMaterialById(materialId) для имени файла
        // но здесь нам не нужно имя, просто отдаём поток
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
    }
}