package com.nexevent.nexevent.repositories;

import com.nexevent.nexevent.domains.entities.TicketType;
import com.nexevent.nexevent.domains.enums.StatusTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {
    Page<TicketType> findByEventIdAndStatus(Long eventId, StatusTicket status, Pageable pageable);

    Page<TicketType> findByEventId(Long eventId, Pageable pageable);
}
