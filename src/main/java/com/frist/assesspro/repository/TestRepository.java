package com.frist.assesspro.repository;

import com.frist.assesspro.dto.TestDTO;
import com.frist.assesspro.dto.test.TestInfoDTO;
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
    @Query("SELECT COUNT(t) FROM Test t WHERE t.createdBy = :creator")
    long countByCreator(@Param("creator") User creator);

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
    // Обновляем существующий метод для DTO
    @Query("SELECT new com.frist.assesspro.dto.TestDTO(" +
            "t.id, t.title, t.description, t.isPublished, " +
            "COUNT(q.id), t.createdAt, t.timeLimitMinutes, " +
            "t.category.id, t.category.name) " +  // Добавляем категорию
            "FROM Test t " +
            "LEFT JOIN t.questions q " +
            "WHERE t.createdBy = :creator " +
            "GROUP BY t.id, t.title, t.description, t.isPublished, t.createdAt, " +
            "t.timeLimitMinutes, t.category.id, t.category.name " +
            "ORDER BY t.createdAt DESC")
    Page<TestDTO> findTestDTOsByCreator(@Param("creator") User creator, Pageable pageable);

    @Query("SELECT new com.frist.assesspro.dto.TestDTO(" +
            "t.id, t.title, t.description, t.isPublished, " +
            "COUNT(q.id), t.createdAt, t.timeLimitMinutes, " +
            "t.category.id, t.category.name) " +
            "FROM Test t " +
            "LEFT JOIN t.questions q " +
            "WHERE t.id IN :testIds " +
            "GROUP BY t.id, t.title, t.description, t.isPublished, t.createdAt, " +
            "t.timeLimitMinutes, t.category.id, t.category.name " +
            "ORDER BY t.createdAt DESC")
    Page<TestDTO> findTestDTOsByIds(@Param("testIds") List<Long> testIds, Pageable pageable);

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

    /**
     * 🔥 НОВОЕ: Поиск опубликованных тестов по названию (без пагинации)
     */
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

    /**
     * 🔥 НОВОЕ: Поиск опубликованных тестов по названию С ПАГИНАЦИЕЙ
     */
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

    /**
     * 🔥 НОВОЕ: Поиск тестов создателя по названию
     */
    @Query("SELECT new com.frist.assesspro.dto.TestDTO(" +
            "t.id, t.title, t.description, t.isPublished, " +
            "COUNT(q.id), t.createdAt, t.timeLimitMinutes, " +
            "t.category.id, t.category.name) " +
            "FROM Test t " +
            "LEFT JOIN t.questions q " +
            "WHERE t.createdBy = :creator " +
            "AND LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "GROUP BY t.id, t.title, t.description, t.isPublished, t.createdAt, " +
            "t.timeLimitMinutes, t.category.id, t.category.name " +
            "ORDER BY t.createdAt DESC")
    Page<TestDTO> searchTestsByCreator(@Param("creator") User creator,
                                       @Param("searchTerm") String searchTerm,
                                       Pageable pageable);
}