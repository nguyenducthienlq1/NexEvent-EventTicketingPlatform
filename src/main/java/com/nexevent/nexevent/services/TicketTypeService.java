package com.nexevent.nexevent.services;

import com.nexevent.nexevent.domains.dto.request.TicketTypeReqDTO;
import com.nexevent.nexevent.domains.dto.response.TicketTypeResDTO;
import com.nexevent.nexevent.domains.entities.Event;
import com.nexevent.nexevent.domains.entities.TicketType;
import com.nexevent.nexevent.domains.enums.StatusTicketType;
import com.nexevent.nexevent.repositories.EventRepository;
import com.nexevent.nexevent.repositories.TicketTypeRepository;
import com.nexevent.nexevent.utils.exception.IdInvalidException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public class TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final EventRepository eventRepository;

    public TicketTypeService(TicketTypeRepository ticketTypeRepository, EventRepository eventRepository) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.eventRepository = eventRepository;
    }

    public TicketTypeResDTO createTicketType(TicketTypeReqDTO dto){
        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new IdInvalidException("The opening time for sales cannot be after the closing time.!");
        }
        Event event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new IdInvalidException("Cannot find Event have ID: " + dto.getEventId()));
        TicketType newTicket = TicketType.builder()
                .event(event)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .totalQuantity(dto.getTotalQuantity())
                .soldQuantity(0)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(StatusTicketType.AVAILABLE)
                .build();

        ticketTypeRepository.save(newTicket);
        return convertToResDTO(newTicket);
    }

    public TicketTypeResDTO updateTicketType(Long id, TicketTypeReqDTO dto){
        TicketType ticket = ticketTypeRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Cannot find TicketType have ID: " + id));
        if (dto.getTotalQuantity() < ticket.getSoldQuantity()) {
            throw new IdInvalidException("The total number cannot be less than the number of tickets sold (" + ticket.getSoldQuantity() + ")");
        }

        ticket.setTitle(dto.getTitle());
        ticket.setPrice(dto.getPrice());
        ticket.setDescription(dto.getDescription());
        ticket.setTotalQuantity(dto.getTotalQuantity());
        ticket.setStartTime(dto.getStartTime());
        ticket.setEndTime(dto.getEndTime());

        if (ticket.getStatus() == StatusTicketType.SOLD_OUT && ticket.getTotalQuantity() > ticket.getSoldQuantity()) {
            ticket.setStatus(StatusTicketType.AVAILABLE);
        }

        ticketTypeRepository.save(ticket);
        return convertToResDTO(ticket);
    }

    public void deleteTicketType(Long id) {
        TicketType ticket = ticketTypeRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Cannot find TicketType have ID: " + id));

        ticket.setStatus(StatusTicketType.UNAVAILABE);
        ticketTypeRepository.save(ticket);
    }

    public Page<TicketTypeResDTO> getAllTicketsByEventForAdmin(Long eventId, Pageable pageable) {
        return ticketTypeRepository.findByEventId(eventId, pageable)
                .map(this::convertToResDTO);
    }


    public Page<TicketTypeResDTO> getActiveTicketsByEvent(Long eventId, Pageable pageable) {
        return ticketTypeRepository.findByEventIdAndStatus(eventId, StatusTicketType.AVAILABLE, pageable)
                .map(this::convertToResDTO);
    }

    private TicketTypeResDTO convertToResDTO(TicketType ticket) {
        return TicketTypeResDTO.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .price(ticket.getPrice())
                .totalQuantity(ticket.getTotalQuantity())
                .soldQuantity(ticket.getSoldQuantity())
                .remainQuantity(ticket.getTotalQuantity() - ticket.getSoldQuantity())
                .startTime(ticket.getStartTime())
                .endTime(ticket.getEndTime())
                .status(ticket.getStatus())
                .build();
    }
}