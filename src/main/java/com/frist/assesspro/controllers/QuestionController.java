package com.frist.assesspro.controllers;


import com.frist.assesspro.dto.AnswerOptionDTO;
import com.frist.assesspro.dto.QuestionDTO;
import com.frist.assesspro.entity.Question;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.service.QuestionService;
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
public class QuestionController {

    private final QuestionService questionService;

    /**
     * Страница управления вопросами теста
     */

    @GetMapping
    public String manageQuestions(
            @PathVariable Long testId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            List<Question> questions = questionService.getQuestionsByTestId(testId, userDetails.getUsername());
            Test test = questionService.getTestById(testId, userDetails.getUsername());

            model.addAttribute("test", test);
            model.addAttribute("questions", questions);
            return "creator/question-list";

        } catch (Exception e) {
            log.error("Ошибка при загрузке вопросов", e);
            return "redirect:/creator/tests?error=" + e.getMessage();
        }
    }

    /**
     * Форма создания нового вопроса
     */

    @GetMapping("/new")
    public String showCreateQuestionForm(
            @PathVariable Long testId,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Test test = questionService.getTestById(testId, userDetails.getUsername());

            QuestionDTO questionDTO = new QuestionDTO();
            questionDTO.setOrderIndex(0);

            model.addAttribute("test", test);
            model.addAttribute("questionDTO", questionDTO);
            model.addAttribute("action", "create");
            return "creator/question-form";

        } catch (Exception e) {
            log.error("Ошибка при открытии формы вопроса", e);
            return "redirect:/creator/tests/" + testId + "/questions?error=" + e.getMessage();
        }
    }


    /**
     * Создание нового вопроса
     */
    @PostMapping("/new")
    public String createQuestion(
            @PathVariable Long testId,
            @Valid @ModelAttribute QuestionDTO questionDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        log.info("Создание вопроса для теста ID: {}", testId);

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

            Question savedQuestion = questionService.createQuestion(testId, questionDTO, userDetails.getUsername());

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

    /**
     * Форма редактирования вопроса
     */

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
            return "creator/question-form";

        } catch (Exception e) {
            log.error("Ошибка при открытии формы редактирования", e);
            return "redirect:/creator/tests/" + testId + "/questions?error=" + e.getMessage();
        }
    }

    /**
     * Обновление вопроса
     */

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

    /**
     * Удаление вопроса
     */
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
