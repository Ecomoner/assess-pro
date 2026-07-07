package com.frist.assesspro.repository;

import com.frist.assesspro.dto.material.SectionDTO;
import com.frist.assesspro.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findAllByActiveTrueOrderByOrderIndex();

    @Query("SELECT COALESCE(MAX(s.orderIndex), 0) FROM Section s")
    int getMaxOrderIndex();

    List<Section> findAllByOrderByOrderIndex();
}
