package com.frist.assesspro.service.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsServiceTest {

    private MeterRegistry meterRegistry;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }

    @Test
    void incrementTestsStarted_ShouldIncreaseCounter() {
        metricsService.incrementTestsStarted();
        double count = meterRegistry.counter("assesspro.tests.started").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void incrementTestsCompleted_ShouldIncreaseCounter() {
        metricsService.incrementTestsCompleted();
        double count = meterRegistry.counter("assesspro.tests.completed").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void incrementUsersRegistered_ShouldIncreaseCounter() {
        metricsService.incrementUsersRegistered();
        double count = meterRegistry.counter("assesspro.users.registered").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void incrementQuestionsCreated_ShouldIncreaseCounter() {
        metricsService.incrementQuestionsCreated();
        double count = meterRegistry.counter("assesspro.questions.created").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void setActiveUsers_ShouldSetGauge() {
        metricsService.setActiveUsers(10L);
        double value = meterRegistry.get("assesspro.users.active").gauge().value();
        assertThat(value).isEqualTo(10.0);
    }

    @Test
    void incrementActiveUsers_ShouldIncreaseGauge() {
        metricsService.setActiveUsers(5L);
        metricsService.incrementActiveUsers();
        double value = meterRegistry.get("assesspro.users.active").gauge().value();
        assertThat(value).isEqualTo(6.0);
    }

    @Test
    void decrementActiveUsers_ShouldDecreaseGauge() {
        metricsService.setActiveUsers(5L);
        metricsService.decrementActiveUsers();
        double value = meterRegistry.get("assesspro.users.active").gauge().value();
        assertThat(value).isEqualTo(4.0);
    }

    @Test
    void startTimer_ShouldReturnSample() {
        var sample = metricsService.startTimer();
        assertThat(sample).isNotNull();
    }

    @Test
    void stopTimer_ShouldRecordTimer() {
        var sample = metricsService.startTimer();
        metricsService.stopTimer(sample, "/test-endpoint");
        var timer = meterRegistry.find("assesspro.request.duration").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }
}