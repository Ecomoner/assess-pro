package com.frist.assesspro.repository;

import com.frist.assesspro.dto.TestDTO;
import com.frist.assesspro.dto.test.AnswerPreviewDTO;
import com.frist.assesspro.dto.test.QuestionPreviewDTO;
import com.frist.assesspro.dto.test.TestInfoDTO;
import com.frist.assesspro.dto.test.TestPreviewDTO;
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
import java.util.Map;
import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<Test, Long>, JpaSpecificationExecutor<Test> {

    @EntityGraph(value = "Test.withCreator", type = EntityGraph.EntityGraphType.LOAD)
    List<Test> findByCreatedBy(User createdBy);

    @EntityGraph(value = "Test.withCreator", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT t FROM Test t WHERE t.id = :id AND t.createdBy = :creator")
    Optional<Test> findByIdAndCreatedBy(@Param("id") Long id, @Param("creator") User createdBy);

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

    @Query("SELECT COUNT(t) FROM Test t")
    long countByTests(@Param("creator") User creator);

    @Query("SELECT COUNT(t) FROM Test t WHERE t.createdBy = :creator AND t.isPublished = true")
    long countPublishedByCreator(@Param("creator") User creator);

    @Query(value = "SELECT COUNT(q.id) FROM assess_pro_db.questions q " +
            "JOIN assess_pro_db.tests t ON q.test_id = t.id " +
            "WHERE t.created_by = :creatorId",
            nativeQuery = true)
    Long countQuestionsByCreatorId(@Param("creatorId") Long creatorId);

    @EntityGraph(value = "Test.withCreatorAndCategory", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT t FROM Test t WHERE t.id = :id AND t.createdBy = :creator")
    Optional<Test> findByIdWithCreatorAndCategory(@Param("id") Long id, @Param("creator") User creator);

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

    @Query("SELECT new com.frist.assesspro.dto.test.TestPreviewDTO(" +
            "t.id, t.title, t.description, t.timeLimitMinutes, " +
            "t.createdBy.id, t.createdBy.username) " +
            "FROM Test t " +
            "WHERE t.id = :testId")
    Optional<TestPreviewDTO> findTestPreviewDTO(@Param("testId") Long testId);

    @Query("SELECT new com.frist.assesspro.dto.test.QuestionPreviewDTO(" +
            "q.id, q.text, q.orderIndex) " +
            "FROM Question q " +
            "WHERE q.test.id = :testId " +
            "ORDER BY q.orderIndex")
    List<QuestionPreviewDTO> findQuestionPreviewDTOs(@Param("testId") Long testId);

    @Query("SELECT new com.frist.assesspro.dto.test.AnswerPreviewDTO(" +
            "a.id, a.text, a.isCorrect) " +
            "FROM AnswerOption a " +
            "WHERE a.question.id = :questionId " +
            "ORDER BY a.id")
    List<AnswerPreviewDTO> findAnswerPreviewDTOs(@Param("questionId") Long questionId);

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


    @Query("SELECT t FROM Test t " +
            "LEFT JOIN FETCH t.questions q " +
            "WHERE t.id = :id AND t.isPublished = true")
    Optional<Test> findByIdAndIsPublishedTrueWithQuestions(@Param("id") Long id);


    @Query("SELECT t.category.name, COUNT(t) FROM Test t " +
            "WHERE t.category IS NOT NULL " +
            "GROUP BY t.category.name")
    List<Object[]> countTestsByCategory();

    @Query("SELECT t.category.name, AVG(ta.totalScore) FROM Test t " +
            "JOIN t.attempts ta " +
            "WHERE t.category IS NOT NULL AND ta.status = 'COMPLETED' " +
            "GROUP BY t.category.name")
    List<Object[]> averageScoreByCategory();



}