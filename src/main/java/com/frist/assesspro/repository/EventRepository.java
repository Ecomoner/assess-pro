package com.frist.assesspro.repository;

import com.frist.assesspro.dto.EventDTO;
import com.frist.assesspro.entity.Event;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;


@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e WHERE e.eventDate BETWEEN :start AND :end ORDER BY e.eventDate")
    List<Event> findByEventDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<Event> findAllByOrderByEventDateDesc();
}
