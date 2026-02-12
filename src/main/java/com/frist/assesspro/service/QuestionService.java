package com.frist.assesspro.service;

import com.frist.assesspro.dto.AnswerOptionDTO;
import com.frist.assesspro.dto.QuestionDTO;
import com.frist.assesspro.entity.AnswerOption;
import com.frist.assesspro.entity.Question;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TestRepository testRepository;
    private final UserRepository userRepository;

    /**
     * Создание вопроса из DTO
     */
    @Transactional
    public Question createQuestion(Long testId, QuestionDTO questionDTO, String username) {
        log.info("Создание вопроса для теста {}", testId);

        validateQuestionDTO(questionDTO);
        Test test = getTestWithAuthCheck(testId, username);

        // Проверка дубликатов
        checkDuplicateQuestion(testId, questionDTO.getText().trim());

        // Создаем вопрос
        Question question = new Question();
        question.setText(questionDTO.getText().trim());
        question.setOrderIndex(questionDTO.getOrderIndex());
        question.setTest(test);

        // 🔥 ПРАВИЛЬНО: Добавляем ответы через helper метод
        List<AnswerOptionDTO> validAnswers = filterAndValidateAnswers(questionDTO.getAnswerOptions());

        for (AnswerOptionDTO answerDTO : validAnswers) {
            AnswerOption answerOption = new AnswerOption();
            answerOption.setText(answerDTO.getText().trim());
            answerOption.setIsCorrect(answerDTO.getIsCorrect() != null && answerDTO.getIsCorrect());

            question.addAnswerOption(answerOption);
        }

        // Сохраняем - каскадно сохранятся все ответы
        Question savedQuestion = questionRepository.save(question);

        log.info("Создан вопрос ID: {} с {} вариантами ответов",
                savedQuestion.getId(), savedQuestion.getAnswerOptions().size());

        return savedQuestion;
    }

    /**
     * Обновление вопроса из DTO
     */
    @Transactional
    public Question updateQuestion(Long questionId, QuestionDTO questionDTO, String username) {
        Question existingQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Вопрос не найден"));

        validateQuestionOwnership(existingQuestion, username);

        // Обновляем поля вопроса
        existingQuestion.setText(questionDTO.getText().trim());
        existingQuestion.setOrderIndex(questionDTO.getOrderIndex());

        existingQuestion.clearAnswerOptions();

        if (questionDTO.getAnswerOptions() != null && !questionDTO.getAnswerOptions().isEmpty()) {
            List<AnswerOptionDTO> validAnswers = filterAndValidateAnswers(questionDTO.getAnswerOptions());

            for (AnswerOptionDTO answerDTO : validAnswers) {
                AnswerOption answerOption = new AnswerOption();
                answerOption.setText(answerDTO.getText().trim());
                answerOption.setIsCorrect(answerDTO.getIsCorrect() != null && answerDTO.getIsCorrect());


                existingQuestion.addAnswerOption(answerOption);
            }
        }

        Question updatedQuestion = questionRepository.save(existingQuestion);

        log.info("Обновлен вопрос ID: {} с {} вариантами ответов",
                questionId, updatedQuestion.getAnswerOptions().size());

        return updatedQuestion;
    }

    /**
     * Получение вопроса как DTO
     */
    @Transactional(readOnly = true)
    public QuestionDTO getQuestionDTO(Long questionId, String username) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Вопрос не найден"));

        validateQuestionOwnership(question, username);

        return convertToDTO(question);
    }

    /**
     * Получение всех вопросов теста - ЕДИНСТВЕННЫЙ метод для этого
     */
    @Transactional(readOnly = true)
    public List<Question> getQuestionsByTestId(Long testId, String username) {
        Test test = getTestWithAuthCheck(testId, username);

        // Загружаем вопросы с вариантами ответов (один запрос)
        List<Question> questions = questionRepository.findByTestIdOrderByOrderIndex(testId);

        // Hibernate автоматически использует batch fetching для загрузки answerOptions
        // благодаря настройке default_batch_fetch_size
        return questions;
    }

    /**
     * Получение теста с проверкой прав
     */
    @Transactional(readOnly = true)
    public Test getTestById(Long testId, String username) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест не найден"));

        if (!test.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Нет прав для доступа к этому тесту");
        }

        return test;
    }

    /**
     * Удаление вопроса
     */
    @Transactional
    public void deleteQuestion(Long questionId, String username) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Вопрос не найден"));

        validateQuestionOwnership(question, username);

        Test test = question.getTest();
        if (test.getQuestions().size() <= 1) {
            throw new RuntimeException("Нельзя удалить последний вопрос в тесте");
        }

        questionRepository.delete(question);
        log.info("Удален вопрос ID: {} из теста ID: {}", questionId, test.getId());
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private void validateQuestionDTO(QuestionDTO questionDTO) {
        if (questionDTO.getText() == null || questionDTO.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Текст вопроса обязателен");
        }

        if (questionDTO.getText().trim().length() < 5) {
            throw new IllegalArgumentException("Текст вопроса слишком короткий");
        }

        if (questionDTO.getOrderIndex() == null || questionDTO.getOrderIndex() < 0) {
            throw new IllegalArgumentException("Некорректный порядковый номер");
        }
    }

    private Test getTestWithAuthCheck(Long testId, String username) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест не найден"));

        if (!test.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Нет прав для редактирования этого теста");
        }

        return test;
    }

    private void validateQuestionOwnership(Question question, String username) {
        if (!question.getTest().getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Нет прав для доступа к этому вопросу");
        }
    }

    private List<AnswerOptionDTO> filterAndValidateAnswers(List<AnswerOptionDTO> answerOptions) {
        if (answerOptions == null || answerOptions.isEmpty()) {
            throw new IllegalArgumentException("Добавьте хотя бы один вариант ответа");
        }

        // Фильтруем пустые варианты
        List<AnswerOptionDTO> validAnswers = answerOptions.stream()
                .filter(answer -> answer.getText() != null && !answer.getText().trim().isEmpty())
                .collect(Collectors.toList());

        if (validAnswers.size() < 2) {
            throw new IllegalArgumentException("Добавьте как минимум 2 варианта ответа");
        }

        // Проверяем, что есть хотя бы один правильный ответ
        boolean hasCorrectAnswer = validAnswers.stream()
                .anyMatch(AnswerOptionDTO::getIsCorrect);

        if (!hasCorrectAnswer) {
            throw new IllegalArgumentException("Отметьте хотя бы один правильный вариант ответа");
        }

        return validAnswers;
    }

    private void checkDuplicateQuestion(Long testId, String questionText) {
        List<Question> existingQuestions = questionRepository.findByTestIdOrderByOrderIndex(testId);

        boolean duplicateExists = existingQuestions.stream()
                .anyMatch(q -> q.getText() != null &&
                        q.getText().trim().equalsIgnoreCase(questionText));

        if (duplicateExists) {
            throw new RuntimeException("Такой вопрос уже существует в этом тесте!");
        }
    }

    private QuestionDTO convertToDTO(Question question) {
        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setText(question.getText());
        dto.setOrderIndex(question.getOrderIndex());

        List<AnswerOptionDTO> answerDTOs = question.getAnswerOptions().stream()
                .map(this::convertToAnswerOptionDTO)
                .collect(Collectors.toList());

        dto.setAnswerOptions(answerDTOs);
        return dto;
    }

    private AnswerOptionDTO convertToAnswerOptionDTO(AnswerOption answer) {
        AnswerOptionDTO dto = new AnswerOptionDTO();
        dto.setId(answer.getId());
        dto.setText(answer.getText());
        dto.setIsCorrect(answer.getIsCorrect());
        return dto;
    }

}
