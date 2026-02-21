package com.frist.assesspro.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@NamedEntityGraph(
        name = "Question.withAnswers",
        attributeNodes = @NamedAttributeNode("answerOptions")
)
@NamedEntityGraph(
        name = "Question.withTestAndAnswers",
        attributeNodes = {
                @NamedAttributeNode("test"),
                @NamedAttributeNode("answerOptions")
        }
)
@Entity
@Table(name = "questions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Cacheable
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Тест обязателен")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @NotBlank(message = "Текст вопроса обязателен")
    @Size(min = 5, max = 1000, message = "Текст вопроса должен быть от 5 до 1000 символов")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Min(value = 0, message = "Порядковый номер не может быть отрицательным")
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    @OneToMany(mappedBy = "question",
            cascade = CascadeType.ALL,      // Все операции каскадируются
            orphanRemoval = true,           // Удаляем сирот
            fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @ToString.Exclude
    private List<AnswerOption> answerOptions = new ArrayList<>();

    // ВЕРНУТЬ LIST
    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @ToString.Exclude
    private List<UserAnswer> userAnswers = new ArrayList<>();

    public void addAnswerOption(AnswerOption answerOption) {
        answerOptions.add(answerOption);
        answerOption.setQuestion(this);
    }

    public void removeAnswerOption(AnswerOption answerOption) {
        answerOptions.remove(answerOption);
        answerOption.setQuestion(null);
    }

    public void clearAnswerOptions() {
        for (AnswerOption answer : new ArrayList<>(answerOptions)) {
            removeAnswerOption(answer);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Question question = (Question) o;
        return id != null && id.equals(question.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
