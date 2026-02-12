package com.frist.assesspro.util;

import com.frist.assesspro.entity.TestAttempt;

public final class TestConstants {

    private TestConstants() {}

    // Статусы попыток
    public static final String STATUS_IN_PROGRESS = TestAttempt.AttemptStatus.IN_PROGRESS.name();
    public static final String STATUS_COMPLETED = TestAttempt.AttemptStatus.COMPLETED.name();
    public static final String STATUS_TIMEOUT = TestAttempt.AttemptStatus.TIMEOUT.name();
    public static final String STATUS_CANCELLED = TestAttempt.AttemptStatus.CANCELLED.name();

    // Роли
    public static final String ROLE_CREATOR = "ROLE_CREATOR";
    public static final String ROLE_TESTER = "ROLE_TESTER";

    // Лимиты
    public static final int MAX_TEST_TITLE_LENGTH = 200;
    public static final int MAX_TEST_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_QUESTION_TEXT_LENGTH = 1000;
    public static final int MAX_ANSWER_TEXT_LENGTH = 500;
    public static final int MAX_TIME_LIMIT_MINUTES = 300;
    public static final int MAX_QUESTIONS_PER_TEST = 100;

    // Пагинация по умолчанию
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
}