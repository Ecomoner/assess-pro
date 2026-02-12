package com.frist.assesspro.controllers.creator;


import com.frist.assesspro.dto.statistics.TesterAttemptDTO;
import com.frist.assesspro.dto.statistics.TesterDetailedAnswersDTO;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.CooldownService;
import com.frist.assesspro.service.TestService;
import com.frist.assesspro.service.TesterStatisticsService;
import com.frist.assesspro.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/creator/tests/{testId}/statistics")
@RequiredArgsConstructor
@Slf4j
public class TesterStatisticsController {

    private final TesterStatisticsService testerStatisticsService;
    private final TestService testService;
    private final CooldownService cooldownService;
    private final UserService userService;

    /**
     * Главная страница статистики теста - список тестировщиков
     */

    @GetMapping("/testers")
    public String getTestersList(
            @PathVariable Long testId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            Test test = testService.getTestById(testId, userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Тест не найден"));

            Page<TesterAttemptDTO> testersPage;

            if (search != null && !search.trim().isEmpty()) {
                List<TesterAttemptDTO> testers = testerStatisticsService
                        .searchTestersByTestAndName(testId, userDetails.getUsername(), search);
                testersPage = new PageImpl<>(
                        testers, PageRequest.of(page, size), testers.size());
            } else {
                Pageable pageable = PageRequest.of(page, size);
                testersPage = testerStatisticsService.getTestersByTest(
                        testId, userDetails.getUsername(), pageable);
            }

            // 🔥 ИСПРАВЛЕНО: Используем UserService для получения информации о тестировщиках
            Map<String, String> cooldownStatuses = new HashMap<>();
            Map<String, String> fullNames = new HashMap<>();
            Map<String, Boolean> profileCompletion = new HashMap<>();

            for (TesterAttemptDTO tester : testersPage.getContent()) {
                try {
                    User testerUser = userService.findByUsername(tester.getTesterUsername()).orElse(null);
                    if (testerUser != null) {
                        // Статус ограничений
                        String status = cooldownService.getCooldownStatus(test, testerUser);
                        cooldownStatuses.put(tester.getTesterUsername(), status);

                        // Полное имя
                        fullNames.put(tester.getTesterUsername(), testerUser.getFullName());

                        // Статус профиля
                        profileCompletion.put(tester.getTesterUsername(), testerUser.isProfileComplete());
                    }
                } catch (Exception e) {
                    log.warn("Не удалось получить данные для пользователя {}", tester.getTesterUsername(), e);
                }
            }

            model.addAttribute("test", test);
            model.addAttribute("testers", testersPage.getContent());
            model.addAttribute("currentPage", testersPage.getNumber());
            model.addAttribute("totalPages", testersPage.getTotalPages());
            model.addAttribute("totalItems", testersPage.getTotalElements());
            model.addAttribute("search", search);
            model.addAttribute("cooldownStatuses", cooldownStatuses);
            model.addAttribute("fullNames", fullNames);
            model.addAttribute("profileCompletion", profileCompletion);
            model.addAttribute("hasCooldown", test.hasRetryCooldown());

            return "creator/tester-statistics-main";

        } catch (Exception e) {
            log.error("Ошибка при загрузке списка тестировщиков", e);
            model.addAttribute("errorMessage", "Ошибка: " + e.getMessage());
            return "redirect:/creator/tests";
        }
    }

    /**
     * Детальная статистика конкретного тестировщика
     */
    @GetMapping("/tester/{attemptId}")
    public String getTesterDetailedAnswers(
            @PathVariable Long testId,
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            TesterDetailedAnswersDTO detailedAnswers = testerStatisticsService
                    .getTesterDetailedAnswers(attemptId, userDetails.getUsername());

            Test test = testService.getTestById(testId, userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Тест не найден"));

            User tester = userService.findByUsername(detailedAnswers.getTesterUsername()).orElse(null);
            if (tester != null) {
                model.addAttribute("testerFullName", tester.getFullName());
            }

            model.addAttribute("test", test);
            model.addAttribute("detailedAnswers", detailedAnswers);

            return "creator/tester-detailed-answers";

        } catch (Exception e) {
            log.error("Ошибка при загрузке детальных ответов", e);
            model.addAttribute("errorMessage", "Ошибка: " + e.getMessage());
            return "redirect:/creator/tests/" + testId + "/statistics/testers";
        }
    }

    /**
     * Экспорт результатов тестировщика в PDF
     */
    @GetMapping("/tester/{attemptId}/export")
    public ResponseEntity<byte[]> exportTesterResults(
            @PathVariable Long testId,
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            TesterDetailedAnswersDTO detailedAnswers = testerStatisticsService
                    .getTesterDetailedAnswers(attemptId, userDetails.getUsername());

            // Генерация простого PDF
            byte[] pdfContent = generateSimplePdfReport(detailedAnswers);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"results_" +
                                    detailedAnswers.getTesterUsername() + "_" +
                                    attemptId + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfContent.length)
                    .body(pdfContent);

        } catch (Exception e) {
            log.error("Ошибка при экспорте результатов", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Быстрый просмотр результатов (AJAX)
     */
    @GetMapping("/tester/{attemptId}/quick-view")
    @ResponseBody
    public TesterDetailedAnswersDTO getQuickView(
            @PathVariable Long testId,
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails) {

        return testerStatisticsService.getTesterDetailedAnswers(
                attemptId, userDetails.getUsername());
    }

    private byte[] generateSimplePdfReport(TesterDetailedAnswersDTO detailedAnswers) {
        // Заглушка для генерации PDF
        // В реальной реализации используйте iText, Apache PDFBox или другой PDF библиотеку
        String reportContent = buildReportContent(detailedAnswers);
        return reportContent.getBytes();
    }

    private String buildReportContent(TesterDetailedAnswersDTO detailedAnswers) {
        StringBuilder sb = new StringBuilder();
        sb.append("Отчет по результатам тестирования\n");
        sb.append("===================================\n\n");
        sb.append("Тестировщик: ").append(detailedAnswers.getTesterUsername()).append("\n");
        sb.append("Дата прохождения: ").append(detailedAnswers.getStartTime()).append("\n");
        sb.append("Результат: ").append(detailedAnswers.getSummary().getCorrectAnswers())
                .append("/").append(detailedAnswers.getSummary().getTotalQuestions())
                .append(" (").append(String.format("%.1f", detailedAnswers.getSummary().getPercentage()))
                .append("%)\n\n");

        sb.append("Детальные ответы:\n");
        sb.append("=================\n");

        for (int i = 0; i < detailedAnswers.getQuestionAnswers().size(); i++) {
            var answer = detailedAnswers.getQuestionAnswers().get(i);
            sb.append("\n").append(i + 1).append(". ").append(answer.getQuestionText()).append("\n");

            if (answer.getChosenAnswer() != null) {
                sb.append("   Выбранный ответ: ").append(answer.getChosenAnswer().getAnswerText());
                sb.append(" [").append(answer.getIsCorrect() ? "✓" : "✗").append("]\n");
            } else {
                sb.append("   Ответ не выбран\n");
            }

            if (!answer.getIsCorrect() && answer.getCorrectAnswer() != null) {
                sb.append("   Правильный ответ: ").append(answer.getCorrectAnswer().getAnswerText()).append("\n");
            }
        }

        return sb.toString();
    }
}




