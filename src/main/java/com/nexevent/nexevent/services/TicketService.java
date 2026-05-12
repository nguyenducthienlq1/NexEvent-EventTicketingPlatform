package com.nexevent.nexevent.services;

import com.nexevent.nexevent.domains.entities.OrderItem;
import com.nexevent.nexevent.domains.entities.Ticket;
import com.nexevent.nexevent.domains.enums.StatusTicket;
import com.nexevent.nexevent.repositories.OrderItemRepository;
import com.nexevent.nexevent.repositories.OrderRepository;
import com.nexevent.nexevent.repositories.TicketRepository;
import com.nexevent.nexevent.utils.TicketQrUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
}
