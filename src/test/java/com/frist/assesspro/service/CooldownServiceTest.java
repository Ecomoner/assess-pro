package com.frist.assesspro.service;

import com.frist.assesspro.entity.RetryCooldownException;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.TestAttempt;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.RetryCooldownExceptionRepository;
import com.frist.assesspro.repository.TestAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CooldownServiceTest {

    @Mock
    private TestAttemptRepository testAttemptRepository;

    @Mock
    private RetryCooldownExceptionRepository exceptionRepository;

    private CooldownService cooldownService;

    private Clock fixedClock;
    private LocalDateTime now;
    private Test test;
    private User user;
    private User creator;
    private TestAttempt lastAttempt;
    private RetryCooldownException exception;

    @BeforeEach
    void setUp() {
        // Фиксируем время на 2025-02-25T10:00:00Z
        fixedClock = Clock.fixed(Instant.parse("2025-02-25T10:00:00Z"), ZoneId.systemDefault());
        now = LocalDateTime.now(fixedClock);

        cooldownService = new CooldownService(testAttemptRepository, exceptionRepository, fixedClock);

        test = new Test();
        test.setId(1L);
        test.setRetryCooldownHours(24);
        test.setRetryCooldownDays(1);

        user = new User();
        user.setId(1L);
        user.setUsername("tester");

        creator = new User();
        creator.setId(2L);
        creator.setUsername("creator");

        lastAttempt = new TestAttempt();
        lastAttempt.setId(1L);
        lastAttempt.setTest(test);
        lastAttempt.setUser(user);
        lastAttempt.setStatus(TestAttempt.AttemptStatus.COMPLETED);

        exception = new RetryCooldownException();
        exception.setId(1L);
        exception.setTest(test);
        exception.setUser(user);
        exception.setCreatedBy(creator);
        exception.setIsPermanent(true);
    }

    // ==================== canUserTakeTest тесты ====================

    @org.junit.jupiter.api.Test
    @DisplayName("canUserTakeTest: нет ограничений (cooldown = 0) -> true")
    void canUserTakeTest_NoCooldown_ReturnsTrue() {
        test.setRetryCooldownHours(0);
        test.setRetryCooldownDays(0);

        boolean result = cooldownService.canUserTakeTest(test, user);

        assertThat(result).isTrue();
        verifyNoInteractions(exceptionRepository, testAttemptRepository);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("canUserTakeTest: есть активное исключение -> true")
    void canUserTakeTest_HasException_ReturnsTrue() {
        when(exceptionRepository.hasActiveException(eq(test), eq(user), any(LocalDateTime.class)))
                .thenReturn(true);

        boolean result = cooldownService.canUserTakeTest(test, user);

        assertThat(result).isTrue();
        verify(exceptionRepository).hasActiveException(eq(test), eq(user), any(LocalDateTime.class));
        verifyNoInteractions(testAttemptRepository);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("canUserTakeTest: нет завершенных попыток -> true")
    void canUserTakeTest_NoCompletedAttempts_ReturnsTrue() {
        when(exceptionRepository.hasActiveException(eq(test), eq(user), any(LocalDateTime.class)))
                .thenReturn(false);
        when(testAttemptRepository.findByTestIdAndUserIdAndStatus(
                eq(test.getId()), eq(user.getId()), eq(TestAttempt.AttemptStatus.COMPLETED)))
                .thenReturn(Optional.empty());

        boolean result = cooldownService.canUserTakeTest(test, user);

        assertThat(result).isTrue();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("canUserTakeTest: есть попытка, но время еще не прошло -> false")
    void canUserTakeTest_AttemptNotExpired_ReturnsFalse() {
        lastAttempt.setEndTime(now.minusHours(12)); // прошло 12 часов, осталось 12
        when(exceptionRepository.hasActiveException(eq(test), eq(user), any(LocalDateTime.class)))
                .thenReturn(false);
        when(testAttemptRepository.findByTestIdAndUserIdAndStatus(
                eq(test.getId()), eq(user.getId()), eq(TestAttempt.AttemptStatus.COMPLETED)))
                .thenReturn(Optional.of(lastAttempt));

        boolean result = cooldownService.canUserTakeTest(test, user);

        assertThat(result).isFalse();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("canUserTakeTest: есть попытка и время прошло -> true")
    void canUserTakeTest_AttemptExpired_ReturnsTrue() {
        lastAttempt.setEndTime(now.minusHours(48)); // прошло 48 часов
        when(exceptionRepository.hasActiveException(eq(test), eq(user), any(LocalDateTime.class)))
                .thenReturn(false);
        when(testAttemptRepository.findByTestIdAndUserIdAndStatus(
                eq(test.getId()), eq(user.getId()), eq(TestAttempt.AttemptStatus.COMPLETED)))
                .thenReturn(Optional.of(lastAttempt));

        boolean result = cooldownService.canUserTakeTest(test, user);

        assertThat(result).isTrue();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("canUserTakeTest: используется effectiveCooldownHours (дни)")
    void canUserTakeTest_UseEffectiveCooldownHours() {
        test.setRetryCooldownDays(2);
        test.setRetryCooldownHours(48); // добавляем явную синхронизацию
        lastAttempt.setEndTime(now.minusHours(30));

        when(exceptionRepository.hasActiveException(eq(test), eq(user), any(LocalDateTime.class)))
                .thenReturn(false);
        when(testAttemptRepository.findByTestIdAndUserIdAndStatus(
                eq(test.getId()), eq(user.getId()), eq(TestAttempt.AttemptStatus.COMPLETED)))
                .thenReturn(Optional.of(lastAttempt));

        boolean result = cooldownService.canUserTakeTest(test, user);
        assertThat(result).isFalse();
    }

    // ==================== createException тесты ====================

    @org.junit.jupiter.api.Test
    @DisplayName("createException: успешное создание (срок действия)")
    void createException_WithExpiry_Success() {
        Integer hours = 48;
        boolean permanent = false;
        String reason = "Тестовое исключение";

        when(exceptionRepository.save(any(RetryCooldownException.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RetryCooldownException created = cooldownService.createException(
                test, user, creator, hours, permanent, reason);

        assertThat(created).isNotNull();
        assertThat(created.getTest()).isEqualTo(test);
        assertThat(created.getUser()).isEqualTo(user);
        assertThat(created.getCreatedBy()).isEqualTo(creator);
        assertThat(created.getIsPermanent()).isFalse();
        assertThat(created.getReason()).isEqualTo(reason);
        assertThat(created.getExpiresAt()).isNotNull();
        assertThat(created.getExpiresAt()).isEqualTo(now.plusHours(48));

        verify(exceptionRepository).deleteByTestAndUser(test, user);
        verify(exceptionRepository).save(any(RetryCooldownException.class));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("createException: успешное создание (постоянное)")
    void createException_Permanent_Success() {
        boolean permanent = true;
        String reason = "Постоянное исключение";

        when(exceptionRepository.save(any(RetryCooldownException.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RetryCooldownException created = cooldownService.createException(
                test, user, creator, null, permanent, reason);

        assertThat(created).isNotNull();
        assertThat(created.getIsPermanent()).isTrue();
        assertThat(created.getExpiresAt()).isNull();
        verify(exceptionRepository).deleteByTestAndUser(test, user);
        verify(exceptionRepository).save(any(RetryCooldownException.class));
    }

    // ==================== removeException тесты ====================

    @org.junit.jupiter.api.Test
    @DisplayName("removeException: успешное удаление")
    void removeException_Success() {
        cooldownService.removeException(test, user);
        verify(exceptionRepository).deleteByTestAndUser(test, user);
    }

    // ==================== getNextAvailableTime тесты ====================

    @org.junit.jupiter.api.Test
    @DisplayName("getNextAvailableTime: без ограничений -> сейчас")
    void getNextAvailableTime_NoCooldown_ReturnsNow() {
        test.setRetryCooldownHours(0);
        LocalDateTime next = cooldownService.getNextAvailableTime(test, user);
        assertThat(next).isEqualTo(now);
        verifyNoInteractions(exceptionRepository, testAttemptRepository);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("getNextAvailableTime: есть активное исключение -> сейчас")
    void getNextAvailableTime_HasException_ReturnsNow() {
        when(exceptionRepository.hasActiveException(eq(test), eq(user), any(LocalDateTime.class)))
                .thenReturn(true);

        LocalDateTime next = cooldownService.getNextAvailableTime(test, user);

        assertThat(next).isEqualTo(now);
        verify(exceptionRepository).hasActiveException(eq(test), eq(user), any(LocalDateTime.class));
        verifyNoInteractions(testAttemptRepository);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("getNextAvailableTime: нет завершенных попыток -> сейчас")
    void getNextAvailableTime_NoAttempts_ReturnsNow() {
        when(exceptionRepository.hasActiveException(eq(test), eq(user), any(LocalDateTime.class)))
                .thenReturn(false);
        when(testAttemptRepository.findByTestIdAndUserIdAndStatus(
                eq(test.getId()), eq(user.getId()), eq(TestAttempt.AttemptStatus.COMPLETED)))
                .thenReturn(Optional.empty());

        LocalDateTime next = cooldownService.getNextAvailableTime(test, user);

        assertThat(next).isEqualTo(now);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("getNextAvailableTime: есть попытка -> время последней + cooldown")
    void getNextAvailableTime_HasAttempt_ReturnsLastAttemptPlusCooldown() {
        LocalDateTime lastAttemptTime = now.minusHours(12);
        lastAttempt.setEndTime(lastAttemptTime);

        when(exceptionRepository.hasActiveException(eq(test), eq(user), any(LocalDateTime.class)))
                .thenReturn(false);
        when(testAttemptRepository.findByTestIdAndUserIdAndStatus(
                eq(test.getId()), eq(user.getId()), eq(TestAttempt.AttemptStatus.COMPLETED)))
                .thenReturn(Optional.of(lastAttempt));

        LocalDateTime expected = lastAttemptTime.plusHours(test.getEffectiveCooldownHours());
        LocalDateTime next = cooldownService.getNextAvailableTime(test, user);

        assertThat(next).isEqualTo(expected);
    }

    // ==================== getCooldownStatus тесты ====================

    @org.junit.jupiter.api.Test
    @DisplayName("getCooldownStatus: без ограничений -> 'Доступно'")
    void getCooldownStatus_NoCooldown_ReturnsAvailable() {
        test.setRetryCooldownHours(0);
        String status = cooldownService.getCooldownStatus(test, user);
        assertThat(status).isEqualTo("Доступно");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("getCooldownStatus: с исключением -> 'Доступно (исключение)'")
    void getCooldownStatus_HasException_ReturnsException() {
        when(exceptionRepository.hasActiveException(eq(test), eq(user), any(LocalDateTime.class)))
                .thenReturn(true);

        String status = cooldownService.getCooldownStatus(test, user);

        assertThat(status).isEqualTo("Доступно (исключение)");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("getCooldownStatus: нет попыток -> 'Доступно'")
    void getCooldownStatus_NoAttempts_ReturnsAvailable() {
        when(exceptionRepository.hasActiveException(eq(test), eq(user), any(LocalDateTime.class)))
                .thenReturn(false);
        when(testAttemptRepository.findByTestIdAndUserIdAndStatus(
                eq(test.getId()), eq(user.getId()), eq(TestAttempt.AttemptStatus.COMPLETED)))
                .thenReturn(Optional.empty());

        String status = cooldownService.getCooldownStatus(test, user);

        assertThat(status).isEqualTo("Доступно");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("getCooldownStatus: время не прошло (осталось > 24 часов)")
    void getCooldownStatus_RemainingDays() {
        test.setRetryCooldownDays(0);     // отключаем дни
        test.setRetryCooldownHours(72);   // 3 дня
        lastAttempt.setEndTime(now.minusHours(10)); // прошло 10 часов → осталось 62 часа

        when(exceptionRepository.hasActiveException(eq(test), eq(user), any(LocalDateTime.class)))
                .thenReturn(false);
        when(testAttemptRepository.findByTestIdAndUserIdAndStatus(
                eq(test.getId()), eq(user.getId()), eq(TestAttempt.AttemptStatus.COMPLETED)))
                .thenReturn(Optional.of(lastAttempt));

        String status = cooldownService.getCooldownStatus(test, user);

        assertThat(status).startsWith("Недоступно еще 2 дня");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("getCooldownStatus: время не прошло (осталось менее 24 часов)")
    void getCooldownStatus_RemainingHours() {
        test.setRetryCooldownHours(24);
        lastAttempt.setEndTime(now.minusHours(10)); // осталось 14 часов

        when(exceptionRepository.hasActiveException(eq(test), eq(user), any(LocalDateTime.class)))
                .thenReturn(false);
        when(testAttemptRepository.findByTestIdAndUserIdAndStatus(
                eq(test.getId()), eq(user.getId()), eq(TestAttempt.AttemptStatus.COMPLETED)))
                .thenReturn(Optional.of(lastAttempt));

        String status = cooldownService.getCooldownStatus(test, user);

        assertThat(status).startsWith("Недоступно еще 14 час");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("getCooldownStatus: время не прошло (осталось менее часа)")
    void getCooldownStatus_RemainingMinutes() {
        test.setRetryCooldownHours(24);
        lastAttempt.setEndTime(now.minusHours(23).minusMinutes(30)); // осталось 30 минут

        when(exceptionRepository.hasActiveException(eq(test), eq(user), any(LocalDateTime.class)))
                .thenReturn(false);
        when(testAttemptRepository.findByTestIdAndUserIdAndStatus(
                eq(test.getId()), eq(user.getId()), eq(TestAttempt.AttemptStatus.COMPLETED)))
                .thenReturn(Optional.of(lastAttempt));

        String status = cooldownService.getCooldownStatus(test, user);

        assertThat(status).startsWith("Недоступно еще 30 мин");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("getCooldownStatus: время прошло -> 'Доступно'")
    void getCooldownStatus_Expired_ReturnsAvailable() {
        lastAttempt.setEndTime(now.minusHours(48));

        when(exceptionRepository.hasActiveException(eq(test), eq(user), any(LocalDateTime.class)))
                .thenReturn(false);
        when(testAttemptRepository.findByTestIdAndUserIdAndStatus(
                eq(test.getId()), eq(user.getId()), eq(TestAttempt.AttemptStatus.COMPLETED)))
                .thenReturn(Optional.of(lastAttempt));

        String status = cooldownService.getCooldownStatus(test, user);

        assertThat(status).isEqualTo("Доступно");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("getCooldownStatus: если endTime == null, используем startTime")
    void getCooldownStatus_UseStartTimeWhenEndNull() {
        lastAttempt.setEndTime(null);
        lastAttempt.setStartTime(now.minusHours(30));

        when(exceptionRepository.hasActiveException(eq(test), eq(user), any(LocalDateTime.class)))
                .thenReturn(false);
        when(testAttemptRepository.findByTestIdAndUserIdAndStatus(
                eq(test.getId()), eq(user.getId()), eq(TestAttempt.AttemptStatus.COMPLETED)))
                .thenReturn(Optional.of(lastAttempt));

        String status = cooldownService.getCooldownStatus(test, user);

        // 30 часов прошло, cooldown 24 → доступно
        assertThat(status).isEqualTo("Доступно");
    }
}