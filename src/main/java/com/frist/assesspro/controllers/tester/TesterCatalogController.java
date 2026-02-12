package com.frist.assesspro.controllers.tester;

import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.dto.test.TestInfoDTO;
import com.frist.assesspro.service.TestPassingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/tester/catalog")
@RequiredArgsConstructor
@Slf4j
public class TesterCatalogController {

    private final TestPassingService testPassingService;

    /**
     * Каталог категорий
     */
    @GetMapping
    public String catalogCategories(Model model) {
        log.info("Открытие каталога категорий");

        List<CategoryDTO> categories = testPassingService.getAvailableCategories();
        model.addAttribute("categories", categories);

        return "tester/category-catalog";
    }


    /**
     * Тесты в категории
     */
    @GetMapping("/category/{categoryId}")
    public String catalogTestsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String categoryName,
            Model model) {

        log.info("Открытие тестов категории ID: {}, страница: {}, размер: {}", categoryId, page, size);

        // ✅ ИСПОЛЬЗУЕМ МЕТОД ДЛЯ ТЕСТОВ ПО КАТЕГОРИИ С ПАГИНАЦИЕЙ
        Page<TestInfoDTO> testsPage = testPassingService
                .getAvailableTestsByCategoryDTOPaginated(categoryId, page, size);

        // Получаем название категории, если не передано
        if (categoryName == null && !testsPage.getContent().isEmpty()) {
            categoryName = testsPage.getContent().get(0).getCategoryName();
        }

        model.addAttribute("tests", testsPage.getContent());
        model.addAttribute("testsPage", testsPage);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categoryName", categoryName);
        model.addAttribute("currentPage", testsPage.getNumber());
        model.addAttribute("totalPages", testsPage.getTotalPages());
        model.addAttribute("totalItems", testsPage.getTotalElements());

        return "tester/test-catalog";
    }

    @GetMapping("/all-tests")
    public String allTestsCatalog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {

        log.info("Открытие каталога всех тестов, страница: {}, размер: {}", page, size);

        // ✅ ИСПОЛЬЗУЕМ МЕТОД ДЛЯ ВСЕХ ТЕСТОВ С ПАГИНАЦИЕЙ
        Page<TestInfoDTO> testsPage = testPassingService.getAllAvailableTestsDTOPaginated(page, size);

        model.addAttribute("tests", testsPage.getContent());
        model.addAttribute("testsPage", testsPage);
        model.addAttribute("currentPage", testsPage.getNumber());
        model.addAttribute("totalPages", testsPage.getTotalPages());
        model.addAttribute("totalItems", testsPage.getTotalElements());
        model.addAttribute("showAll", true);

        return "tester/all-tests-catalog";
    }
}