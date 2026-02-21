package com.frist.assesspro.repository;

import com.frist.assesspro.dto.TestDTO;
import com.frist.assesspro.dto.test.TestInfoDTO;
import com.frist.assesspro.entity.Question;
import com.frist.assesspro.entity.Test;

import com.frist.assesspro.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<Test, Long>, JpaSpecificationExecutor<Test> {

    @EntityGraph(value = "Test.withCreator", type = EntityGraph.EntityGraphType.LOAD)
    List<Test> findByCreatedBy(User createdBy);

    @EntityGraph(value = "Test.withCreator", type = EntityGraph.EntityGraphType.LOAD)
    Page<Test> findByCreatedBy(User createdBy, Pageable pageable);

    @EntityGraph(value = "Test.withCreator", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT t FROM Test t WHERE t.id = :id AND t.createdBy = :creator")
    Optional<Test> findByIdAndCreatedBy(@Param("id") Long id, @Param("creator") User createdBy);

    @EntityGraph(value = "Test.withQuestions", type = EntityGraph.EntityGraphType.LOAD)
    List<Test> findByIsPublishedTrue();

    @EntityGraph(value = "Test.withQuestions", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Test> findByIdAndIsPublishedTrue(Long id);

    // DTO проекция для опубликованных тестов (используем LEFT JOIN)
    @Query("SELECT new com.frist.assesspro.dto.test.TestInfoDTO(" +
            "t.id, t.title, t.description, " +
            "COUNT(q.id), t.timeLimitMinutes, t.createdAt, " +
            "t.category.id, t.category.name) " +
            "FROM Test t " +
            "LEFT JOIN t.questions q " +
            "WHERE t.isPublished = true " +
            "GROUP BY t.id, t.title, t.description, t.timeLimitMinutes, t.createdAt, " +
            "t.category.id, t.category.name " +
            "ORDER BY t.createdAt DESC")
    List<TestInfoDTO> findPublishedTestInfoDTOs();

    // Оптимизированные счетчики
    @Query("SELECT COUNT(t) FROM Test t")
    long countByTests(@Param("creator") User creator);

    @Query("SELECT COUNT(t) FROM Test t WHERE t.createdBy = :creator AND t.isPublished = true")
    long countPublishedByCreator(@Param("creator") User creator);

    // Для DashboardService
    @Query("SELECT t FROM Test t WHERE t.createdBy = :creator")
    List<Test> findAllByCreatedBy(@Param("creator") User creator);

    @Query(value = "SELECT COUNT(q.id) FROM assess_pro_db.questions q " +
            "JOIN assess_pro_db.tests t ON q.test_id = t.id " +
            "WHERE t.created_by = :creatorId",
            nativeQuery = true)
    Long countQuestionsByCreatorId(@Param("creatorId") Long creatorId);

    @EntityGraph(value = "Test.withQuestionsAndAnswers", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT t FROM Test t WHERE t.id = :id AND t.createdBy = :creator")
    Optional<Test> findByIdWithQuestionsAndAnswers(@Param("id") Long id, @Param("creator") User creator);

    @EntityGraph(value = "Test.withQuestionsAndAnswers", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT t FROM Test t WHERE t.id = :id")
    Optional<Test> findByIdWithQuestionsAndAnswers(@Param("id") Long id);

    @Query("SELECT new com.frist.assesspro.dto.test.TestInfoDTO(" +
            "t.id, t.title, t.description, " +
            "COUNT(q.id), t.timeLimitMinutes, t.createdAt, " +
            "t.category.id, t.category.name) " +
            "FROM Test t " +
            "LEFT JOIN t.questions q " +
            "WHERE t.isPublished = true AND t.category.id = :categoryId " +
            "GROUP BY t.id, t.title, t.description, t.timeLimitMinutes, t.createdAt, " +
            "t.category.id, t.category.name " +
            "ORDER BY t.createdAt DESC")
    List<TestInfoDTO> findPublishedTestInfoDTOsByCategoryId(@Param("categoryId") Long categoryId);

    // ============= СТАРЫЕ МЕТОДЫ (ИСПРАВЛЕНЫ) =============

    /**
     * 🔧 ИСПРАВЛЕНО: Добавлены creatorId и creatorUsername в конструктор
     */
    @Query("SELECT new com.frist.assesspro.dto.TestDTO(" +
            "t.id, t.title, t.description, t.isPublished, " +
            "COUNT(q.id), t.createdAt, t.timeLimitMinutes, " +
            "t.retryCooldownHours, t.retryCooldownDays, " +
            "t.category.id, t.category.name, " +
            "t.createdBy.id, t.createdBy.username) " +
            "FROM Test t " +
            "LEFT JOIN t.questions q " +
            "WHERE t.createdBy = :creator " +
            "GROUP BY t.id, t.title, t.description, t.isPublished, t.createdAt, " +
            "t.timeLimitMinutes, t.retryCooldownHours, t.retryCooldownDays, " +
            "t.category.id, t.category.name, t.createdBy.id, t.createdBy.username " +
            "ORDER BY t.createdAt DESC")
    Page<TestDTO> findTestDTOsByCreator(@Param("creator") User creator, Pageable pageable);

    /**
     * 🔧 ИСПРАВЛЕНО: Добавлены creatorId и creatorUsername в конструктор
     */
    @Query("SELECT new com.frist.assesspro.dto.TestDTO(" +
            "t.id, t.title, t.description, t.isPublished, " +
            "COUNT(q.id), t.createdAt, t.timeLimitMinutes, " +
            "t.category.id, t.category.name, " +
            "t.createdBy.id, t.createdBy.username) " +
            "FROM Test t " +
            "LEFT JOIN t.questions q " +
            "WHERE t.id IN :testIds " +
            "GROUP BY t.id, t.title, t.description, t.isPublished, t.createdAt, " +
            "t.timeLimitMinutes, t.category.id, t.category.name, t.createdBy.id, t.createdBy.username " +
            "ORDER BY t.createdAt DESC")
    Page<TestDTO> findTestDTOsByIds(@Param("testIds") List<Long> testIds, Pageable pageable);

    /**
     * 🔧 ИСПРАВЛЕНО: Добавлены creatorId и creatorUsername в конструктор
     */
    @Query("SELECT new com.frist.assesspro.dto.TestDTO(" +
            "t.id, t.title, t.description, t.isPublished, " +
            "COUNT(q.id), t.createdAt, t.timeLimitMinutes, " +
            "t.retryCooldownHours, t.retryCooldownDays, " +
            "t.category.id, t.category.name, " +
            "t.createdBy.id, t.createdBy.username) " +
            "FROM Test t " +
            "LEFT JOIN t.questions q " +
            "WHERE t.createdBy = :creator " +
            "AND LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "GROUP BY t.id, t.title, t.description, t.isPublished, t.createdAt, " +
            "t.timeLimitMinutes, t.retryCooldownHours, t.retryCooldownDays, " +
            "t.category.id, t.category.name, t.createdBy.id, t.createdBy.username " +
            "ORDER BY t.createdAt DESC")
    Page<TestDTO> searchTestsByCreator(@Param("creator") User creator,
                                       @Param("searchTerm") String searchTerm,
                                       Pageable pageable);

    /**
     * 🔧 ИСПРАВЛЕНО: Добавлены creatorId и creatorUsername в конструктор
     */
    @Query("SELECT new com.frist.assesspro.dto.TestDTO(" +
            "t.id, t.title, t.description, t.isPublished, " +
            "COUNT(q.id), t.createdAt, t.timeLimitMinutes, " +
            "t.retryCooldownHours, t.retryCooldownDays, " +
            "t.category.id, t.category.name, " +
            "t.createdBy.id, t.createdBy.username) " +
            "FROM Test t " +
            "LEFT JOIN t.questions q " +
            "GROUP BY t.id, t.title, t.description, t.isPublished, t.createdAt, " +
            "t.timeLimitMinutes, t.retryCooldownHours, t.retryCooldownDays, " +
            "t.category.id, t.category.name, t.createdBy.id, t.createdBy.username " +
            "ORDER BY t.createdAt DESC")
    Page<TestDTO> findAllTestDTOs(Pageable pageable);

    // ============= НОВЫЙ МЕТОД =============

    /**
     * 🔥 НОВОЕ: Получение ВСЕХ тестов с фильтрацией (для создателя)
     */
    @Query("SELECT new com.frist.assesspro.dto.TestDTO(" +
            "t.id, t.title, t.description, t.isPublished, " +
            "COUNT(q.id), t.createdAt, t.timeLimitMinutes, " +
            "t.retryCooldownHours, t.retryCooldownDays, " +
            "t.category.id, t.category.name, " +
            "t.createdBy.id, t.createdBy.username) " +
            "FROM Test t " +
            "LEFT JOIN t.questions q " +
            "WHERE (:status IS NULL OR :status = '' " +
            "       OR (:status = 'published' AND t.isPublished = true) " +
            "       OR (:status = 'draft' AND t.isPublished = false)) " +
            "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
            "AND (:creatorId IS NULL OR t.createdBy.id = :creatorId) " +
            "AND (:search IS NULL OR :search = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "GROUP BY t.id, t.title, t.description, t.isPublished, t.createdAt, t.timeLimitMinutes, " +
            "t.retryCooldownHours, t.retryCooldownDays, t.category.id, t.category.name, " +
            "t.createdBy.id, t.createdBy.username " +
            "ORDER BY t.createdAt DESC")
    Page<TestDTO> findAllTestsWithFilters(@Param("status") String status,
                                          @Param("categoryId") Long categoryId,
                                          @Param("creatorId") Long creatorId,
                                          @Param("search") String search,
                                          Pageable pageable);

    // ============= ОСТАЛЬНЫЕ МЕТОДЫ =============

    @Query("SELECT new com.frist.assesspro.dto.test.TestInfoDTO(" +
            "t.id, t.title, t.description, " +
            "COUNT(q.id), t.timeLimitMinutes, t.createdAt, " +
            "t.category.id, t.category.name) " +
            "FROM Test t " +
            "LEFT JOIN t.questions q " +
            "WHERE t.isPublished = true " +
            "GROUP BY t.id, t.title, t.description, t.timeLimitMinutes, t.createdAt, " +
            "t.category.id, t.category.name")
    Page<TestInfoDTO> findPublishedTestInfoDTOs(Pageable pageable);

    @Query("SELECT new com.frist.assesspro.dto.test.TestInfoDTO(" +
            "t.id, t.title, t.description, " +
            "COUNT(q.id), t.timeLimitMinutes, t.createdAt, " +
            "t.category.id, t.category.name) " +
            "FROM Test t " +
            "LEFT JOIN t.questions q " +
            "WHERE t.isPublished = true AND t.category.id = :categoryId " +
            "GROUP BY t.id, t.title, t.description, t.timeLimitMinutes, t.createdAt, " +
            "t.category.id, t.category.name")
    Page<TestInfoDTO> findPublishedTestInfoDTOsByCategoryId(
            @Param("categoryId") Long categoryId,
            Pageable pageable);

    @Query("SELECT new com.frist.assesspro.dto.test.TestInfoDTO(" +
            "t.id, t.title, t.description, " +
            "COUNT(q.id), t.timeLimitMinutes, t.createdAt, " +
            "t.category.id, t.category.name) " +
            "FROM Test t " +
            "LEFT JOIN t.questions q " +
            "WHERE t.isPublished = true " +
            "AND LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "GROUP BY t.id, t.title, t.description, t.timeLimitMinutes, t.createdAt, " +
            "t.category.id, t.category.name " +
            "ORDER BY t.createdAt DESC")
    List<TestInfoDTO> searchPublishedTests(@Param("searchTerm") String searchTerm);

    @Query("SELECT new com.frist.assesspro.dto.test.TestInfoDTO(" +
            "t.id, t.title, t.description, " +
            "COUNT(q.id), t.timeLimitMinutes, t.createdAt, " +
            "t.category.id, t.category.name) " +
            "FROM Test t " +
            "LEFT JOIN t.questions q " +
            "WHERE t.isPublished = true " +
            "AND LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "GROUP BY t.id, t.title, t.description, t.timeLimitMinutes, t.createdAt, " +
            "t.category.id, t.category.name")
    Page<TestInfoDTO> searchPublishedTests(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Test t WHERE t.isPublished = :isPublished")
    long countByIsPublished(@Param("isPublished") boolean isPublished);

    @Query("SELECT COUNT(t) FROM Test t WHERE t.createdBy = :creator")
    long countByCreatedBy(@Param("creator") User creator);

    @Query("SELECT COUNT(t) FROM Test t")
    long countAllTests();

    @Query("SELECT t FROM Test t " +
            "LEFT JOIN FETCH t.category " +
            "WHERE t.id = :id")
    Optional<Test> findByIdWithCategory(@Param("id") Long id);

    @Query("SELECT q FROM Question q " +
            "LEFT JOIN FETCH q.answerOptions " +
            "WHERE q.test.id = :testId " +
            "ORDER BY q.orderIndex")
    List<Question> findQuestionsWithAnswersByTestId(@Param("testId") Long testId);
}