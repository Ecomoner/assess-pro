package com.frist.assesspro.service;


import com.frist.assesspro.dto.AnswerOptionDTO;
import com.frist.assesspro.dto.QuestionDTO;
import com.frist.assesspro.entity.AnswerOption;
import com.frist.assesspro.entity.Question;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.repository.AnswerOptionRepository;
import com.frist.assesspro.repository.QuestionRepository;
import com.frist.assesspro.repository.TestRepository;
import com.frist.assesspro.repository.UserRepository;
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
    private final AnswerOptionRepository answerOptionRepository;
    private final TestRepository testRepository;
    private final UserRepository userRepository;

    /**
     * Создание вопроса из DTO
     */
    @Transactional
    public Question createQuestion(Long testId, QuestionDTO questionDTO,String username){
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест не найден"));

        if (!test.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Нет прав для редактирования этого теста");
        }

        Question question = new Question();
        question.setText(questionDTO.getText());
        question.setOrderIndex(questionDTO.getOrderIndex());
        question.setTest(test);

        Question savedQuestion = questionRepository.save(question);

        if (questionDTO.getAnswerOptions() != null && questionDTO.getAnswerOptions().isEmpty()){
            for (AnswerOptionDTO answer:questionDTO.getAnswerOptions()){
                AnswerOption answerOption = new AnswerOption();
                answerOption.setText(answer.getText());
                answerOption.setIsCorrect(answer.getIsCorrect());
                answerOption.setQuestion(savedQuestion);

                answerOptionRepository.save(answerOption);
            }
        }

        log.info("Создан вопрос ID: {} для теста ID: {}", savedQuestion.getId(), testId);
        return savedQuestion;
    }

    /**
     * Обновление вопроса из DTO
     */
    @Transactional
    public Question updateQuestion(Long questionId, QuestionDTO questionDTO, String username) {
        Question existingQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Вопрос не найден"));

        // Проверяем права
        Test test = existingQuestion.getTest();
        if (!test.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Нет прав для редактирования этого вопроса");
        }

        // Обновляем поля вопроса
        existingQuestion.setText(questionDTO.getText());
        existingQuestion.setOrderIndex(questionDTO.getOrderIndex());

        // Удаляем старые варианты ответов
        answerOptionRepository.deleteByQuestionId(questionId);

        // Добавляем новые варианты ответов
        if (questionDTO.getAnswerOptions() != null && !questionDTO.getAnswerOptions().isEmpty()) {
            for (AnswerOptionDTO answerDTO : questionDTO.getAnswerOptions()) {
                AnswerOption answerOption = new AnswerOption();
                answerOption.setText(answerDTO.getText());
                answerOption.setIsCorrect(answerDTO.getIsCorrect() != null && answerDTO.getIsCorrect());
                answerOption.setQuestion(existingQuestion);

                answerOptionRepository.save(answerOption);
            }
        }

        Question updatedQuestion = questionRepository.save(existingQuestion);
        log.info("Обновлен вопрос ID: {}", questionId);
        return updatedQuestion;
    }

    /**
     * Получение вопроса как DTO
     */
    @Transactional(readOnly = true)
    public QuestionDTO getQuestionDTO(Long questionId, String username) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Вопрос не найден"));

        Test test = question.getTest();
        if (!test.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Нет прав для просмотра этого вопроса");
        }

        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setText(question.getText());
        dto.setOrderIndex(question.getOrderIndex());

        List<AnswerOptionDTO> answerDTOs = question.getAnswerOptions().stream()
                .map(answer -> {
                    AnswerOptionDTO answerDTO = new AnswerOptionDTO();
                    answerDTO.setId(answer.getId());
                    answerDTO.setText(answer.getText());
                    answerDTO.setIsCorrect(answer.getIsCorrect());
                    return answerDTO;
                })
                .collect(Collectors.toList());

        dto.setAnswerOptions(answerDTOs);
        return dto;
    }

    /**
     * Получение всех вопросов теста
     */
    @Transactional(readOnly = true)
    public List<Question> getQuestionsByTestId(Long testId, String username) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест не найден"));

        if (!test.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Нет прав для просмотра вопросов этого теста");
        }

        List<Question> questions = questionRepository.findByTestIdOrderByOrderIndex(testId);

        for (Question question : questions) {
            question.getAnswerOptions().size();
        }

        return questions;
    }

    /**
     * Получение всех вопросов теста
     */

    @Transactional(readOnly = true)
    public List<Question> getQuestionByTestId(Long testId,String username){
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест не найден"));

        if (!test.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Нет прав для просмотра вопросов этого теста");
        }

        List<Question> questions = questionRepository.findByTestIdOrderByOrderIndex(testId);

        for (Question question:questions){
            question.getAnswerOptions().size();
        }
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

        Test test = question.getTest();
        if (!test.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Нет прав для удаления этого вопроса");
        }

        if (test.getQuestions().size() <= 1) {
            throw new RuntimeException("Нельзя удалить последний вопрос в тесте");
        }

        questionRepository.delete(question);
        log.info("Удален вопрос ID: {} из теста ID: {}", questionId, test.getId());
    }
}
