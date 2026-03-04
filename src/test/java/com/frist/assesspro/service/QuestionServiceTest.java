package com.frist.assesspro.service;

import com.frist.assesspro.dto.AnswerOptionDTO;
import com.frist.assesspro.dto.QuestionDTO;
import com.frist.assesspro.entity.AnswerOption;
import com.frist.assesspro.entity.Question;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.QuestionRepository;
import com.frist.assesspro.repository.TestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private TestRepository testRepository;

    @InjectMocks
    private QuestionService questionService;

    private User creator;
    private Test test;
    private Question question;
    private QuestionDTO questionDTO;
    private AnswerOptionDTO answer1DTO;
    private AnswerOptionDTO answer2DTO;
    private AnswerOption answer1;
    private AnswerOption answer2;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setUsername("creator");
        creator.setRole(User.Roles.CREATOR);

        test = new Test();
        test.setId(1L);
        test.setTitle("Тест 1");
        test.setCreatedBy(creator);

        question = new Question();
        question.setId(1L);
        question.setText("Вопрос 1?");
        question.setOrderIndex(1);
        question.setTest(test);

        answer1 = new AnswerOption();
        answer1.setId(1L);
        answer1.setText("Ответ 1");
        answer1.setIsCorrect(true);

        answer2 = new AnswerOption();
        answer2.setId(2L);
        answer2.setText("Ответ 2");
        answer2.setIsCorrect(false);

        question.setAnswerOptions(Arrays.asList(answer1, answer2));

        // Создаем DTO для создания вопроса
        questionDTO = new QuestionDTO();
        questionDTO.setText("Новый вопрос?");
        questionDTO.setOrderIndex(1);

        answer1DTO = new AnswerOptionDTO();
        answer1DTO.setText("Вариант 1");
        answer1DTO.setIsCorrect(true);

        answer2DTO = new AnswerOptionDTO();
        answer2DTO.setText("Вариант 2");
        answer2DTO.setIsCorrect(false);

        AnswerOptionDTO answer3DTO = new AnswerOptionDTO();
        answer3DTO.setText("Вариант 3");
        answer3DTO.setIsCorrect(false);

        questionDTO.setAnswerOptions(Arrays.asList(answer1DTO, answer2DTO, answer3DTO));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Успешное создание вопроса")
    void createQuestion_Success() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        when(questionRepository.findByTestIdOrderByOrderIndex(1L)).thenReturn(new ArrayList<>());
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question savedQuestion = invocation.getArgument(0);
            savedQuestion.setId(1L);
            return savedQuestion;
        });

        Question createdQuestion = questionService.createQuestion(1L, questionDTO, "creator");

        assertThat(createdQuestion).isNotNull();
        assertThat(createdQuestion.getId()).isEqualTo(1L);
        assertThat(createdQuestion.getText()).isEqualTo("Новый вопрос?");
        assertThat(createdQuestion.getAnswerOptions()).hasSize(3);

        verify(questionRepository).save(any(Question.class));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Создание вопроса без текста - ошибка")
    void createQuestion_EmptyText_ThrowsException() {
        questionDTO.setText("   ");

        assertThatThrownBy(() -> questionService.createQuestion(1L, questionDTO, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Текст вопроса обязателен");

        verify(questionRepository, never()).save(any(Question.class));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Создание вопроса без вариантов ответа - ошибка")
    void createQuestion_NoAnswers_ThrowsException() {
        questionDTO.setAnswerOptions(new ArrayList<>());
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        when(questionRepository.findByTestIdOrderByOrderIndex(1L)).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> questionService.createQuestion(1L, questionDTO, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Добавьте хотя бы один вариант ответа");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Создание вопроса с одним вариантом ответа - ошибка")
    void createQuestion_OneAnswer_ThrowsException() {
        // given
        questionDTO.setAnswerOptions(Arrays.asList(answer1DTO));
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        when(questionRepository.findByTestIdOrderByOrderIndex(1L)).thenReturn(new ArrayList<>());

        // when/then
        assertThatThrownBy(() -> questionService.createQuestion(1L, questionDTO, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Добавьте как минимум 2 варианта ответа");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Создание вопроса без правильного ответа - ошибка")
    void createQuestion_NoCorrectAnswer_ThrowsException() {
        answer1DTO.setIsCorrect(false);
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));// Оба ответа неправильные

        assertThatThrownBy(() -> questionService.createQuestion(1L, questionDTO, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Отметьте хотя бы один правильный вариант ответа");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Создание вопроса с несуществующим тестом - ошибка")
    void createQuestion_TestNotFound_ThrowsException() {
        when(testRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.createQuestion(99L, questionDTO, "creator"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Тест не найден");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Создание вопроса с дубликатом текста - ошибка")
    void createQuestion_DuplicateText_ThrowsException() {
        Question existingQuestion = new Question();
        existingQuestion.setText("Новый вопрос?");

        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        when(questionRepository.findByTestIdOrderByOrderIndex(1L)).thenReturn(List.of(existingQuestion));

        assertThatThrownBy(() -> questionService.createQuestion(1L, questionDTO, "creator"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Такой вопрос уже существует");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Успешное обновление вопроса")
    void updateQuestion_Success() {
        QuestionDTO updateDTO = new QuestionDTO();
        updateDTO.setText("Обновленный вопрос?");
        updateDTO.setOrderIndex(2);

        AnswerOptionDTO updatedAnswer1 = new AnswerOptionDTO();
        updatedAnswer1.setId(1L);
        updatedAnswer1.setText("Обновленный ответ 1");
        updatedAnswer1.setIsCorrect(true);

        AnswerOptionDTO updatedAnswer2 = new AnswerOptionDTO();
        updatedAnswer2.setId(2L);
        updatedAnswer2.setText("Обновленный ответ 2");
        updatedAnswer2.setIsCorrect(false);

        updateDTO.setAnswerOptions(Arrays.asList(updatedAnswer1, updatedAnswer2));

        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenReturn(question);

        Question updatedQuestion = questionService.updateQuestion(1L, updateDTO, "creator");

        assertThat(updatedQuestion).isNotNull();
        assertThat(updatedQuestion.getText()).isEqualTo("Обновленный вопрос?");
        assertThat(updatedQuestion.getOrderIndex()).isEqualTo(2);

        verify(questionRepository).save(question);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Обновление несуществующего вопроса - ошибка")
    void updateQuestion_NotFound_ThrowsException() {
        when(questionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.updateQuestion(99L, questionDTO, "creator"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Вопрос не найден");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Обновление чужого вопроса - ошибка")
    void updateQuestion_NotOwner_ThrowsException() {
        User anotherUser = new User();
        anotherUser.setUsername("another");

        Test anotherTest = new Test();
        anotherTest.setCreatedBy(anotherUser);
        question.setTest(anotherTest);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> questionService.updateQuestion(1L, questionDTO, "creator"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Нет прав");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Получение вопроса по ID - успешно")
    void getQuestionDTO_Success() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));

        QuestionDTO foundDTO = questionService.getQuestionDTO(1L, "creator");

        assertThat(foundDTO).isNotNull();
        assertThat(foundDTO.getId()).isEqualTo(1L);
        assertThat(foundDTO.getText()).isEqualTo("Вопрос 1?");
        assertThat(foundDTO.getAnswerOptions()).hasSize(2);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Получение вопросов по ID теста - успешно")
    void getQuestionsByTestId_Success() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        when(questionRepository.findByTestIdOrderByOrderIndex(1L)).thenReturn(List.of(question));

        List<Question> questions = questionService.getQuestionsByTestId(1L, "creator");

        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).getText()).isEqualTo("Вопрос 1?");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Удаление вопроса - успешно")
    void deleteQuestion_Success() {
        test.setQuestions(new ArrayList<>(List.of(question, new Question()))); // Минимум 2 вопроса

        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));

        questionService.deleteQuestion(1L, "creator");

        verify(questionRepository).delete(question);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Удаление последнего вопроса в тесте - ошибка")
    void deleteQuestion_LastQuestion_ThrowsException() {
        test.setQuestions(List.of(question)); // Только один вопрос

        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> questionService.deleteQuestion(1L, "creator"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Нельзя удалить последний вопрос");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Конвертация AnswerOption в AnswerOptionDTO")
    void convertToAnswerOptionDTO_ShouldMapCorrectly() {
        // 1. Сначала настраиваем мок
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));

        // 2. Потом вызываем метод
        QuestionDTO result = questionService.getQuestionDTO(1L, "creator");

        // 3. Проверяем результат
        assertThat(result).isNotNull();
        assertThat(result.getAnswerOptions()).hasSize(2);
        assertThat(result.getAnswerOptions().get(0).getId()).isEqualTo(1L);
        assertThat(result.getAnswerOptions().get(0).getText()).isEqualTo("Ответ 1");
        assertThat(result.getAnswerOptions().get(0).getIsCorrect()).isTrue();
        assertThat(result.getAnswerOptions().get(1).getId()).isEqualTo(2L);
        assertThat(result.getAnswerOptions().get(1).getText()).isEqualTo("Ответ 2");
        assertThat(result.getAnswerOptions().get(1).getIsCorrect()).isFalse();
    }
}