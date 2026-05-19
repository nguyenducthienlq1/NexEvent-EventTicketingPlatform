package com.nexevent.nexevent.services;

import com.nexevent.nexevent.domains.dto.response.ResCheckinStatsDTO;
import com.nexevent.nexevent.domains.dto.response.ResTicketDTO;
import com.nexevent.nexevent.domains.entities.OrderItem;
import com.nexevent.nexevent.domains.entities.Ticket;
import com.nexevent.nexevent.domains.enums.StatusTicket;
import com.nexevent.nexevent.repositories.OrderItemRepository;
import com.nexevent.nexevent.repositories.TicketRepository;
import com.nexevent.nexevent.utils.SecurityUtil;
import com.nexevent.nexevent.utils.TicketQrUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final OrderItemRepository orderItemRepository;
    private final TicketQrUtil ticketQrUtil;
    public TicketService(TicketRepository ticketRepository,
                         OrderItemRepository orderItemRepository,
                         TicketQrUtil ticketQrUtil) {
        this.ticketRepository = ticketRepository;
        this.orderItemRepository = orderItemRepository;
        this.ticketQrUtil = ticketQrUtil;
    }
    @Transactional
    public List<Ticket> createAllTicketsOfOrder(Long idOrder){
        List<OrderItem> orderItemList = orderItemRepository.findByOrderId(idOrder);
        List<Ticket> ticketList = new ArrayList<Ticket>();
        for (OrderItem orderItem : orderItemList) {
            int quantity = orderItem.getQuantity();

            for (int i = 0; i < quantity; i++) {
                Ticket ticket = new Ticket();
                ticket.setId(java.util.UUID.randomUUID().toString());
                ticket.setIssuedAt(LocalDateTime.now());
                ticket.setStatus(StatusTicket.UNUSED);
                ticket.setOrderItem(orderItem);

                ticket.setQrCode(ticketQrUtil.generateTicketQr(ticket));

                ticketList.add(ticket);
            }
        }
        return ticketRepository.saveAll(ticketList);
    }
    public Optional<Ticket> findById(String id){
        return ticketRepository.findById(id);
    }
    public void saveTicket(Ticket ticket){
        ticketRepository.save(ticket);
    }
    public Page<ResTicketDTO> getMyTickets(StatusTicket status, Pageable pageable) {
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();

        Page<Ticket> ticketPage = ticketRepository.findMyTickets(currentUserEmail, status, pageable);

        return ticketPage.map(ticket -> ResTicketDTO.builder()
                .ticketId(ticket.getId())
                .eventName(ticket.getOrderItem().getTicketType().getEvent().getTitle())
                .eventStartTime(ticket.getOrderItem().getTicketType().getEvent().getStartTime())
                .eventLocation(ticket.getOrderItem().getTicketType().getEvent().getLocation())
                .ticketTypeName(ticket.getOrderItem().getTicketType().getTitle())
                .status(ticket.getStatus().name())
                .qrCode(ticket.getQrCode())
                .build());
    }
    public ResCheckinStatsDTO getCheckinStatsWidget(Long eventId) {
        Object[] rawData = (Object[]) ticketRepository.getCheckinStatsByEvent(eventId)[0];
        Long totalSold = rawData[0] != null ? ((Number) rawData[0]).longValue() : 0L;
        Long totalCheckedIn = rawData[1] != null ? ((Number) rawData[1]).longValue() : 0L;

        double rate = 0.0;
        if (totalSold > 0) {
            rate = ((double) totalCheckedIn / totalSold) * 100;
            rate = Math.round(rate * 100.0) / 100.0;
        }

        return ResCheckinStatsDTO.builder()
                .totalSold(totalSold)
                .totalCheckedIn(totalCheckedIn)
                .checkinRate(rate)
                .build();
    }
}
