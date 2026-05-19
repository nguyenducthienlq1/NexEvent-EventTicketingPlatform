package com.nexevent.nexevent.repositories;

import com.nexevent.nexevent.domains.entities.Ticket;
import com.nexevent.nexevent.domains.enums.StatusTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {

    @Query(value = "SELECT t FROM Ticket t " +
            "JOIN FETCH t.orderItem oi " +
            "JOIN FETCH oi.ticketType tt " +
            "JOIN FETCH tt.event e " +
            "JOIN oi.order o " +
            "WHERE o.user.email = :email " +
            "AND (:status IS NULL OR t.status = :status)",
            countQuery = "SELECT COUNT(t) FROM Ticket t JOIN t.orderItem oi JOIN oi.order o WHERE o.user.email = :email AND (:status IS NULL OR t.status = :status)")
    Page<Ticket> findMyTickets(@Param("email") String email,
                               @Param("status") StatusTicket status,
                               Pageable pageable);

    @Query("SELECT SUM(oi.unitPrice) FROM Ticket t " +
            "JOIN t.orderItem oi JOIN oi.ticketType tt " +
            "WHERE tt.event.id = :eventId")
    Double getTotalRevenueByEvent(@Param("eventId") Long eventId);

    // 2. Đếm số vé đã bán và số vé đã Check-in (USED)
    @Query("SELECT COUNT(t.id), " +
            "SUM(CASE WHEN t.status = 'USED' THEN 1 ELSE 0 END) " +
            "FROM Ticket t JOIN t.orderItem oi JOIN oi.ticketType tt " +
            "WHERE tt.event.id = :eventId")
    Object[] getCheckinStatsByEvent(@Param("eventId") Long eventId);
}
