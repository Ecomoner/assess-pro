package com.frist.assesspro.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final Counter testsStartedCounter;
    private final Counter testsCompletedCounter;
    private final Counter usersRegisteredCounter;
    private final Counter questionsCreatedCounter;
    private final AtomicLong activeUsers;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Счетчики
        this.testsStartedCounter = Counter.builder("assesspro.tests.started")
                .description("Количество начатых тестов")
                .register(meterRegistry);

        this.testsCompletedCounter = Counter.builder("assesspro.tests.completed")
                .description("Количество завершенных тестов")
                .register(meterRegistry);

        this.usersRegisteredCounter = Counter.builder("assesspro.users.registered")
                .description("Количество зарегистрированных пользователей")
                .register(meterRegistry);

        this.questionsCreatedCounter = Counter.builder("assesspro.questions.created")
                .description("Количество созданных вопросов")
                .register(meterRegistry);

        // Гейдж для активных пользователей
        this.activeUsers = meterRegistry.gauge("assesspro.users.active", new AtomicLong(0));

        // Таймер для измерения времени ответа
        Timer.Sample sample = Timer.start(meterRegistry);
    }

    // Методы для инкремента счетчиков
    public void incrementTestsStarted() {
        testsStartedCounter.increment();
    }

    public void incrementTestsCompleted() {
        testsCompletedCounter.increment();
    }

    public void incrementUsersRegistered() {
        usersRegisteredCounter.increment();
    }

    public void incrementQuestionsCreated() {
        questionsCreatedCounter.increment();
    }

    // Методы для работы с активными пользователями
    public void setActiveUsers(long count) {
        activeUsers.set(count);
    }

    public void incrementActiveUsers() {
        activeUsers.incrementAndGet();
    }

    public void decrementActiveUsers() {
        activeUsers.decrementAndGet();
    }

    // Метод для измерения времени выполнения
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopTimer(Timer.Sample sample, String endpoint) {
        sample.stop(Timer.builder("assesspro.request.duration")
                .description("Время выполнения запроса")
                .tag("endpoint", endpoint)
                .register(meterRegistry));
    }
}
