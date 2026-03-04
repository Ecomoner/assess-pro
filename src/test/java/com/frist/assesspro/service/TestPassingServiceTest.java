package com.frist.assesspro.service;

import com.frist.assesspro.dto.UserStatisticsDTO;
import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.dto.test.*;
import com.frist.assesspro.entity.*;
import com.frist.assesspro.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestPassingServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private TestAttemptRepository testAttemptRepository;

    @Mock
    private UserAnswerRepository userAnswerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerOptionRepository answerOptionRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ProfileService profileService;

    @Mock
    private CooldownService cooldownService;

    @InjectMocks
    private TestPassingService testPassingService;

    private User tester;
    private com.frist.assesspro.entity.Test test;
    private TestAttempt attempt;
    private Question question;
    private AnswerOption answer;
    private TestInfoDTO testInfoDTO;
    private CategoryDTO categoryDTO;

    @BeforeEach
    void setUp() {
        tester = new User();
        tester.setId(1L);
        tester.setUsername("tester");
        tester.setFirstName("Иван");
        tester.setLastName("Петров");
        tester.setIsProfileComplete(true);

        test = new com.frist.assesspro.entity.Test();
        test.setId(1L);
        test.setTitle("Тест по математике");
        test.setIsPublished(true);
        test.setTimeLimitMinutes(30);
        test.setQuestions(new ArrayList<>());

        question = new Question();
        question.setId(1L);
        question.setText("2+2=?");
        question.setOrderIndex(1);
        question.setTest(test);

        answer = new AnswerOption();
        answer.setId(1L);
        answer.setText("4");
        answer.setIsCorrect(true);
        answer.setQuestion(question);

        question.setAnswerOptions(List.of(answer));
        test.setQuestions(List.of(question));

        attempt = new TestAttempt();
        attempt.setId(1L);
        attempt.setTest(test);
        attempt.setUser(tester);
        attempt.setStartTime(LocalDateTime.now().minusHours(1));
        attempt.setEndTime(LocalDateTime.now());
        attempt.setTotalScore(1);
        attempt.setStatus(TestAttempt.AttemptStatus.COMPLETED);

        testInfoDTO = new TestInfoDTO();
        testInfoDTO.setId(1L);
        testInfoDTO.setTitle("Тест по математике");
        testInfoDTO.setQuestionCount(1L);
        testInfoDTO.setTimeLimitMinutes(30);

        categoryDTO = new CategoryDTO();
        categoryDTO.setId(1L);
        categoryDTO.setName("Математика");
    }

    @Test
    @DisplayName("getAllAvailableTestsDTOPaginated: успешное получение тестов с пагинацией")
    void getAllAvailableTestsDTOPaginated_Success() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<TestInfoDTO> page = new PageImpl<>(List.of(testInfoDTO), pageable, 1);

        when(testRepository.findPublishedTestInfoDTOs(pageable)).thenReturn(page);

        Page<TestInfoDTO> result = testPassingService.getAllAvailableTestsDTOPaginated(0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        verify(testRepository).findPublishedTestInfoDTOs(pageable);
    }

    @Test
    @DisplayName("getAvailableTestsByCategoryDTOPaginated: по категории")
    void getAvailableTestsByCategoryDTOPaginated_Success() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<TestInfoDTO> page = new PageImpl<>(List.of(testInfoDTO), pageable, 1);

        when(testRepository.findPublishedTestInfoDTOsByCategoryId(1L, pageable)).thenReturn(page);

        Page<TestInfoDTO> result = testPassingService.getAvailableTestsByCategoryDTOPaginated(1L, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        verify(testRepository).findPublishedTestInfoDTOsByCategoryId(1L, pageable);
    }

    @Test
    @DisplayName("getAvailableTestsByCategoryDTOPaginated: без категории")
    void getAvailableTestsByCategoryDTOPaginated_NullCategory() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<TestInfoDTO> page = new PageImpl<>(List.of(testInfoDTO), pageable, 1);

        when(testRepository.findPublishedTestInfoDTOs(pageable)).thenReturn(page);

        Page<TestInfoDTO> result = testPassingService.getAvailableTestsByCategoryDTOPaginated(null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        verify(testRepository).findPublishedTestInfoDTOs(pageable);
    }

    @Test
    @DisplayName("getTestForTaking: успешное начало прохождения")
    void getTestForTaking_Success() {
        when(profileService.isProfileComplete("tester")).thenReturn(true);
        when(testRepository.findByIdAndIsPublishedTrueWithQuestions(1L)).thenReturn(Optional.of(test));
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(tester));
        when(cooldownService.canUserTakeTest(test, tester)).thenReturn(true);
        when(testAttemptRepository.findByTestIdAndUserIdAndStatus(1L, 1L, TestAttempt.AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(testAttemptRepository.save(any(TestAttempt.class))).thenAnswer(invocation -> {
            TestAttempt saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(userAnswerRepository.findByAttemptId(2L)).thenReturn(List.of());

        Optional<TestTakingDTO> result = testPassingService.getTestForTaking(1L, "tester");

        assertThat(result).isPresent();
        TestTakingDTO dto = result.get();
        assertThat(dto.getAttemptId()).isEqualTo(2L);
        assertThat(dto.getTestId()).isEqualTo(1L);
        assertThat(dto.getQuestions()).hasSize(1);
        assertThat(dto.getAnsweredQuestions()).isEqualTo(0);
        assertThat(dto.getRemainingQuestions()).isEqualTo(1);
    }

    @Test
    @DisplayName("getTestForTaking: профиль не заполнен -> ошибка")
    void getTestForTaking_ProfileNotComplete_ThrowsException() {
        when(profileService.isProfileComplete("tester")).thenReturn(false);

        assertThatThrownBy(() -> testPassingService.getTestForTaking(1L, "tester"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("необходимо заполнить профиль");
    }

    @Test
    @DisplayName("getTestForTaking: тест не опубликован -> пусто")
    void getTestForTaking_TestNotFound() {
        when(profileService.isProfileComplete("tester")).thenReturn(true);
        when(testRepository.findByIdAndIsPublishedTrueWithQuestions(1L)).thenReturn(Optional.empty());

        Optional<TestTakingDTO> result = testPassingService.getTestForTaking(1L, "tester");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTestForTaking: ограничение cooldown -> ошибка")
    void getTestForTaking_Cooldown_ThrowsException() {
        when(profileService.isProfileComplete("tester")).thenReturn(true);
        when(testRepository.findByIdAndIsPublishedTrueWithQuestions(1L)).thenReturn(Optional.of(test));
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(tester));
        when(cooldownService.canUserTakeTest(test, tester)).thenReturn(false);
        when(cooldownService.getNextAvailableTime(test, tester)).thenReturn(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> testPassingService.getTestForTaking(1L, "tester"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Следующая попытка доступна");
    }

    @Test
    @DisplayName("getTestForTaking: продолжение существующей попытки")
    void getTestForTaking_ContinueAttempt() {
        TestAttempt inProgress = new TestAttempt();
        inProgress.setId(3L);
        inProgress.setTest(test);
        inProgress.setUser(tester);
        inProgress.setStatus(TestAttempt.AttemptStatus.IN_PROGRESS);

        when(profileService.isProfileComplete("tester")).thenReturn(true);
        when(testRepository.findByIdAndIsPublishedTrueWithQuestions(1L)).thenReturn(Optional.of(test));
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(tester));
        when(cooldownService.canUserTakeTest(test, tester)).thenReturn(true);
        when(testAttemptRepository.findByTestIdAndUserIdAndStatus(1L, 1L, TestAttempt.AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(inProgress));
        when(userAnswerRepository.findByAttemptId(3L)).thenReturn(List.of());

        Optional<TestTakingDTO> result = testPassingService.getTestForTaking(1L, "tester");

        assertThat(result).isPresent();
        assertThat(result.get().getAttemptId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getTestForTaking: все вопросы отвечены -> возврат пусто")
    void getTestForTaking_AllQuestionsAnswered() {
        when(profileService.isProfileComplete("tester")).thenReturn(true);
        when(testRepository.findByIdAndIsPublishedTrueWithQuestions(1L)).thenReturn(Optional.of(test));
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(tester));
        when(cooldownService.canUserTakeTest(test, tester)).thenReturn(true);

        // Возвращаем существующую попытку (уже есть ответы)
        TestAttempt existingAttempt = new TestAttempt();
        existingAttempt.setId(1L);
        existingAttempt.setTest(test);
        existingAttempt.setUser(tester);
        existingAttempt.setStatus(TestAttempt.AttemptStatus.IN_PROGRESS);
        when(testAttemptRepository.findByTestIdAndUserIdAndStatus(1L, 1L, TestAttempt.AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(existingAttempt));

        // Мокаем ответы для этой попытки
        UserAnswer answer = new UserAnswer();
        answer.setQuestion(question);
        when(userAnswerRepository.findByAttemptId(1L)).thenReturn(List.of(answer));

        // Мокаем завершение теста (при завершении сохраняется попытка)
        when(testAttemptRepository.save(any(TestAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<TestTakingDTO> result = testPassingService.getTestForTaking(1L, "tester");

        assertThat(result).isEmpty();
        verify(testAttemptRepository).save(argThat(ta -> ta.getStatus() == TestAttempt.AttemptStatus.COMPLETED));
        verify(testAttemptRepository, times(1)).save(any()); // только один save при завершении
    }

    @Test
    @DisplayName("saveAnswer: успешное сохранение")
    void saveAnswer_Success() {
        // Убеждаемся, что статус IN_PROGRESS
        attempt.setStatus(TestAttempt.AttemptStatus.IN_PROGRESS);

        // Добавляем второй вопрос, чтобы не завершать тест
        Question question2 = new Question();
        question2.setId(2L);
        test.setQuestions(List.of(question, question2));

        TestPassingDTO passingDTO = new TestPassingDTO();
        passingDTO.setAttemptId(1L);
        passingDTO.setQuestionId(1L);
        passingDTO.setAnswerOptionId(1L);

        when(testAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(answerOptionRepository.findById(1L)).thenReturn(Optional.of(answer));
        doNothing().when(userAnswerRepository).upsertAnswer(anyLong(), anyLong(), anyLong(), anyBoolean(), anyInt());
        when(userAnswerRepository.sumPointsEarnedByAttemptId(1L)).thenReturn(1);
        when(userAnswerRepository.countByAttemptId(1L)).thenReturn(1L); // ответили на 1 из 2

        testPassingService.saveAnswer(passingDTO, "tester");

        verify(userAnswerRepository).upsertAnswer(1L, 1L, 1L, true, 1);
        verify(testAttemptRepository).updateTotalScore(1L, 1);
        verify(testAttemptRepository, never()).save(any(TestAttempt.class)); // не завершён
    }

    @Test
    @DisplayName("saveAnswer: попытка не принадлежит пользователю -> ошибка")
    void saveAnswer_NotOwner_ThrowsException() {
        TestPassingDTO passingDTO = new TestPassingDTO();
        passingDTO.setAttemptId(1L);

        when(testAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> testPassingService.saveAnswer(passingDTO, "another"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Нет прав доступа");
    }

    @Test
    @DisplayName("saveAnswer: последний вопрос -> тест завершается")
    void saveAnswer_LastQuestion_FinishesTest() {
        // Убеждаемся, что статус IN_PROGRESS
        attempt.setStatus(TestAttempt.AttemptStatus.IN_PROGRESS);
        // Только один вопрос
        test.setQuestions(List.of(question));

        TestPassingDTO passingDTO = new TestPassingDTO();
        passingDTO.setAttemptId(1L);
        passingDTO.setQuestionId(1L);
        passingDTO.setAnswerOptionId(1L);

        // Моки для сохранения ответа
        when(testAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(answerOptionRepository.findById(1L)).thenReturn(Optional.of(answer));
        doNothing().when(userAnswerRepository).upsertAnswer(anyLong(), anyLong(), anyLong(), anyBoolean(), anyInt());
        when(userAnswerRepository.sumPointsEarnedByAttemptId(1L)).thenReturn(1);
        when(userAnswerRepository.countByAttemptId(1L)).thenReturn(1L); // все вопросы отвечены

        // Моки для finishTestAndGetResults
        // второй вызов findById
        when(testAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));
        // для getTestResults нужен userAnswerRepository.findByAttemptId
        when(userAnswerRepository.findByAttemptId(1L)).thenReturn(List.of());
        // для сохранения
        when(testAttemptRepository.save(any(TestAttempt.class))).thenReturn(attempt);

        testPassingService.saveAnswer(passingDTO, "tester");

        verify(userAnswerRepository).upsertAnswer(1L, 1L, 1L, true, 1);
        verify(testAttemptRepository, atLeastOnce()).save(attempt);
        assertThat(attempt.getStatus()).isEqualTo(TestAttempt.AttemptStatus.COMPLETED);
    }

    @Test
    @DisplayName("finishTestAndGetResults: успешное завершение")
    void finishTestAndGetResults_Success() {
        when(testAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));
        when(userAnswerRepository.findByAttemptId(1L)).thenReturn(List.of());

        TestResultsDTO result = testPassingService.finishTestAndGetResults(1L, "tester");

        assertThat(result).isNotNull();
        assertThat(result.getAttemptId()).isEqualTo(1L);
        assertThat(result.getTestId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getTestResults: получение результатов")
    void getTestResults_Success() {
        UserAnswer ua = new UserAnswer();
        ua.setQuestion(question);
        ua.setChosenAnswerOption(answer);
        ua.setIsCorrect(true);
        ua.setPointsEarned(1);

        when(testAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));
        when(userAnswerRepository.findByAttemptId(1L)).thenReturn(List.of(ua));

        TestResultsDTO result = testPassingService.getTestResults(1L, "tester");

        assertThat(result.getTotalScore()).isEqualTo(1);
        assertThat(result.getCorrectAnswers()).isEqualTo(1);
        assertThat(result.getQuestionResults()).hasSize(1);
    }

    @Test
    @DisplayName("getUserTestHistory: история с пагинацией")
    void getUserTestHistory_WithPagination_Success() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("startTime").descending());
        TestHistoryDTO historyDTO = new TestHistoryDTO();
        historyDTO.setAttemptId(1L);
        historyDTO.setStatus(TestAttempt.AttemptStatus.COMPLETED);

        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(tester));
        Page<TestHistoryDTO> page = new PageImpl<>(List.of(historyDTO), pageable, 1);
        when(testAttemptRepository.findTestHistoryDTOsByUserId(1L, pageable)).thenReturn(page);

        Page<TestHistoryDTO> result = testPassingService.getUserTestHistory("tester", 0, 10, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProgressPercentage()).isEqualTo(100); // COMPLETED
    }

    @Test
    @DisplayName("getUserTestHistory: история с in-progress и подсчётом прогресса")
    void getUserTestHistory_WithProgress() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("startTime").descending());
        TestHistoryDTO historyDTO = new TestHistoryDTO();
        historyDTO.setAttemptId(1L);
        historyDTO.setStatus(TestAttempt.AttemptStatus.IN_PROGRESS);
        historyDTO.setMaxPossibleScore(10L);

        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(tester));
        Page<TestHistoryDTO> page = new PageImpl<>(List.of(historyDTO), pageable, 1);
        when(testAttemptRepository.findTestHistoryDTOsByUserId(1L, pageable)).thenReturn(page);

        List<Object[]> countResult = new ArrayList<>();
        countResult.add(new Object[]{1L, 5L});
        when(userAnswerRepository.countByAttemptIds(List.of(1L))).thenReturn(countResult);

        Page<TestHistoryDTO> result = testPassingService.getUserTestHistory("tester", 0, 10, null);

        assertThat(result.getContent().get(0).getProgressPercentage()).isEqualTo(50); // 5/10
    }

    @Test
    @DisplayName("getUserTestHistory: без пагинации")
    void getUserTestHistory_WithoutPagination_Success() {
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(tester));
        Pageable pageable = PageRequest.of(0, 10, Sort.by("startTime").descending());
        Page<TestHistoryDTO> page = new PageImpl<>(List.of(), pageable, 0);
        when(testAttemptRepository.findTestHistoryDTOsByUserId(1L, pageable)).thenReturn(page);

        List<TestHistoryDTO> result = testPassingService.getUserTestHistory("tester");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getUserStatistics: успешное получение статистики пользователя")
    void getUserStatistics_Success() {
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(tester));
        when(testAttemptRepository.countByUserId(1L)).thenReturn(10L);
        when(testAttemptRepository.countByUserIdAndStatus(1L, TestAttempt.AttemptStatus.COMPLETED)).thenReturn(7L);
        when(testAttemptRepository.countByUserIdAndStatus(1L, TestAttempt.AttemptStatus.IN_PROGRESS)).thenReturn(2L);
        when(testAttemptRepository.findAverageScoreByUserId(1L)).thenReturn(75.5);

        UserStatisticsDTO stats = testPassingService.getUserStatistics("tester");

        assertThat(stats.getTotalAttempts()).isEqualTo(10L);
        assertThat(stats.getCompletedAttempts()).isEqualTo(7L);
        assertThat(stats.getInProgressAttempts()).isEqualTo(2L);
        assertThat(stats.getAverageScore()).isEqualTo(75.5);
    }

    @Test
    @DisplayName("getAvailableCategories: получение категорий через categoryService")
    void getAvailableCategories_Success() {
        when(categoryService.getAllActiveCategories()).thenReturn(List.of(categoryDTO));

        List<CategoryDTO> result = testPassingService.getAvailableCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Математика");
    }

    @Test
    @DisplayName("searchTests: поиск тестов")
    void searchTests_Success() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<TestInfoDTO> page = new PageImpl<>(List.of(testInfoDTO), pageable, 1);
        when(testRepository.searchPublishedTests("математика", pageable)).thenReturn(page);

        Page<TestInfoDTO> result = testPassingService.searchTests("математика", 0, 10);

        assertThat(result.getContent()).hasSize(1);
        verify(testRepository).searchPublishedTests("математика", pageable);
    }

    @Test
    @DisplayName("searchTests: пустой поиск -> все тесты")
    void searchTests_EmptySearch() {
        when(testRepository.findPublishedTestInfoDTOs(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testInfoDTO)));

        Page<TestInfoDTO> result = testPassingService.searchTests(null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("quickSearchTests: быстрый поиск")
    void quickSearchTests_Success() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<TestInfoDTO> page = new PageImpl<>(List.of(testInfoDTO), pageable, 1);
        when(testRepository.searchPublishedTests("мат", pageable)).thenReturn(page);

        List<TestInfoDTO> result = testPassingService.quickSearchTests("мат", 5);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("quickSearchTests: слишком короткий запрос -> пусто")
    void quickSearchTests_TooShort() {
        List<TestInfoDTO> result = testPassingService.quickSearchTests("м", 5);

        assertThat(result).isEmpty();
        verifyNoInteractions(testRepository);
    }
}