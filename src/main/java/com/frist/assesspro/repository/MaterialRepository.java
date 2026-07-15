package com.frist.assesspro.repository;


import com.frist.assesspro.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface MaterialRepository  extends JpaRepository<Material, Long> {

    @Query("SELECT COALESCE(MAX(m.orderIndex), 0) FROM Material m WHERE m.section.id = :sectionId")
    Optional<Integer> findMaxOrderIndexBySectionId(@Param("sectionId") Long sectionId);
}
