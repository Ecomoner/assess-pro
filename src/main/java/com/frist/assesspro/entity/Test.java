package com.frist.assesspro.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@NamedEntityGraph(
        name = "Test.withCreator",
        attributeNodes = @NamedAttributeNode("createdBy")
)
@NamedEntityGraph(
        name = "Test.withCreatorAndCategory",
        attributeNodes = {
                @NamedAttributeNode("createdBy"),
                @NamedAttributeNode("category")
        }
)
@NamedEntityGraph(
        name = "Test.withQuestionsAndAnswers",
        attributeNodes = {
                @NamedAttributeNode("createdBy"),
                @NamedAttributeNode("category"),
                @NamedAttributeNode(value = "questions", subgraph = "questions.answers")
        },
        subgraphs = @NamedSubgraph(
                name = "questions.answers",
                attributeNodes = @NamedAttributeNode("answerOptions")
        )
)
@Entity
@Table(name = "tests")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Cacheable
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название теста обязательно")
    @Size(min = 3, max = 200, message = "Название теста должно быть от 3 до 200 символов")
    @Column(nullable = false)
    private String title;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Создатель обязателен")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_published")
    private Boolean isPublished = false;

    @Min(value = 0, message = "Лимит времени не может быть отрицательным")
    @Max(value = 300, message = "Максимальный лимит времени - 300 минут")
    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes = 0;


    @Min(value = 0)
    @Max(value = 336)  // 14 дней = 336 часов
    @Column(name = "retry_cooldown_hours")
    private Integer retryCooldownHours = 0;

    @Min(value = 0)
    @Max(value = 14)
    @Column(name = "retry_cooldown_days")
    private Integer retryCooldownDays = 0;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    @Size(max = 100, message = "Максимальное количество вопросов в тесте - 100")
    @ToString.Exclude
    @BatchSize(size = 20)
    private List<Question> questions;

    @OneToMany(mappedBy = "test", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<TestAttempt> attempts;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Transient
    public int getQuestionCount() {
        return questions != null ? questions.size() : 0;
    }

    public void validateForPublishing() {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalStateException("Нельзя опубликовать тест без вопросов");
        }

        if (questions.size() > 100) {
            throw new IllegalStateException("Максимальное количество вопросов - 100");
        }

        for (Question question : questions) {
            if (question.getAnswerOptions() == null || question.getAnswerOptions().size() < 2) {
                throw new IllegalStateException("Каждый вопрос должен иметь минимум 2 варианта ответа");
            }
        }
    }
    @Transient
    public boolean hasRetryCooldown() {
        return retryCooldownHours != null && retryCooldownHours > 0;
    }
    @Transient
    public int getEffectiveCooldownHours() {
        if (retryCooldownDays != null && retryCooldownDays > 0) {
            return retryCooldownDays * 24;
        }
        return retryCooldownHours != null ? retryCooldownHours : 0;
    }

    @Transient
    public String getRetryCooldownDisplay() {
        if (retryCooldownDays != null && retryCooldownDays > 0) {
            return retryCooldownDays + " " + getDaysWord(retryCooldownDays);
        }
        if (retryCooldownHours != null && retryCooldownHours > 0) {
            return retryCooldownHours + " " + getHoursWord(retryCooldownHours);
        }
        return "Нет ограничений";
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Test test = (Test) o;
        return id != null && id.equals(test.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}