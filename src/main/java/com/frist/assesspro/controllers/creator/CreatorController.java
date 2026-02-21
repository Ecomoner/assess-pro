package com.frist.assesspro.controllers.creator;


import com.frist.assesspro.dto.DashboardStatsDTO;
import com.frist.assesspro.dto.TestDTO;
import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.dto.statistics.TesterAttemptDTO;
import com.frist.assesspro.dto.test.TestUpdateDTO;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.UserRepository;
import com.frist.assesspro.service.*;
import com.frist.assesspro.service.export.StatisticsExportService;
import com.frist.assesspro.util.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/creator")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Создатель",description = "API для создателей")
public class CreatorController {

    private final TestService testService;
    private final CategoryService categoryService;
    private final StatisticsExportService statisticsExportService;
    private final TesterStatisticsService testerStatisticsService;
    private final DashboardService dashboardService;
    private final UserService userService;
    private final UserRepository userRepository;

    @ModelAttribute("currentUri")
    public String getCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @Operation(summary = "Получение всех тестов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/tests")
    public String getAllTests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long creatorId,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());


        Page<TestDTO> testsPage = testService.getAllTestsForCreator(
                userDetails.getUsername(), pageable, status, search, categoryId, creatorId);

        // Создаем карту владельцев для отображения кнопок
        Map<Long, Boolean> ownershipMap = new HashMap<>();
        for (TestDTO test : testsPage.getContent()) {
            boolean isOwner = test.getCreatorUsername() != null &&
                    test.getCreatorUsername().equals(userDetails.getUsername());
            ownershipMap.put(test.getId(), isOwner);
        }

        // Добавляем в модель список всех создателей для фильтра
        List<User> creators = userRepository.findAllCreators();
        model.addAttribute("creators", creators);
        model.addAttribute("selectedCreatorId", creatorId);

        // Статистика (оставляем как есть)
        long publishedTestsCount = testsPage.getContent().stream()
                .filter(TestDTO::isPublished)
                .count();

        long totalQuestionsCount = testsPage.getContent().stream()
                .mapToLong(TestDTO::getQuestionCount)
                .sum();

        // Добавляем атрибуты в модель
        model.addAttribute("tests", testsPage.getContent());
        model.addAttribute("ownershipMap", ownershipMap);
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
        List<CategoryDTO> categories = categoryService.getAllCategories(PageRequest.of(0, 100)).getContent();

        model.addAttribute("filterCategories", categories);

        // Пагинация
        Map<String, String> params = new HashMap<>();
        params.put("size", String.valueOf(size));
        params.put("sort", sort);
        if (status != null) params.put("status", status);
        if (search != null) params.put("search", search);
        if (categoryId != null) params.put("categoryId", categoryId.toString());
        if (creatorId != null) params.put("creatorId", creatorId.toString());

        PaginationUtils.addPaginationAttributes(model, testsPage, "/creator/tests", params);

        return "creator/test-list";
    }

    @Operation(summary = "Создание теста")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/tests/new")
    @Transactional
    public String createTest(
            @Valid @ModelAttribute("test") TestDTO testDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        log.info("Создание теста: {}, часы: {}, дни: {}",
                testDTO.getTitle(), testDTO.getRetryCooldownHours(), testDTO.getRetryCooldownDays());

        if (bindingResult.hasErrors()) {
            log.warn("Ошибки валидации: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("errorMessage", "Проверьте правильность заполнения формы");
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
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
            redirectAttributes.addFlashAttribute("test", testDTO);
            return "redirect:/creator/tests/new";
        }
    }

    @Operation(summary = "Форма редактирования теста")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/tests/edit/{id}")
    @Transactional(readOnly = true)
    public String showEditTestForm(
            @PathVariable Long id,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

                    Test test = testService.getTestByIdWithoutOwnershipCheck(id);
                    if (!test.getCreatedBy().getUsername().equals(userDetails.getUsername())) {
                        redirectAttributes.addFlashAttribute("errorMessage",
                                "У вас нет прав на редактирование этого теста");
                        return "redirect:/creator/tests";
                    }
                    // Конвертируем Entity в DTO
                    TestDTO testDTO = testService.convertToDTO(test);

                    model.addAttribute("test", testDTO);
                    model.addAttribute("formAction", "/creator/tests/update/" + id);
                    model.addAttribute("action", "edit");

                    // Список категорий для выбора
        List<CategoryDTO> categories = categoryService.getAllCategories(PageRequest.of(0, 100)).getContent();

        model.addAttribute("categories", categories);

                    return "creator/test-form";

    }

    @Operation(summary = "Обновление теста")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/tests/update/{id}")
    @Transactional
    public String updateTest(
            @PathVariable Long id,
            @Valid @ModelAttribute("test") TestDTO testDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (!testService.isTestOwner(id, userDetails.getUsername())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "У вас нет прав на редактирование этого теста");
            return "redirect:/creator/tests";
        }

        if (bindingResult.hasErrors()) {
            log.warn("Ошибки валидации при обновлении: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("errorMessage", "Проверьте правильность заполнения формы");
            redirectAttributes.addFlashAttribute("test", testDTO);
            return "redirect:/creator/tests/edit/" + id;
        }

        try {
            // Конвертируем DTO в UpdateDTO
            TestUpdateDTO updateDTO = new TestUpdateDTO();
            updateDTO.setTitle(testDTO.getTitle());
            updateDTO.setDescription(testDTO.getDescription());
            updateDTO.setTimeLimitMinutes(testDTO.getTimeLimitMinutes());
            updateDTO.setRetryCooldownHours(testDTO.getRetryCooldownHours());
            updateDTO.setRetryCooldownDays(testDTO.getRetryCooldownDays());
            updateDTO.setCategoryId(testDTO.getCategoryId());

            Test updatedTest = testService.updateTest(id, updateDTO, userDetails.getUsername());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Тест '" + updatedTest.getTitle() + "' успешно обновлен!");
            return "redirect:/creator/tests";
        } catch (Exception e) {
            log.error("Ошибка при обновлении теста", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
            redirectAttributes.addFlashAttribute("test", testDTO);
            return "redirect:/creator/tests/edit/" + id;
        }
    }

    @Operation(summary = "Публикация теста")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/tests/publish/{id}")
    public String publishTest(@PathVariable Long id,
                              @RequestParam boolean publish,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes){

        if (!testService.isTestOwner(id, userDetails.getUsername())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "У вас нет прав на изменение статуса этого теста");
            return "redirect:/creator/tests";
        }
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

    @Operation(summary = "Удаление теста")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/tests/delete/{id}")
    public String deleteTest(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes){

        if (!testService.isTestOwner(id, userDetails.getUsername())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "У вас нет прав на удаление этого теста");
            return "redirect:/creator/tests";
        }
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

    @Operation(summary = "Форма создания теста")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/tests/new")
    public String showCreateForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        TestDTO testDTO = new TestDTO();
        testDTO.setTimeLimitMinutes(0);
        testDTO.setRetryCooldownHours(0);
        testDTO.setRetryCooldownDays(0);

        model.addAttribute("test", testDTO);
        model.addAttribute("formAction", "/creator/tests/new");
        model.addAttribute("action", "create");

        // Список категорий для выбора
        List<CategoryDTO> categories = categoryService.getAllCategories(PageRequest.of(0, 100)).getContent();

        model.addAttribute("categories", categories);

        return "creator/test-form";
    }

    @Operation(summary = "Быстрая статистика")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
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

        } catch (Exception e) {
            log.error("Ошибка при получении быстрой статистики", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    @Operation(summary = "Превью теста без прохождения")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
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

    @Operation(summary = "Экспот статистики теста в PDF файл")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/tests/{testId}/export")
    public ResponseEntity<byte[]> exportTestStatistics(
            @PathVariable Long testId,
            @RequestParam(required = false) String testerUsername,
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Экспорт статистики теста ID: {}, тестировщик: {}, категория: {}",
                testId, testerUsername, categoryId);

        try {
            // Загружаем тест со всеми необходимыми данными
            Test test = testService.getTestWithAllDataWithoutOwnershipCheck(testId)
                    .orElseThrow(() -> new RuntimeException("Тест не найден"));

            byte[] pdfContent = statisticsExportService.generateTestStatisticsPDF(
                    test, testerUsername, categoryId);

            String filename = "statistics_test_" + testId;
            if (testerUsername != null) {
                filename += "_" + testerUsername;
            }
            filename += "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfContent.length)
                    .body(pdfContent);

        } catch (Exception e) {
            log.error("Ошибка при экспорте статистики", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Страница экспорта в PDF")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/tests/{testId}/export-page")
    public String showExportPage(
            @PathVariable Long testId,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {

        Test test = testService.getTestByIdWithoutOwnershipCheck(testId);

        // Получаем список УНИКАЛЬНЫХ тестировщиков для фильтра
        List<User> testers = testerStatisticsService.getDistinctTestersByTest(testId, userDetails.getUsername());

        model.addAttribute("test", test);
        model.addAttribute("testers", testers);  // ← теперь это список User, а не TesterAttemptDTO
        model.addAttribute("categories", categoryService.getAllCategories(PageRequest.of(0, 100)).getContent());

        return "creator/export-page";
    }

    @Operation(summary = "Дашборд создателя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('CREATOR')")
    public String creatorDashboard(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            DashboardStatsDTO stats = dashboardService.getCreatorStats(username);
            User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
            String firstName = user != null ? user.getFirstName() : userDetails.getUsername();

            // Получаем последние 5 попыток тестировщиков
            List<TesterAttemptDTO> recentAttempts = testerStatisticsService
                    .getRecentTestAttemptsForCreator(username, 5);

            // Получаем общее количество уникальных тестировщиков
            long totalTesters = testerStatisticsService.getTotalTesters();

            model.addAttribute("username", username);
            model.addAttribute("role", auth.getAuthorities().iterator().next().getAuthority());
            model.addAttribute("message", "Добро пожаловать в панель создателя тестов!");
            model.addAttribute("stats", stats);
            model.addAttribute("firstName", firstName);
            model.addAttribute("recentAttempts", recentAttempts);
            model.addAttribute("totalTesters", totalTesters);

            return "creator/dashboard";
        } catch (Exception e) {
            log.error("Ошибка при загрузке дашборда создателя", e);
            model.addAttribute("errorMessage", "Ошибка при загрузке данных: " + e.getMessage());
            return "creator/dashboard";
        }
    }


}
