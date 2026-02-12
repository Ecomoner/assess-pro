package com.frist.assesspro.service;

import com.frist.assesspro.entity.RetryCooldownException;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.TestAttempt;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.RetryCooldownExceptionRepository;
import com.frist.assesspro.repository.TestAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CooldownService {

    private final TestAttemptRepository testAttemptRepository;
    private final RetryCooldownExceptionRepository exceptionRepository;

    /**
     * Проверка, может ли пользователь пройти тест сейчас
     */
    @Transactional(readOnly = true)
    public boolean canUserTakeTest(Test test, User user) {
        // 1. Если нет ограничений - можно
        if (!test.hasRetryCooldown()) {
            return true;
        }

        // 2. Проверяем активные исключения
        if (exceptionRepository.hasActiveException(test, user, LocalDateTime.now())) {
            log.debug("Пользователь {} имеет исключение для теста {}", user.getUsername(), test.getId());
            return true;
        }

        // 3. Получаем последнюю завершенную попытку
        Optional<TestAttempt> lastAttempt = testAttemptRepository
                .findByTestIdAndUserIdAndStatus(
                        test.getId(),
                        user.getId(),
                        TestAttempt.AttemptStatus.COMPLETED);

        if (lastAttempt.isEmpty()) {
            // Ни разу не проходил - можно
            return true;
        }

        // 4. Проверяем время с последней попытки
        LocalDateTime lastAttemptTime = lastAttempt.get().getEndTime();
        if (lastAttemptTime == null) {
            lastAttemptTime = lastAttempt.get().getStartTime();
        }

        LocalDateTime nextAllowedTime = lastAttemptTime.plusHours(test.getEffectiveCooldownHours());
        boolean canTake = LocalDateTime.now().isAfter(nextAllowedTime);

        if (!canTake) {
            log.debug("Пользователь {} не может пройти тест {} до {}",
                    user.getUsername(), test.getId(), nextAllowedTime);
        }

        return canTake;
    }

    /**
     * Создание исключения для пользователя (снятие ограничений)
     */
    @Transactional
    public RetryCooldownException createException(Test test, User user, User creator,
                                                  Integer hours, boolean permanent, String reason) {
        // Удаляем старые исключения
        exceptionRepository.deleteByTestAndUser(test, user);

        RetryCooldownException exception = new RetryCooldownException();
        exception.setTest(test);
        exception.setUser(user);
        exception.setCreatedBy(creator);
        exception.setIsPermanent(permanent);
        exception.setReason(reason);

        if (!permanent && hours != null && hours > 0) {
            exception.setExpiresAt(LocalDateTime.now().plusHours(hours));
        }

        RetryCooldownException saved = exceptionRepository.save(exception);

        log.info("Создано исключение для пользователя {} по тесту {} создателем {}",
                user.getUsername(), test.getId(), creator.getUsername());

        return saved;
    }

    /**
     * Удаление исключения
     */
    @Transactional
    public void removeException(Test test, User user) {
        exceptionRepository.deleteByTestAndUser(test, user);
        log.info("Удалено исключение для пользователя {} по тесту {}",
                user.getUsername(), test.getId());
    }

    /**
     * Получение времени следующей доступной попытки
     */
    @Transactional(readOnly = true)
    public LocalDateTime getNextAvailableTime(Test test, User user) {
        if (!test.hasRetryCooldown()) {
            return LocalDateTime.now();
        }

        if (exceptionRepository.hasActiveException(test, user, LocalDateTime.now())) {
            return LocalDateTime.now();
        }

        Optional<TestAttempt> lastAttempt = testAttemptRepository
                .findByTestIdAndUserIdAndStatus(
                        test.getId(),
                        user.getId(),
                        TestAttempt.AttemptStatus.COMPLETED);

        if (lastAttempt.isEmpty()) {
            return LocalDateTime.now();
        }

        LocalDateTime lastAttemptTime = lastAttempt.get().getEndTime();
        if (lastAttemptTime == null) {
            lastAttemptTime = lastAttempt.get().getStartTime();
        }

        return lastAttemptTime.plusHours(test.getEffectiveCooldownHours());
    }

    /**
     * Получение статуса ограничения для отображения
     */
    @Transactional(readOnly = true)
    public String getCooldownStatus(Test test, User user) {
        if (!test.hasRetryCooldown()) {
            return "Доступно";
        }

        if (exceptionRepository.hasActiveException(test, user, LocalDateTime.now())) {
            return "Доступно (исключение)";
        }

        Optional<TestAttempt> lastAttempt = testAttemptRepository
                .findByTestIdAndUserIdAndStatus(
                        test.getId(),
                        user.getId(),
                        TestAttempt.AttemptStatus.COMPLETED);

        if (lastAttempt.isEmpty()) {
            return "Доступно";
        }

        LocalDateTime nextAllowed = getNextAvailableTime(test, user);
        if (LocalDateTime.now().isAfter(nextAllowed)) {
            return "Доступно";
        }

        // Форматируем оставшееся время
        long hoursRemaining = java.time.Duration.between(LocalDateTime.now(), nextAllowed).toHours();
        if (hoursRemaining > 24) {
            long daysRemaining = hoursRemaining / 24;
            return "Недоступно еще " + daysRemaining + " " + getDaysWord((int) daysRemaining);
        } else if (hoursRemaining > 0) {
            return "Недоступно еще " + hoursRemaining + " " + getHoursWord((int) hoursRemaining);
        } else {
            long minutesRemaining = java.time.Duration.between(LocalDateTime.now(), nextAllowed).toMinutes();
            return "Недоступно еще " + minutesRemaining + " мин";
        }
    }

    private String getDaysWord(int days) {
        if (days % 10 == 1 && days % 100 != 11) return "день";
        if (days % 10 >= 2 && days % 10 <= 4 && (days % 100 < 10 || days % 100 >= 20)) return "дня";
        return "дней";
    }

    private String getHoursWord(int hours) {
        if (hours % 10 == 1 && hours % 100 != 11) return "час";
        if (hours % 10 >= 2 && hours % 10 <= 4 && (hours % 100 < 10 || hours % 100 >= 20)) return "часа";
        return "часов";
    }
}