package com.frist.assesspro.controllers;

import com.frist.assesspro.dto.*;
import com.frist.assesspro.dto.test.*;
import com.frist.assesspro.service.TestPassingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/tester")
@RequiredArgsConstructor
@Slf4j
public class TesterController {

    private final TestPassingService testPassingService;

    /**
     * Каталог доступных тестов
     */
    @GetMapping("/tests")
    public String testCatalog(Model model) {
        List<TestInfoDTO> availableTests = testPassingService.getAvailableTestsDTO();
        model.addAttribute("tests", availableTests);
        return "tester/test-catalog";
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
            TestResultsDTO results = testPassingService.getTestResults(attemptId, userDetails.getUsername());

            if (results.getEndTime() != null) {
                return "redirect:/tester/attempt/" + attemptId + "/results";
            }

            TestTakingDTO testTakingDTO = testPassingService.getTestForTaking(results.getTestId(), userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Тест не найден"));

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
            model.addAttribute("results", results);
            return "tester/test-results";

        } catch (Exception e) {
            log.error("Ошибка при загрузке результатов", e);
            return "redirect:/tester/tests?error=" + e.getMessage();
        }
    }

    /**
     * История пройденных тестов
     */
    @GetMapping("/history")
    public String testHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            List<TestHistoryDTO> history = testPassingService.getUserTestHistory(userDetails.getUsername());
            model.addAttribute("history", history);
            return "tester/test-history";

        } catch (Exception e) {
            log.error("Ошибка при загрузке истории", e);
            return "redirect:/tester/tests?error=" + e.getMessage();
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


            TestTakingDTO testTakingDTO = testPassingService.getTestForTaking(results.getTestId, userDetails.getUsername())
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
}
