package com.frist.assesspro.service;


import com.frist.assesspro.dto.test.*;
import com.frist.assesspro.entity.*;
import com.frist.assesspro.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestPassingService {

    private final TestRepository testRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;

    /**
     * Получение списка доступных тестов как DTO
     */
    @Transactional(readOnly = true)
    public List<TestInfoDTO> getAvailableTestsDTO() {
        List<Test> tests = testRepository.findByIsPublishedTrue();

        return tests.stream()
                .map(this::convertToTestInfoDTO)
                .collect(Collectors.toList());
    }

    private TestInfoDTO convertToTestInfoDTO(Test test) {
        TestInfoDTO dto = new TestInfoDTO();
        dto.setId(test.getId());
        dto.setTitle(test.getTitle());
        dto.setDescription(test.getDescription());
        dto.setQuestionCount(test.getQuestions() != null ? test.getQuestions().size() : 0);
        dto.setTimeLimitMinutes(test.getTimeLimitMinutes());
        dto.setCreatedAt(test.getCreatedAt());
        return dto;
    }

    /**
     * Получение теста для прохождения как DTO
     */
    @Transactional(readOnly = true)
    public Optional<TestTakingDTO> getTestForTaking(Long testId, String username){
        Test test = testRepository.findByIdAndIsPublishedTrue(testId)
                .orElse(null);

        if (test == null) {
            return Optional.empty();
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Optional<TestAttempt> existingAttempt = testAttemptRepository
                .findByTestIdAndUserIdAndStatus(testId, user.getId(), TestAttempt.AttemptStatus.IN_PROGRESS);

        Long attemptId;
        if (existingAttempt.isPresent()){
            attemptId = existingAttempt.get().getId();
        }else {
            TestAttempt attempt = new TestAttempt();
            attempt.setTest(test);
            attempt.setUser(user);
            attempt.setStartTime(LocalDateTime.now());
            attempt.setStatus(TestAttempt.AttemptStatus.IN_PROGRESS);
            attempt.setTotalScore(0);

            TestAttempt savedAttempt = testAttemptRepository.save(attempt);
            attemptId = savedAttempt.getId();
            log.info("Создана новая попытка теста ID: {}", testId);
        }

        List<QuestionForTakingDTO> questionDTOs = test.getQuestions().stream()
                .sorted((q1, q2) -> q1.getOrderIndex().compareTo(q2.getOrderIndex()))
                .map(this::convertToQuestionForTakingDTO)
                .collect(Collectors.toList());

        TestTakingDTO dto = new TestTakingDTO();
        dto.setAttemptId(attemptId);
        dto.setTestId(test.getId());
        dto.setTestTitle(test.getTitle());
        dto.setTimeLimitMinutes(test.getTimeLimitMinutes());
        dto.setQuestions(questionDTOs);
        dto.setTotalQuestions(questionDTOs.size());

        return Optional.of(dto);
    }

    private QuestionForTakingDTO convertToQuestionForTakingDTO(Question question) {
        QuestionForTakingDTO dto = new QuestionForTakingDTO();
        dto.setId(question.getId());
        dto.setText(question.getText());
        dto.setOrderIndex(question.getOrderIndex());

        List<QuestionForTakingDTO.AnswerOptionForTakingDTO> answerDTOs = question.getAnswerOptions().stream()
                .map(this::convertToAnswerOptionForTakingDTO)
                .collect(Collectors.toList());

        dto.setAnswerOptions(answerDTOs);
        return dto;
    }

    private QuestionForTakingDTO.AnswerOptionForTakingDTO convertToAnswerOptionForTakingDTO(AnswerOption answer) {
        QuestionForTakingDTO.AnswerOptionForTakingDTO dto = new QuestionForTakingDTO.AnswerOptionForTakingDTO();
        dto.setId(answer.getId());
        dto.setText(answer.getText());
        return dto;
    }

    /**
     * Сохранение ответа
     */
    @Transactional
    public void saveAnswer(TestPassingDTO testPassingDTO, String username){
        TestAttempt attempt = testAttemptRepository.findById(testPassingDTO.getAttemptId())
                .orElseThrow(() -> new RuntimeException("Попытка не найдена!"));

        if (!attempt.getUser().getUsername().equals(username)){
            throw new RuntimeException("Нет прав доступа к этой попытке!");
        }

        if(attempt.getStatus() != TestAttempt.AttemptStatus.IN_PROGRESS){
            throw new RuntimeException("Попытка уже завершена!");
        }

        Question question = questionRepository.findById(testPassingDTO.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Вопрос не найден"));

        if (!question.getTest().getId().equals(attempt.getTest().getId())) {
            throw new RuntimeException("Вопрос не принадлежит этому тесту");
        }

        AnswerOption chosenAnswer = null;

        if(testPassingDTO.getAnswerOptionId() != null){
            chosenAnswer = answerOptionRepository.findById(testPassingDTO.getAnswerOptionId())
                    .orElseThrow(() -> new RuntimeException("Вариант ответа не найдент!"));
        }

        Optional<UserAnswer> existingAnswer = userAnswerRepository
                .findByAttemptIdAndQuestionId(attempt.getId(), question.getId());

        UserAnswer userAnswer;
        if (existingAnswer.isPresent()) {
            userAnswer = existingAnswer.get();
            userAnswer.setChosenAnswerOption(chosenAnswer);
        } else {
            userAnswer = new UserAnswer();
            userAnswer.setAttempt(attempt);
            userAnswer.setQuestion(question);
            userAnswer.setChosenAnswerOption(chosenAnswer);
        }

        // Определяем, правильный ли ответ
        if (chosenAnswer != null) {
            userAnswer.setIsCorrect(chosenAnswer.getIsCorrect());
            userAnswer.setPointsEarned(chosenAnswer.getIsCorrect() ? 1 : 0);
        } else {
            userAnswer.setIsCorrect(false);
            userAnswer.setPointsEarned(0);
        }

        userAnswerRepository.save(userAnswer);

        // Обновляем общий счет
        updateAttemptTotalScore(attempt.getId());
    }

    /**
     * Завершение теста и получение результатов как DTO
     */
    @Transactional
    public TestResultsDTO finishTestAndGetResults(Long attemptId, String username){
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Попытка не найдена"));

        if (!attempt.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Нет доступа к этой попытке");
        }

        if (attempt.getStatus() == TestAttempt.AttemptStatus.IN_PROGRESS){
            attempt.setStatus(TestAttempt.AttemptStatus.COMPLETED);
            attempt.setEndTime(LocalDateTime.now());
            testAttemptRepository.save(attempt);
            log.info("Завершена попытка теста ID: {}", attempt.getTest().getId());
        }
        return getTestResults(attemptId, username);
    }

    /**
     * Получение результатов теста как DTO
     */
    @Transactional(readOnly = true)
    public TestResultsDTO getTestResults(Long attemptId, String username) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Попытка не найдена"));


        if (!attempt.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Нет доступа к этой попытке");
        }

        Test test = attempt.getTest();


        List<UserAnswer> userAnswers = userAnswerRepository.findByAttemptId(attemptId);


        List<QuestionResultDTO> questionResults = userAnswers.stream()
                .map(this::convertToQuestionResultDTO)
                .collect(Collectors.toList());


        int totalQuestions = test.getQuestions().size();
        int answeredQuestions = userAnswers.size();
        int correctAnswers = (int) userAnswers.stream()
                .filter(answer -> Boolean.TRUE.equals(answer.getIsCorrect()))
                .count();
        int maxPossibleScore = totalQuestions; // По 1 баллу за каждый правильный ответ

        TestResultsDTO dto = new TestResultsDTO();
        dto.setAttemptId(attempt.getId());
        dto.setTestId(test.getId());  // ← УСТАНАВЛИВАЕМ testId!
        dto.setTestTitle(test.getTitle());
        dto.setStartTime(attempt.getStartTime());
        dto.setEndTime(attempt.getEndTime());
        dto.setTotalScore(attempt.getTotalScore() != null ? attempt.getTotalScore() : 0);
        dto.setMaxPossibleScore(maxPossibleScore);
        dto.setTotalQuestions(totalQuestions);
        dto.setAnsweredQuestions(answeredQuestions);
        dto.setCorrectAnswers(correctAnswers);
        dto.setQuestionResults(questionResults);

        return dto;
    }

    private QuestionResultDTO convertToQuestionResultDTO(UserAnswer userAnswer){
        QuestionResultDTO dto = new QuestionResultDTO();
        dto.setQuestionId(userAnswer.getQuestion().getId());
        dto.setQuestionText(userAnswer.getQuestion().getText());

        if (userAnswer.getChosenAnswerOption() != null) {
            dto.setChosenAnswerId(userAnswer.getChosenAnswerOption().getId());
            dto.setChosenAnswerText(userAnswer.getChosenAnswerOption().getText());
        }

        Optional<AnswerOption> correctAnswer = userAnswer.getQuestion().getAnswerOptions().stream()
                .filter(AnswerOption::getIsCorrect)
                .findFirst();

        if (correctAnswer.isPresent()) {
            dto.setCorrectAnswerId(correctAnswer.get().getId());
            dto.setCorrectAnswerText(correctAnswer.get().getText());
        }

        dto.setIsCorrect(userAnswer.getIsCorrect());
        dto.setPointsEarned(userAnswer.getPointsEarned());

        return dto;
    }

    /**
     * Получение истории тестов как DTO
     */
    @Transactional(readOnly = true)
    public List<TestHistoryDTO> getUserTestHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<TestAttempt> attempts = testAttemptRepository.findByUserIdOrderByStartTimeDesc(user.getId());

        return attempts.stream()
                .map(this::convertToTestHistoryDTO)
                .collect(Collectors.toList());
    }

    private TestHistoryDTO convertToTestHistoryDTO(TestAttempt attempt) {
        TestHistoryDTO dto = new TestHistoryDTO();
        dto.setAttemptId(attempt.getId());
        dto.setTestId(attempt.getTest().getId());
        dto.setTestTitle(attempt.getTest().getTitle());
        dto.setStartTime(attempt.getStartTime());
        dto.setEndTime(attempt.getEndTime());
        dto.setStatus(attempt.getStatus().toString());
        dto.setTotalScore(attempt.getTotalScore() != null ? attempt.getTotalScore() : 0);
        dto.setMaxPossibleScore(attempt.getTest().getQuestions().size());
        return dto;
    }


    /**
     * Обновление общего счета попытки
     */
    private void updateAttemptTotalScore(Long attemptId) {
        Integer totalScore = userAnswerRepository.sumPointsEarnedByAttemptId(attemptId);
        if (totalScore == null) {
            totalScore = 0;
        }

        testAttemptRepository.updateTotalScore(attemptId, totalScore);
    }
}
