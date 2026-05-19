package com.nexevent.nexevent.services;

import com.nexevent.nexevent.domains.dto.request.EventReqDTO;
import com.nexevent.nexevent.domains.dto.response.EventResDTO;
import com.nexevent.nexevent.domains.dto.response.ResEventStatsDTO;
import com.nexevent.nexevent.domains.entities.Event;
import com.nexevent.nexevent.domains.entities.TicketType;
import com.nexevent.nexevent.domains.entities.User;
import com.nexevent.nexevent.repositories.EventRepository;
import com.nexevent.nexevent.repositories.TicketRepository;
import com.nexevent.nexevent.repositories.TicketTypeRepository;
import com.nexevent.nexevent.repositories.UserRepository;
import com.nexevent.nexevent.utils.exception.IdInvalidException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final TicketRepository ticketRepository;
    private final TicketTypeRepository ticketTypeRepository;

    public EventService(EventRepository eventRepository,
                        UserRepository userRepository,
                        CloudinaryService cloudinaryService,
                        TicketRepository ticketRepository,
                        TicketTypeRepository ticketTypeRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.ticketRepository = ticketRepository;
        this.ticketTypeRepository = ticketTypeRepository;
    }
    @Transactional
    public EventResDTO createEvent(EventReqDTO dto, String adminEmail) {
        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new IdInvalidException("Thời gian bắt đầu không thể sau thời gian kết thúc!");
        }
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy thông tin Admin"));
        Event newEvent = Event.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .date(dto.getDate())
                .cover(dto.getCover())
                .location(dto.getLocation())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .active(true)
                .user(admin)
                .build();

        eventRepository.save(newEvent);
        return convertToResDTO(newEvent);
    }
    @Transactional
    public EventResDTO updateEvent(Long eventId, EventReqDTO dto) {
        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new IdInvalidException("The start time can't be after the end time!");
        }

        Event currentEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new IdInvalidException("Can't find the event with ID: " + eventId));

        if (currentEvent.getCover() != null && !currentEvent.getCover().equals(dto.getCover())) {
            cloudinaryService.deleteImageByUrl(currentEvent.getCover());
        }

        currentEvent.setTitle(dto.getTitle());
        currentEvent.setDescription(dto.getDescription());
        currentEvent.setDate(dto.getDate());
        currentEvent.setCover(dto.getCover());
        currentEvent.setLocation(dto.getLocation());
        currentEvent.setStartTime(dto.getStartTime());
        currentEvent.setEndTime(dto.getEndTime());

        eventRepository.save(currentEvent);
        return convertToResDTO(currentEvent);
    }
    public void deleteEvent(Long eventId){
        Event currentEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new IdInvalidException("Can't find the event with ID: " + eventId));

        currentEvent.setActive(false);
        eventRepository.save(currentEvent);
    }

    public Page<EventResDTO> getAllActiveEvents(Pageable pageable) {
        return eventRepository.findAllByActiveTrue(pageable)
                .map(this::convertToResDTO);
    }

    public Page<EventResDTO> searchActiveEvents(String keyword, Pageable pageable) {
        return eventRepository.findByTitleContainingIgnoreCaseAndActiveTrue(keyword, pageable)
                .map(this::convertToResDTO);
    }

    public EventResDTO getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IdInvalidException("Can't find the event with ID: " + eventId));

        return convertToResDTO(event);
    }

    private EventResDTO convertToResDTO(Event event) {
        return EventResDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .date(event.getDate())
                .cover(event.getCover())
                .location(event.getLocation())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .active(event.isActive())
                .organizerName(event.getUser().getFullname())
                .build();
    }
    public ResEventStatsDTO infoEvent(Long eventId) {
        Double revenue = ticketRepository.getTotalRevenueByEvent(eventId);
        List<TicketType> types = ticketTypeRepository.findByEventId(eventId);

        int totalIssued = 0;
        int totalSold = 0;
        List<ResEventStatsDTO.TicketTypeStat> typeStats = new ArrayList<>();

        for (TicketType tt : types) {
            int issued = tt.getTotalQuantity() != null ? tt.getTotalQuantity() : 0;
            int sold = tt.getSoldQuantity() != null ? tt.getSoldQuantity() : 0;

            totalIssued += issued;
            totalSold += sold;
            typeStats.add(ResEventStatsDTO.TicketTypeStat.builder()
                    .ticketTypeName(tt.getTitle())
                    .issued(issued)
                    .sold(sold)
                    .remaining(issued - sold)
                    .build());
        }
        return ResEventStatsDTO.builder()
                .totalRevenue(revenue)
                .totalIssued(totalIssued)
                .totalSold(totalSold)
                .totalRemaining(totalIssued - totalSold)
                .ticketTypes(typeStats)
                .build();
    }
}