package com.frist.assesspro.controllers.tester;

import com.frist.assesspro.dto.*;
import com.frist.assesspro.dto.test.*;
import com.frist.assesspro.entity.TestAttempt;
import com.frist.assesspro.repository.TestAttemptRepository;
import com.frist.assesspro.service.DashboardService;
import com.frist.assesspro.service.TestPassingService;
import com.frist.assesspro.util.TestConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
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
public class TesterController {

    private final TestPassingService testPassingService;
    private final DashboardService dashboardService;
    private final TestAttemptRepository testAttemptRepository;

    /**
     * Каталог доступных тестов
     */
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

    /**
     * 🔥 НОВОЕ: Быстрый поиск для AJAX
     */
    @GetMapping("/tests/search/quick")
    @ResponseBody
    public List<TestInfoDTO> quickSearch(
            @RequestParam String term,
            @RequestParam(defaultValue = "5") int limit) {

        return testPassingService.quickSearchTests(term, limit);
    }

    /**
     * Начало прохождения теста
     */
    @GetMapping("/test/{testId}/start")
    public String startTest(
            @PathVariable Long testId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
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

    /**
     * Страница прохождения теста
     */
    @GetMapping("/attempt/{attemptId}")
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
                return "error/test-no-questions";
            }

            model.addAttribute("testTakingDTO", testTakingDTO);
            return "tester/test-taking";

        } catch (Exception e) {
            log.error("Ошибка при загрузке теста", e);
            return "redirect:/tester/tests?error=" + e.getMessage();
        }
    }

    /**
     * Обработка ответа на вопрос (AJAX)
     */
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
            return "{\"status\": \"success\", \"message\": \"Ответ сохранен\"}";
        } catch (Exception e) {
            log.error("Ошибка при сохранении ответа", e);
            return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Завершение теста
     */
    @PostMapping("/attempt/{attemptId}/finish")
    public String finishTest(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            testPassingService.finishTestAndGetResults(attemptId, userDetails.getUsername());

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

    /**
     * Результаты теста
     */
    @GetMapping("/attempt/{attemptId}/results")
    public String testResults(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            TestResultsDTO results = testPassingService.getTestResults(attemptId, userDetails.getUsername());

            if (results == null) {
                model.addAttribute("errorMessage", "Результаты не найдены");
                return "error/test-results-not-found";
            }

            // Убедитесь, что все поля инициализированы
            if (results.getQuestionResults() == null) {
                results.setQuestionResults(new ArrayList<>());
            }

            model.addAttribute("results", results);
            return "tester/test-results-simple";

        } catch (Exception e) {
            log.error("Ошибка при загрузке результатов", e);
            model.addAttribute("errorMessage", "Ошибка при загрузке результатов: " + e.getMessage());
            return "error/general";
        }
    }

    /**
     * История пройденных тестов
     */
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

    /**
     * Переход к следующему вопросу
     */
    @GetMapping("/attempt/{attemptId}/question/{questionIndex}")
    public String nextQuestion(
            @PathVariable Long attemptId,
            @PathVariable Integer questionIndex,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            TestResultsDTO results = testPassingService.getTestResults(attemptId, userDetails.getUsername());

            TestTakingDTO testTakingDTO = testPassingService.getTestForTaking(results.getTestId(), userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Тест не найден"));

            if (questionIndex >= 0 && questionIndex < testTakingDTO.getTotalQuestions()) {
                testTakingDTO.setCurrentQuestionIndex(questionIndex);
            }
            model.addAttribute("testTakingDTO", testTakingDTO);
            return "tester/test-taking";

        } catch (Exception e) {
            log.error("Ошибка при переходе к вопросу", e);
            return "redirect:/tester/attempt/" + attemptId;
        }
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('TESTER')")
    public String testerDashboard(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            DashboardStatsDTO stats = dashboardService.getTesterStats(userDetails.getUsername());

            // ✅ ИСПОЛЬЗУЕМ НОВЫЙ МЕТОД
            List<TestInfoDTO> availableTests = testPassingService.getAllAvailableTestsDTO();

            // Для дашборда берем только первые 3 теста
            List<TestInfoDTO> recommendedTests = availableTests.stream()
                    .limit(3)
                    .collect(Collectors.toList());

            List<TestHistoryDTO> history = testPassingService.getUserTestHistory(userDetails.getUsername());

            List<TestHistoryDTO> inProgressAttempts = history.stream()
                    .filter(attempt -> TestConstants.STATUS_IN_PROGRESS.equals(attempt.getStatus()))
                    .collect(Collectors.toList());

            List<TestHistoryDTO> recentCompleted = history.stream()
                    .filter(attempt -> TestConstants.STATUS_COMPLETED.equals(attempt.getStatus()))
                    .limit(5)
                    .collect(Collectors.toList());

            model.addAttribute("inProgressAttempts", inProgressAttempts);
            model.addAttribute("recentCompleted", recentCompleted);
            model.addAttribute("stats", stats);
            model.addAttribute("recommendedTests", recommendedTests);
            model.addAttribute("username", userDetails.getUsername());
            model.addAttribute("totalAvailableTests", availableTests.size());
            model.addAttribute("message", "Добро пожаловать в панель тестировщика!");

            return "tester/dashboard";

        } catch (Exception e) {
            log.error("Ошибка при загрузке панели тестировщика", e);
            model.addAttribute("errorMessage", "Ошибка при загрузке данных: " + e.getMessage());
            return "tester/dashboard";
        }
    }


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
