package com.nexevent.nexevent.services;

import com.nexevent.nexevent.domains.dto.request.EventReqDTO;
import com.nexevent.nexevent.domains.dto.response.EventResDTO;
import com.nexevent.nexevent.domains.entities.Event;
import com.nexevent.nexevent.domains.entities.User;
import com.nexevent.nexevent.repositories.EventRepository;
import com.nexevent.nexevent.repositories.UserRepository;
import com.nexevent.nexevent.utils.exception.IdInvalidException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventService(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }
    public EventResDTO createEvent(EventReqDTO dto, String adminEmail) throws IdInvalidException {
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
    public EventResDTO updateEvent(Long eventId, EventReqDTO dto) throws IdInvalidException {
        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new IdInvalidException("Thời gian bắt đầu không thể sau thời gian kết thúc!");
        }
        Event currentEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy sự kiện với ID: " + eventId));

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

    public void deleteEvent(Long eventId) throws IdInvalidException {
        Event currentEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy sự kiện với ID: " + eventId));

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

    public EventResDTO getEventById(Long eventId) throws IdInvalidException {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy sự kiện với ID: " + eventId));

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
}