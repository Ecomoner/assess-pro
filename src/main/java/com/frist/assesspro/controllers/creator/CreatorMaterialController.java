package com.frist.assesspro.controllers.creator;


import com.frist.assesspro.dto.material.SectionDTO;
import com.frist.assesspro.dto.material.TestLinkDTO;
import com.frist.assesspro.service.MaterialService;
import com.frist.assesspro.service.TestService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStream;
import java.util.List;

@Controller
@RequestMapping("/creator/materials")
@PreAuthorize("hasRole('CREATOR')")
@RequiredArgsConstructor
public class CreatorMaterialController {

    private final MaterialService materialService;
    private final TestService testService;

    // --------------------------------------------------
    // 1. Страница управления материалами
    // --------------------------------------------------
    @GetMapping
    public String managePage(Model model) {
        // Загружаем все секции (включая неактивные, чтобы создатель видел всё)
        List<SectionDTO> sections = materialService.getAllSections();
        model.addAttribute("sections", sections);

        // Список тестов для привязки: все тесты
        List<TestLinkDTO> availableTests = testService.getAllPublishedTests();
        model.addAttribute("availableTests", availableTests);

        return "creator/materials-management";
    }

    // --------------------------------------------------
    // 2. Создание новой секции
    // --------------------------------------------------
    @PostMapping("/sections")
    public String createSection(@RequestParam String title,
                                @RequestParam(required = false) String description,
                                RedirectAttributes ra) {
        try {
            materialService.createSection(title, description);
            ra.addFlashAttribute("successMessage", "Секция создана");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        return "redirect:/creator/materials";
    }

    // --------------------------------------------------
    // 3. Редактирование секции
    // --------------------------------------------------
    @PostMapping("/sections/{id}/edit")
    public String editSection(@PathVariable Long id,
                              @RequestParam String title,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false, defaultValue = "false") boolean active,
                              RedirectAttributes ra) {
        try {
            materialService.updateSection(id, title, description, active);
            ra.addFlashAttribute("successMessage", "Секция обновлена");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        return "redirect:/creator/materials";
    }

    // --------------------------------------------------
    // 4. Удаление секции (каскадно удалит материалы)
    // --------------------------------------------------
    @PostMapping("/sections/{id}/delete")
    public String deleteSection(@PathVariable Long id, RedirectAttributes ra) {
        try {
            materialService.deleteSection(id);
            ra.addFlashAttribute("successMessage", "Секция удалена");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        return "redirect:/creator/materials";
    }

    // --------------------------------------------------
    // 5. Загрузка PDF в секцию
    // --------------------------------------------------
    @PostMapping("/sections/{sectionId}/pdf/upload")
    public String uploadPdf(@PathVariable Long sectionId,
                            @RequestParam("file") MultipartFile file,
                            RedirectAttributes ra) {
        try {
            materialService.uploadPdfToSection(sectionId, file);
            ra.addFlashAttribute("successMessage", "PDF загружен");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Ошибка загрузки: " + e.getMessage());
        }
        return "redirect:/creator/materials";
    }

    // --------------------------------------------------
    // 6. Удаление PDF-файла из секции
    // --------------------------------------------------
    @PostMapping("/pdf/{materialId}/delete")
    public String deleteMaterial(@PathVariable Long materialId, RedirectAttributes ra) {
        try {
            materialService.deleteMaterial(materialId);
            ra.addFlashAttribute("successMessage", "Файл удалён");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Ошибка удаления: " + e.getMessage());
        }
        return "redirect:/creator/materials";
    }

    // --------------------------------------------------
    // 7. Привязка тестов к секции
    // --------------------------------------------------
    @PostMapping("/sections/{sectionId}/tests/attach")
    public String attachTests(@PathVariable Long sectionId,
                              @RequestParam(required = false) List<Long> testIds,
                              RedirectAttributes ra) {
        try {
            if (testIds != null && !testIds.isEmpty()) {
                materialService.attachTestsToSection(sectionId, testIds);
                ra.addFlashAttribute("successMessage", "Тесты привязаны");
            } else {
                ra.addFlashAttribute("errorMessage", "Не выбрано ни одного теста");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        return "redirect:/creator/materials";
    }

    // --------------------------------------------------
    // 8. Отвязка конкретного теста от секции
    // --------------------------------------------------
    @PostMapping("/sections/{sectionId}/tests/{testId}/detach")
    public String detachTest(@PathVariable Long sectionId,
                             @PathVariable Long testId,
                             RedirectAttributes ra) {
        try {
            materialService.detachTestFromSection(sectionId, testId);
            ra.addFlashAttribute("successMessage", "Тест отвязан");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        return "redirect:/creator/materials";
    }

    @PostMapping("/sections/{sectionId}/video/upload")
    public String uploadVideo(@PathVariable Long sectionId,
                              @RequestParam("file") MultipartFile file,
                              RedirectAttributes ra) {
        try {
            materialService.uploadVideoToSection(sectionId, file);
            ra.addFlashAttribute("successMessage", "Видео загружено");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Ошибка загрузки: " + e.getMessage());
        }
        return "redirect:/creator/materials";
    }
}