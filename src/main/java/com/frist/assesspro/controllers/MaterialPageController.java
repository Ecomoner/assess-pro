package com.frist.assesspro.controllers;


import com.frist.assesspro.dto.material.SectionDTO;
import com.frist.assesspro.service.MaterialService;
import com.frist.assesspro.service.TestPassingService;
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
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/materials")
@RequiredArgsConstructor
public class MaterialPageController {

    private final MaterialService materialService;
    private final TestPassingService testPassingService;

    @GetMapping
    public String viewMaterials(Model model, Principal principal) {

        if (principal != null && testPassingService.hasActiveAttempt(principal.getName())) {
            // Перенаправляем на дашборд тестировщика с сообщением
            return "redirect:/tester/dashboard?materialAccessDenied";
        }
        List<SectionDTO> sections = materialService.getAllActiveSections();
        model.addAttribute("sections", sections);
        return "materials/public-page";
    }

    // endpoint для получения PDF (через поток)
    @GetMapping("/pdf/{materialId}")
    public ResponseEntity<InputStreamResource> getPdf(@PathVariable Long materialId) throws Exception {
        InputStream pdfStream = materialService.getPdfInputStream(materialId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
    }
}