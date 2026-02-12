package com.frist.assesspro.controllers.creator;


import com.frist.assesspro.dto.TestDTO;
import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.dto.test.TestUpdateDTO;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.service.CategoryService;
import com.frist.assesspro.service.TestService;
import com.frist.assesspro.util.PaginationUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/creator")
@RequiredArgsConstructor
@Slf4j
public class CreatorController {

    private final TestService testService;
    private final CategoryService categoryService;

    @GetMapping("/tests")
    public String getAllTests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());

        // Конвертируем строковый статус в Boolean
        Boolean published = null;
        if ("published".equals(status)) published = true;
        if ("draft".equals(status)) published = false;

        // 🔥 ВСЯ ФИЛЬТРАЦИЯ В БД - ОДИН ЗАПРОС
        Page<TestDTO> testsPage;

        if (search != null && !search.trim().isEmpty()) {
            testsPage = testService.searchTestsByCreator(userDetails.getUsername(), search, pageable);
            model.addAttribute("searchTerm", search);
        } else {
            testsPage = testService.getTestsByCreator(userDetails.getUsername(),pageable,published,search,categoryId);
        }

        // Статистика теперь вычисляется на основе отфильтрованных данных
        long publishedTestsCount = testsPage.getContent().stream()
                .filter(TestDTO::isPublished)
                .count();

        long totalQuestionsCount = testsPage.getContent().stream()
                .mapToLong(TestDTO::getQuestionCount)
                .sum();

        // Добавляем атрибуты в модель
        model.addAttribute("tests", testsPage.getContent());
        model.addAttribute("publishedTestsCount", publishedTestsCount);
        model.addAttribute("totalQuestionsCount", totalQuestionsCount);
        model.addAttribute("totalItems", testsPage.getTotalElements());
        model.addAttribute("currentPage", testsPage.getNumber());
        model.addAttribute("totalPages", testsPage.getTotalPages());
        model.addAttribute("status", status);
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sort", sort);
        model.addAttribute("pageSize", size);

        // Категории для фильтра
        List<CategoryDTO> categories = categoryService.getCategoriesByCreator(
                userDetails.getUsername(), PageRequest.of(0, 100)).getContent();
        model.addAttribute("filterCategories", categories);

        // Пагинация
        Map<String, String> params = new HashMap<>();
        params.put("size", String.valueOf(size));
        params.put("sort", sort);
        if (status != null) params.put("status", status);
        if (search != null) params.put("search", search);
        if (categoryId != null) params.put("categoryId", categoryId.toString());

        PaginationUtils.addPaginationAttributes(model, testsPage, "/creator/tests", params);

        return "creator/test-list";
    }

    @PostMapping("/tests/new")
    @Transactional
    public String createTest(
            @Valid @ModelAttribute TestDTO testDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибки валидации: " + bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("test", testDTO);
            return "redirect:/creator/tests/new";
        }

        try {
            Test createdTest = testService.createTest(testDTO, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Тест '" + createdTest.getTitle() + "' успешно создан!");
            return "redirect:/creator/tests";
        } catch (Exception e) {
            log.error("Ошибка при создании теста", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при создании теста: " + e.getMessage());
            redirectAttributes.addFlashAttribute("test", testDTO);
            return "redirect:/creator/tests/new";
        }
    }


    @GetMapping("/tests/edit/{id}")
    public String showEditTestForm(
            @PathVariable Long id,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {

        return testService.getTestForEdit(id, userDetails.getUsername())
                .map(test -> {

                    TestUpdateDTO dto = new TestUpdateDTO();
                    dto.setId(test.getId());
                    dto.setTitle(test.getTitle());
                    dto.setDescription(test.getDescription());
                    dto.setTimeLimitMinutes(test.getTimeLimitMinutes());
                    if (test.getCategory() != null) {
                        dto.setCategoryId(test.getCategory().getId());
                    }

                    model.addAttribute("test", dto);  // ← DTO в модель
                    model.addAttribute("action", "edit");

                    // Список категорий для выбора
                    List<CategoryDTO> categories = categoryService.getCategoriesByCreator(
                            userDetails.getUsername(), PageRequest.of(0, 100)).getContent();
                    model.addAttribute("categories", categories);

                    return "creator/test-form";
                })
                .orElse("redirect:/creator/tests?error=test_not_found");
    }

    @PostMapping("/tests/update/{id}")
    @Transactional
    public String updateTest(
            @PathVariable Long id,
            @Valid @ModelAttribute("test") TestUpdateDTO testUpdateDTO,  // ← DTO
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибки валидации: " + bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("test", testUpdateDTO);
            return "redirect:/creator/tests/edit/" + id;
        }

        try {
            Test updatedTest = testService.updateTest(id, testUpdateDTO, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Тест '" + updatedTest.getTitle() + "' успешно обновлен!");
            return "redirect:/creator/tests";
        } catch (Exception e) {
            log.error("Ошибка при обновлении теста", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при обновлении теста: " + e.getMessage());
            redirectAttributes.addFlashAttribute("test", testUpdateDTO);
            return "redirect:/creator/tests/edit/" + id;
        }
    }

    @PostMapping("/tests/publish/{id}")
    public String publishTest(@PathVariable Long id,
                              @RequestParam boolean publish,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes){
        try {
            Test test = testService.switchPublishStatus(id,userDetails.getUsername(),publish);
            String message = publish ? "опубликован" : "снят с публикации";
            redirectAttributes.addFlashAttribute("successMessage",
                    "Тест '" + test.getTitle() + "' успешно " + message + "!");
        } catch (Exception e) {
            log.error("Ошибка при изменении статуса теста", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка: " + e.getMessage());
        }
        return "redirect:/creator/tests";
    }

    @PostMapping("/tests/delete/{id}")
    public String deleteTest(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes){
        try {
            testService.deleteTest(id,userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Тест  успешно удален!");
            return "redirect:/creator/tests";
        } catch (Exception e) {
            log.error("Ошибка при удалении теста", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при удалении: " + e.getMessage());
            return "redirect:/creator/tests";
        }
    }

    @GetMapping("/tests/new")
    public String showCreateForm(Model model,@AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("action", "create");
        model.addAttribute("test", new Test());

        // Добавляем список категорий для выбора
        List<CategoryDTO> categories = categoryService.getCategoriesByCreator(
                userDetails.getUsername(), PageRequest.of(0, 100)).getContent();
        model.addAttribute("categories", categories);

        return "creator/test-form";
    }

    @GetMapping("/tests/{id}/quick-stats")
    @ResponseBody
    public Map<String, Object> getQuickStats(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, Object> stats = new HashMap<>();

        try {
            Test test = testService.getTestById(id, userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Тест не найден"));

            stats.put("testId", test.getId());
            stats.put("testTitle", test.getTitle());
            stats.put("questionCount", test.getQuestionCount());
            stats.put("published", test.getIsPublished());

            // Можно добавить быструю статистику
            // int attemptsCount = test.getAttempts() != null ? test.getAttempts().size() : 0;
            // stats.put("attemptsCount", attemptsCount);

        } catch (Exception e) {
            log.error("Ошибка при получении быстрой статистики", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    @GetMapping("/tests/{id}/preview")
    public String previewTestAsTester(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        return testService.getTestForPreview(id, userDetails.getUsername())
                .map(testTakingDTO -> {
                    model.addAttribute("testTakingDTO", testTakingDTO);
                    model.addAttribute("isPreview", true); // Флаг для шаблона
                    return "creator/test-preview";
                })
                .orElse("redirect:/creator/tests?error=test_not_found");
    }

}
