package com.frist.assesspro.controllers.tester;

import com.frist.assesspro.dto.*;
import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.dto.test.*;
import com.frist.assesspro.entity.TestAttempt;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.TestAttemptRepository;
import com.frist.assesspro.service.DashboardService;
import com.frist.assesspro.service.TestPassingService;
import com.frist.assesspro.service.UserService;
import com.frist.assesspro.service.metrics.MetricsService;
import com.frist.assesspro.util.TestConstants;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tester")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Тестер",description = "API для тестеров")
public class TesterController {

    private final TestPassingService testPassingService;
    private final DashboardService dashboardService;
    private final TestAttemptRepository testAttemptRepository;
    private final MetricsService metricsService;

    @ModelAttribute("currentUri")
    public String getCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @Operation(summary = "Каталог доступных тестов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/tests")
    public String testCatalog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String search,
            Model model) {

        log.info("Каталог тестов, поиск: '{}', страница: {}, размер: {}", search, page, size);

        Page<TestInfoDTO> testsPage;

        if (search != null && !search.trim().isEmpty()) {
            testsPage = testPassingService.searchTests(search, page, size);
            model.addAttribute("searchTerm", search);
        } else {
            testsPage = testPassingService.getAllAvailableTestsDTOPaginated(page, size);
        }

        model.addAttribute("tests", testsPage.getContent());
        model.addAttribute("testsPage", testsPage);
        model.addAttribute("currentPage", testsPage.getNumber());
        model.addAttribute("totalPages", testsPage.getTotalPages());
        model.addAttribute("totalItems", testsPage.getTotalElements());
        model.addAttribute("pageSize", size);

        return "tester/test-catalog";
    }

    @Operation(summary = "Быстрый поиск")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/tests/search/quick")
    @ResponseBody
    public List<TestInfoDTO> quickSearch(
            @RequestParam String term,
            @RequestParam(defaultValue = "5") int limit) {

        return testPassingService.quickSearchTests(term, limit);
    }

    @Operation(summary = "Начало прохождения теста")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/test/{testId}/start")
    public String startTest(
            @PathVariable Long testId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        Timer.Sample sample = metricsService.startTimer();
        try {
            metricsService.incrementTestsStarted();
            metricsService.incrementActiveUsers();
            TestTakingDTO testTakingDTO = testPassingService.getTestForTaking(testId, userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Тест не найден или недоступен"));


            return "redirect:/tester/attempt/" + testTakingDTO.getAttemptId();

        } catch (Exception e) {
            log.error("Ошибка при начале теста", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Не удалось начать тест: " + e.getMessage());
            return "redirect:/tester/tests";
        }
    }

    @Operation(summary = "Страница прохождения теста")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/attempt/{attemptId}")
    @Transactional(readOnly = true)
    public String takeTest(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            // 🔥 ОДИН ЗАПРОС: проверяем статус попытки
            TestAttempt attempt = testAttemptRepository.findById(attemptId)
                    .orElseThrow(() -> new RuntimeException("Попытка не найдена"));

            if (!attempt.getUser().getUsername().equals(userDetails.getUsername())) {
                throw new RuntimeException("Нет доступа к этой попытке");
            }

            // Если тест завершен - редирект на результаты
            if (attempt.getStatus() != TestAttempt.AttemptStatus.IN_PROGRESS) {
                return "redirect:/tester/attempt/" + attemptId + "/results";
            }

            // Получаем данные для прохождения
            TestTakingDTO testTakingDTO = testPassingService.getTestForTaking(
                            attempt.getTest().getId(), userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Тест не найден"));

            // Проверка наличия вопросов
            if (testTakingDTO.getQuestions() == null || testTakingDTO.getQuestions().isEmpty()) {
                model.addAttribute("errorMessage", "В тесте нет вопросов");
                return "error/general";
            }

            model.addAttribute("testTakingDTO", testTakingDTO);
            return "tester/test-taking";

        } catch (Exception e) {
            log.error("Ошибка при загрузке теста", e);
            return "redirect:/tester/tests?error=" + e.getMessage();
        }
    }

    @Operation(summary = "Обработка ответа")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/attempt/{attemptId}/answer")
    @ResponseBody
    public String saveAnswer(
            @PathVariable Long attemptId,
            @Valid @RequestBody TestPassingDTO testPassingDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (bindingResult.hasErrors()) {
            return "{\"status\": \"error\", \"message\": \"Некорректные данные\"}";
        }

        try {
            testPassingDTO.setAttemptId(attemptId);
            testPassingService.saveAnswer(testPassingDTO, userDetails.getUsername());

            // Проверяем, не завершен ли тест после сохранения
            TestAttempt attempt = testAttemptRepository.findById(attemptId).orElse(null);
            boolean isCompleted = attempt != null && attempt.getStatus() == TestAttempt.AttemptStatus.COMPLETED;

            return String.format("{\"status\": \"success\", \"message\": \"Ответ сохранен\", \"completed\": %b}", isCompleted);
        } catch (Exception e) {
            log.error("Ошибка при сохранении ответа", e);
            return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    @Operation(summary = "Завершение теста")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/attempt/{attemptId}/finish")
    public String finishTest(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            testPassingService.finishTestAndGetResults(attemptId, userDetails.getUsername());
            metricsService.incrementTestsCompleted();
            metricsService.decrementActiveUsers();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Тест успешно завершен!");
            return "redirect:/tester/attempt/" + attemptId + "/results";

        } catch (Exception e) {
            log.error("Ошибка при завершении теста", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при завершении теста: " + e.getMessage());
            return "redirect:/tester/attempt/" + attemptId;
        }
    }

    @Operation(summary = "Результаты теста")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/attempt/{attemptId}/results")
    public String testResults(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            TestResultsDTO results = testPassingService.getTestResults(attemptId, userDetails.getUsername());

            if (results == null) {
                model.addAttribute("errorMessage", "Результаты не найдены");
                return "error/general";
            }

            // Убедитесь, что все поля инициализированы
            if (results.getQuestionResults() == null) {
                results.setQuestionResults(new ArrayList<>());
            }

            model.addAttribute("results", results);
            return "tester/test-results";

        } catch (Exception e) {
            log.error("Ошибка при загрузке результатов", e);
            model.addAttribute("errorMessage", "Ошибка при загрузке результатов: " + e.getMessage());
            return "error/general";
        }
    }

    @Operation(summary = "История пройденых тестов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/history")
    public String testHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort,
            Model model) {

        try {
            // Получаем историю с пагинацией
            Page<TestHistoryDTO> historyPage = testPassingService.getUserTestHistory(
                    userDetails.getUsername(), page, size, status);

            // Получаем статистику пользователя
            UserStatisticsDTO statistics = testPassingService.getUserStatistics(
                    userDetails.getUsername());

            // Рассчитываем дополнительные метрики
            long completedCount = historyPage.getContent().stream()
                    .filter(attempt -> "COMPLETED".equals(attempt.getStatus()))
                    .count();

            long inProgressCount = historyPage.getContent().stream()
                    .filter(attempt -> "IN_PROGRESS".equals(attempt.getStatus()))
                    .count();

            double averageScore = historyPage.getContent().stream()
                    .filter(attempt -> "COMPLETED".equals(attempt.getStatus()))
                    .mapToDouble(TestHistoryDTO::getPercentage)
                    .average()
                    .orElse(0.0);

            // Настройки пагинации
            int currentPage = historyPage.getNumber();
            int totalPages = historyPage.getTotalPages();
            long totalItems = historyPage.getTotalElements();

            // Определяем диапазон страниц для отображения
            int startPage = Math.max(0, currentPage - 2);
            int endPage = Math.min(totalPages - 1, currentPage + 2);

            // Добавляем данные в модель
            model.addAttribute("history", historyPage.getContent());
            model.addAttribute("historyPage", historyPage);
            model.addAttribute("currentPage", currentPage);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalItems", totalItems);
            model.addAttribute("pageSize", size);
            model.addAttribute("startPage", startPage);
            model.addAttribute("endPage", endPage);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("sort", sort);

            model.addAttribute("completedCount", completedCount);
            model.addAttribute("inProgressCount", inProgressCount);
            model.addAttribute("averageScore", averageScore);
            model.addAttribute("statistics", statistics);

            return "tester/test-history";

        } catch (Exception e) {
            log.error("Ошибка при загрузке истории", e);
            model.addAttribute("errorMessage", "Ошибка при загрузке истории: " + e.getMessage());
            return "tester/test-history";
        }
    }

    @Operation(summary = "Переход к следующему вопросу")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/attempt/{attemptId}/question/{questionIndex}")
    public String nextQuestion(
            @PathVariable Long attemptId,
            @PathVariable Integer questionIndex,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            // Получаем текущее состояние теста
            TestTakingDTO testTakingDTO = testPassingService.getTestForTakingByAttemptId(attemptId, userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Тест не найден"));

            // Проверяем, что индекс корректен
            if (questionIndex >= 0 && questionIndex < testTakingDTO.getQuestions().size()) {
                testTakingDTO.setCurrentQuestionIndex(questionIndex);
            } else {
                // Если индекс некорректен, устанавливаем последний доступный
                testTakingDTO.setCurrentQuestionIndex(testTakingDTO.getQuestions().size() - 1);
            }

            model.addAttribute("testTakingDTO", testTakingDTO);
            return "tester/test-taking";

        } catch (Exception e) {
            log.error("Ошибка при переходе к вопросу", e);
            return "redirect:/tester/attempt/" + attemptId;
        }
    }

    @Operation(summary = "Дашборд тестера")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('TESTER')")
    public String testerDashboard(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String username = userDetails.getUsername();

            // Получаем статистику
            DashboardStatsDTO stats = dashboardService.getTesterStats(username);

            // Получаем последние попытки (без пагинации для дашборда)
            List<TestHistoryDTO> recentAttempts = testPassingService.getUserTestHistory(username);

            // Получаем доступные категории
            List<CategoryDTO> categories = testPassingService.getAvailableCategories();

            // Получаем несколько случайных тестов для рекомендаций
            List<TestInfoDTO> recommendedTests = testPassingService.getAllAvailableTestsDTO()
                    .stream()
                    .limit(3)
                    .collect(Collectors.toList());

            // Фильтруем попытки по статусу
            List<TestHistoryDTO> inProgressAttempts = recentAttempts.stream()
                    .filter(a -> a.getStatus() == TestAttempt.AttemptStatus.IN_PROGRESS)
                    .collect(Collectors.toList());

            List<TestHistoryDTO> recentCompleted = recentAttempts.stream()
                    .filter(a -> a.getStatus() == TestAttempt.AttemptStatus.COMPLETED)
                    .collect(Collectors.toList());

            // Добавляем в модель - ИСПОЛЬЗУЕМ ИМЕНА, КОТОРЫЕ ОЖИДАЕТ ШАБЛОН
            model.addAttribute("stats", stats);
            model.addAttribute("recentAttempts", recentAttempts);
            model.addAttribute("inProgressAttempts", inProgressAttempts);
            model.addAttribute("recentCompleted", recentCompleted); // ← ВАЖНО: именно recentCompleted
            model.addAttribute("categories", categories);
            model.addAttribute("recommendedTests", recommendedTests);
            model.addAttribute("username", username);

            log.debug("Загружен дашборд тестировщика: {}", username);
            return "tester/dashboard";

        } catch (Exception e) {
            log.error("Ошибка при загрузке панели тестировщика", e);
            model.addAttribute("errorMessage", "Ошибка при загрузке данных: " + e.getMessage());

            // Добавляем пустые объекты с правильными именами
            model.addAttribute("stats", new DashboardStatsDTO());
            model.addAttribute("recentAttempts", List.of());
            model.addAttribute("inProgressAttempts", List.of());
            model.addAttribute("recentCompleted", List.of()); // ← ВАЖНО
            model.addAttribute("categories", List.of());
            model.addAttribute("recommendedTests", List.of());

            return "tester/dashboard";
        }
    }

    @Operation(summary = "Последний пройденый тест")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/attempts/last-in-progress")
    @ResponseBody
    public TestHistoryDTO getLastInProgressAttempt(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<TestHistoryDTO> history = testPassingService.getUserTestHistory(userDetails.getUsername());
        return history.stream()
                .filter(attempt -> TestConstants.STATUS_IN_PROGRESS.equals(attempt.getStatus()))
                .findFirst()
                .orElse(null);
    }
}
