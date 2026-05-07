package com.nexevent.nexevent.repositories;

import com.nexevent.nexevent.domains.entities.TicketType;
import com.nexevent.nexevent.domains.enums.StatusTicket;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {
    Page<TicketType> findByEventIdAndStatus(Long eventId, StatusTicket status, Pageable pageable);

    Page<TicketType> findByEventId(Long eventId, Pageable pageable);

    @Query("SELECT t FROM TicketType t JOIN FETCH t.event WHERE t.id IN :ids")
    List<TicketType> findTicketsWithEventByIds(@Param("ids") Set<Long> ids);
}
