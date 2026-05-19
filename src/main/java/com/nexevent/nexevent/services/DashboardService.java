package com.nexevent.nexevent.services;

import com.nexevent.nexevent.domains.dto.response.ResCheckinStatsDTO;
import com.nexevent.nexevent.domains.dto.response.ResEventStatsDTO;
import com.nexevent.nexevent.domains.dto.response.ResLiveCheckinDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class DashboardService {

    private final Map<Long, List<SseEmitter>> eventEmitters = new ConcurrentHashMap<>();

    private final EventService eventService;
    private final TicketService ticketService;

    public DashboardService(EventService eventService, TicketService ticketService) {
        this.eventService = eventService;
        this.ticketService = ticketService;
    }

    public SseEmitter subscribe(Long eventId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // Tạo phòng cho eventId nếu chưa có, rồi thêm Admin vào phòng
        eventEmitters.putIfAbsent(eventId, new CopyOnWriteArrayList<>());
        eventEmitters.get(eventId).add(emitter);

        emitter.onCompletion(() -> eventEmitters.get(eventId).remove(emitter));
        emitter.onTimeout(() -> eventEmitters.get(eventId).remove(emitter));
        emitter.onError((e) -> eventEmitters.get(eventId).remove(emitter));

        log.info("Có một Admin vừa mở Dashboard Sự kiện {}. Tổng kết nối phòng này: {}", eventId, eventEmitters.get(eventId).size());

        // QUAN TRỌNG: Bắn ngay data tĩnh lần đầu để Frontend có số vẽ biểu đồ
        triggerInitialData(emitter, eventId);

        return emitter;
    }

    // Cái live-checkin bro truyền thêm eventId vào để biết bắn cho phòng nào nhé
    public void broadcastCheckinEvent(Long eventId, ResLiveCheckinDTO data) {
        List<SseEmitter> emitters = eventEmitters.get(eventId);
        if (emitters == null || emitters.isEmpty()) return;

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("live-checkin")
                        .data(data));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    public void broadcastWidgetUpdates(Long eventId) {
        List<SseEmitter> emitters = eventEmitters.get(eventId);
        if (emitters == null || emitters.isEmpty()) return;

        ResEventStatsDTO eventStatsData = eventService.infoEvent(eventId);
        ResCheckinStatsDTO checkinData = ticketService.getCheckinStatsWidget(eventId);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("event-stats-update").data(eventStatsData));
                emitter.send(SseEmitter.event().name("checkin-rate-update").data(checkinData));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    private void triggerInitialData(SseEmitter emitter, Long eventId) {
        try {
            emitter.send(SseEmitter.event().name("event-stats-update").data(eventService.infoEvent(eventId)));
            emitter.send(SseEmitter.event().name("checkin-rate-update").data(ticketService.getCheckinStatsWidget(eventId)));
        } catch (IOException e) {
            eventEmitters.get(eventId).remove(emitter);
        }
    }
}