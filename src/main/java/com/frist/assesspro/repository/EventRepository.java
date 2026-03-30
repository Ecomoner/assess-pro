package com.frist.assesspro.repository;

import com.frist.assesspro.dto.EventDTO;
import com.frist.assesspro.entity.Event;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT new com.frist.assesspro.dto.EventDTO(e.id,e.name, e.description) " +
            "FROM Event e")
    Page<EventDTO> findAllEventDTOs(Pageable pageable);

    @Query("SELECT COUNT(e) FROM Event e")
    long getTotalEvent();

    boolean existsByName(String name);

    List<Event> findTop5ByOrderByIdDesc();
}
