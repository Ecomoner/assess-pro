package com.frist.assesspro.controllers.creator;


import com.frist.assesspro.dto.AnswerOptionDTO;
import com.frist.assesspro.dto.QuestionDTO;
import com.frist.assesspro.dto.QuestionWithStatsDTO;
import com.frist.assesspro.entity.Question;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.repository.UserRepository;
import com.frist.assesspro.service.QuestionService;
import com.frist.assesspro.service.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/creator/tests/{testId}/questions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Вопросы",description = "API для создателей")
public class QuestionController {

    private final QuestionService questionService;

    @ModelAttribute("currentUri")
    public String getCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @Operation(summary = "Страница управления вопросами теста")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping
    public String manageQuestions(
            @PathVariable Long testId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            List<Question> questions = questionService.getQuestionsByTestId(testId, userDetails.getUsername());
            Test test = questionService.getTestById(testId, userDetails.getUsername());

            // Конвертируем в DTO со статистикой - НО ФИЛЬТРУЕМ ПУСТЫЕ!
            List<QuestionWithStatsDTO> questionDTOs = questions.stream()
                    .map(question -> convertToQuestionWithStatsDTO(question))
                    .collect(Collectors.toList());

            model.addAttribute("test", test);
            model.addAttribute("questions", questions);
            model.addAttribute("questionDTOs", questionDTOs);
            return "creator/question-list";

        } catch (Exception e) {
            log.error("Ошибка при загрузке вопросов", e);
            return "redirect:/creator/tests?error=" + e.getMessage();
        }
    }

    private QuestionWithStatsDTO convertToQuestionWithStatsDTO(Question question) {
        // Фильтруем пустые ответы при конвертации
        List<AnswerOptionDTO> answerOptionDTOs = question.getAnswerOptions().stream()
                .filter(answer -> answer.getText() != null && !answer.getText().trim().isEmpty())
                .map(answer -> {
                    AnswerOptionDTO dto = new AnswerOptionDTO();
                    dto.setId(answer.getId());
                    dto.setText(answer.getText());
                    dto.setIsCorrect(answer.getIsCorrect());
                    return dto;
                })
                .collect(Collectors.toList());

        // Используем фабричный метод для создания DTO со статистикой
        return QuestionWithStatsDTO.createWithStats(
                question.getId(),
                question.getText(),
                question.getOrderIndex(),
                answerOptionDTOs
        );
    }

    @Operation(summary = "Форма создания нового вопроса")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/new")
    public String showCreateQuestionForm(
            @PathVariable Long testId,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("=== ОТКРЫТИЕ ФОРМЫ СОЗДАНИЯ ВОПРОСА ДЛЯ ТЕСТА ID: {} ===", testId);

        try {
            Test test = questionService.getTestById(testId, userDetails.getUsername());

            QuestionDTO questionDTO = new QuestionDTO();
            questionDTO.setText("");
            questionDTO.setOrderIndex(0);


            questionDTO.getAnswerOptions().clear();


            for (int i = 0; i < 2; i++) {
                AnswerOptionDTO emptyAnswer = new AnswerOptionDTO();
                emptyAnswer.setText("");
                emptyAnswer.setIsCorrect(false);
                questionDTO.getAnswerOptions().add(emptyAnswer);
            }

            model.addAttribute("test", test);
            model.addAttribute("questionDTO", questionDTO);
            model.addAttribute("action", "create");
            model.addAttribute("formAction", "/creator/tests/" + testId + "/questions/new");

            log.info("Создана НОВАЯ форма. Вариантов ответов: {}",
                    questionDTO.getAnswerOptions().size());

            return "creator/question-form";

        } catch (Exception e) {
            log.error("Ошибка при открытии формы вопроса", e);
            return "redirect:/creator/tests/" + testId + "/questions?error=" + e.getMessage();
        }
    }


    @Operation(summary = "Создание нового вопроса")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/new")
    public String createQuestion(
            @PathVariable Long testId,
            @Valid @ModelAttribute QuestionDTO questionDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        log.info("=== НАЧАЛО СОЗДАНИЯ ВОПРОСА ДЛЯ ТЕСТА {} ===", testId);

        if (questionDTO.getText() == null || questionDTO.getText().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Текст вопроса обязателен");
            redirectAttributes.addFlashAttribute("questionDTO", questionDTO);
            return "redirect:/creator/tests/" + testId + "/questions/new";
        }

        // Оригинальная валидация
        if (bindingResult.hasErrors()) {
            log.warn("Ошибки валидации при создании вопроса: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.questionDTO",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute("questionDTO", questionDTO);
            return "redirect:/creator/tests/" + testId + "/questions/new";
        }

        try {
            // Фильтруем пустые варианты ответов
            List<AnswerOptionDTO> filteredAnswers = questionDTO.getAnswerOptions().stream()
                    .filter(answer -> answer.getText() != null && !answer.getText().trim().isEmpty())
                    .collect(Collectors.toList());

            if (filteredAnswers.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Добавьте хотя бы один вариант ответа");
                redirectAttributes.addFlashAttribute("questionDTO", questionDTO);
                return "redirect:/creator/tests/" + testId + "/questions/new";
            }

            questionDTO.setAnswerOptions(filteredAnswers);

            // ВАЖНО: Логируем перед созданием
            log.info("Создание вопроса с текстом: '{}'", questionDTO.getText());
            log.info("Количество вариантов ответов: {}", filteredAnswers.size());

            Question savedQuestion = questionService.createQuestion(testId, questionDTO, userDetails.getUsername());

            log.info("=== ВОПРОС УСПЕШНО СОЗДАН. ID: {} ===", savedQuestion.getId());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Вопрос успешно добавлен!");
            return "redirect:/creator/tests/" + testId + "/questions";

        } catch (Exception e) {
            log.error("Ошибка при создании вопроса", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка: " + e.getMessage());
            redirectAttributes.addFlashAttribute("questionDTO", questionDTO);
            return "redirect:/creator/tests/" + testId + "/questions/new";
        }
    }

    @Operation(summary = "Форма редактирования вопроса")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/{questionId}/edit")
    public String showEditQuestionForm(
            @PathVariable Long testId,
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            Test test = questionService.getTestById(testId, userDetails.getUsername());
            QuestionDTO questionDTO = questionService.getQuestionDTO(questionId, userDetails.getUsername());

            while (questionDTO.getAnswerOptions().size() < 4) {
                questionDTO.getAnswerOptions().add(new AnswerOptionDTO());
            }

            model.addAttribute("test", test);
            model.addAttribute("questionDTO", questionDTO);
            model.addAttribute("action", "edit");
            model.addAttribute("formAction", "/creator/tests/" + testId + "/questions/" + questionId + "/update");
            return "creator/question-form";

        } catch (Exception e) {
            log.error("Ошибка при открытии формы редактирования", e);
            return "redirect:/creator/tests/" + testId + "/questions?error=" + e.getMessage();
        }
    }

    @Operation(summary = "Обновление вопроса")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/{questionId}/update")
    public String updateQuestion(
            @PathVariable Long testId,
            @PathVariable Long questionId,
            @Valid @ModelAttribute QuestionDTO questionDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            log.warn("Ошибки валидации при обновлении вопроса: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.questionDTO",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute("questionDTO", questionDTO);
            return "redirect:/creator/tests/" + testId + "/questions/" + questionId + "/edit";
        }

        try {
            // Фильтруем пустые варианты ответов
            List<AnswerOptionDTO> filteredAnswers = questionDTO.getAnswerOptions().stream()
                    .filter(answer -> answer.getText() != null && !answer.getText().trim().isEmpty())
                    .collect(Collectors.toList());

            if (filteredAnswers.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Добавьте хотя бы один вариант ответа");
                redirectAttributes.addFlashAttribute("questionDTO", questionDTO);
                return "redirect:/creator/tests/" + testId + "/questions/" + questionId + "/edit";
            }

            questionDTO.setAnswerOptions(filteredAnswers);

            questionService.updateQuestion(questionId, questionDTO, userDetails.getUsername());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Вопрос успешно обновлен!");
            return "redirect:/creator/tests/" + testId + "/questions";

        } catch (Exception e) {
            log.error("Ошибка при обновлении вопроса", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка: " + e.getMessage());
            redirectAttributes.addFlashAttribute("questionDTO", questionDTO);
            return "redirect:/creator/tests/" + testId + "/questions/" + questionId + "/edit";
        }
    }

    @Operation(summary = "Удаление вопроса")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/{questionId}/delete")
    public String deleteQuestion(
            @PathVariable Long testId,
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            questionService.deleteQuestion(questionId, userDetails.getUsername());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Вопрос успешно удален!");
            return "redirect:/creator/tests/" + testId + "/questions";

        } catch (Exception e) {
            log.error("Ошибка при удалении вопроса", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка: " + e.getMessage());
            return "redirect:/creator/tests/" + testId + "/questions";
        }
    }
}
