package com.frist.assesspro.service;

import com.frist.assesspro.dto.TestDTO;
import com.frist.assesspro.dto.test.TestUpdateDTO;
import com.frist.assesspro.entity.*;
import com.frist.assesspro.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerOptionRepository answerOptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TestService testService;

    private User creator;
    private Test test;
    private TestDTO testDTO;
    private Category category;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setUsername("creator");
        creator.setRole(User.Roles.CREATOR);

        category = new Category();
        category.setId(1L);
        category.setName("Категория 1");

        test = new Test();
        test.setId(1L);
        test.setTitle("Тест 1");
        test.setDescription("Описание");
        test.setCreatedBy(creator);
        test.setIsPublished(false);
        test.setTimeLimitMinutes(30);
        test.setRetryCooldownHours(24);
        test.setRetryCooldownDays(1);
        test.setCategory(category);
        test.setCreatedAt(LocalDateTime.now());

        testDTO = new TestDTO();
        testDTO.setTitle("Новый тест");
        testDTO.setDescription("Новое описание");
        testDTO.setTimeLimitMinutes(45);
        testDTO.setRetryCooldownHours(12);
        testDTO.setRetryCooldownDays(0);
        testDTO.setCategoryId(1L);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Успешное создание теста")
    void createTest_Success() {
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(testRepository.findByCreatedBy(creator)).thenReturn(List.of());
        when(testRepository.save(any(Test.class))).thenAnswer(invocation -> {
            Test savedTest = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedTest, "id", 1L);
            return savedTest;
        });

        Test createdTest = testService.createTest(testDTO, "creator");

        assertThat(createdTest).isNotNull();
        assertThat(createdTest.getId()).isEqualTo(1L);
        assertThat(createdTest.getTitle()).isEqualTo("Новый тест");
        assertThat(createdTest.getRetryCooldownHours()).isEqualTo(12);

        verify(userRepository).findByUsername("creator");
        verify(categoryRepository).findById(1L);
        verify(testRepository).save(any(Test.class));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Создание теста с пустым названием - ошибка")
    void createTest_EmptyTitle_ThrowsException() {
        testDTO.setTitle("   ");

        assertThatThrownBy(() -> testService.createTest(testDTO, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Название теста обязательно");

        verify(userRepository, never()).findByUsername(anyString());
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Создание теста с существующим названием - ошибка")
    void createTest_DuplicateTitle_ThrowsException() {
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(testRepository.findByCreatedBy(creator)).thenReturn(List.of(test));

        testDTO.setTitle(test.getTitle());

        assertThatThrownBy(() -> testService.createTest(testDTO, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Тест с таким названием уже существует");

        verify(testRepository, never()).save(any(Test.class));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Получение теста по ID - успешно")
    void getTestById_Success() {
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(testRepository.findByIdAndCreatedBy(1L, creator)).thenReturn(Optional.of(test));

        Optional<Test> foundTest = testService.getTestById(1L, "creator");

        assertThat(foundTest).isPresent();
        assertThat(foundTest.get().getId()).isEqualTo(1L);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Получение несуществующего теста - пустой результат")
    void getTestById_NotFound_ReturnsEmpty() {
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(testRepository.findByIdAndCreatedBy(99L, creator)).thenReturn(Optional.empty());

        Optional<Test> foundTest = testService.getTestById(99L, "creator");

        assertThat(foundTest).isEmpty();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Обновление теста - успешно")
    void updateTest_Success() {
        TestUpdateDTO updateDTO = new TestUpdateDTO();
        updateDTO.setTitle("Обновленный тест");
        updateDTO.setDescription("Новое описание");
        updateDTO.setTimeLimitMinutes(60);
        updateDTO.setRetryCooldownHours(48);
        updateDTO.setCategoryId(1L);

        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(testRepository.save(any(Test.class))).thenReturn(test);

        Test updatedTest = testService.updateTest(1L, updateDTO, "creator");

        assertThat(updatedTest).isNotNull();
        assertThat(updatedTest.getTitle()).isEqualTo("Обновленный тест");
        verify(testRepository).save(test);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Обновление чужого теста - ошибка")
    void updateTest_NotOwner_ThrowsException() {
        TestUpdateDTO updateDTO = new TestUpdateDTO();
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));

        assertThatThrownBy(() -> testService.updateTest(1L, updateDTO, "another"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Нет прав");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Публикация теста - успешно")
    void switchPublishStatus_Publish_Success() {
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));

        // Создаём вопрос с ответом
        Question question = new Question();
        question.setId(1L);
        AnswerOption answer = new AnswerOption();
        answer.setId(1L);
        answer.setIsCorrect(true);
        question.setAnswerOptions(List.of(answer));

        when(questionRepository.findQuestionsWithAnswersByTestId(1L)).thenReturn(List.of(question));
        when(testRepository.save(any(Test.class))).thenReturn(test);

        Test publishedTest = testService.switchPublishStatus(1L, "creator", true);

        assertThat(publishedTest.getIsPublished()).isTrue();
        verify(testRepository).save(test);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Публикация теста без вопросов - ошибка")
    void switchPublishStatus_NoQuestions_ThrowsException() {
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        when(questionRepository.findQuestionsWithAnswersByTestId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> testService.switchPublishStatus(1L, "creator", true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Нельзя опубликовать тест без вопросов");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Удаление теста - успешно")
    void deleteTest_Success() {
        test.setAttempts(List.of()); // Нет попыток

        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        when(questionRepository.findByTestIdOrderByOrderIndex(1L)).thenReturn(List.of());

        testService.deleteTest(1L, "creator");

        verify(testRepository).delete(test);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Удаление теста с попытками - ошибка")
    void deleteTest_WithAttempts_ThrowsException() {
        test.setAttempts(List.of(new com.frist.assesspro.entity.TestAttempt()));

        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));

        assertThatThrownBy(() -> testService.deleteTest(1L, "creator"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Нельзя удалить тест, у которого есть попытки");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Проверка владельца теста - true")
    void isTestOwner_ReturnsTrue() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));

        boolean isOwner = testService.isTestOwner(1L, "creator");

        assertThat(isOwner).isTrue();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Проверка владельца теста - false")
    void isTestOwner_ReturnsFalse() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));

        boolean isOwner = testService.isTestOwner(1L, "another");

        assertThat(isOwner).isFalse();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Конвертация Test в TestDTO")
    void convertToDTO_Success() {
        TestDTO converted = testService.convertToDTO(test);

        assertThat(converted).isNotNull();
        assertThat(converted.getId()).isEqualTo(1L);
        assertThat(converted.getTitle()).isEqualTo("Тест 1");
        assertThat(converted.getCategoryId()).isEqualTo(1L);
        assertThat(converted.getCategoryName()).isEqualTo("Категория 1");
    }
}