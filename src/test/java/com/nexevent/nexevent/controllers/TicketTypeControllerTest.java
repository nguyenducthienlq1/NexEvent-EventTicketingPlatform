package com.nexevent.nexevent.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexevent.nexevent.BaseIntegrationTest;
import com.nexevent.nexevent.domains.dto.request.TicketTypeReqDTO;
import com.nexevent.nexevent.domains.entities.Event;
import com.nexevent.nexevent.domains.entities.TicketType;
import com.nexevent.nexevent.domains.entities.User;
import com.nexevent.nexevent.domains.enums.StatusTicketType;
import com.nexevent.nexevent.repositories.EventRepository;
import com.nexevent.nexevent.repositories.TicketTypeRepository;
import com.nexevent.nexevent.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TicketTypeControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    private Long savedEventId;
    private Long savedTicketTypeId;

    @BeforeEach
    void cleanDatabase() {
        ticketTypeRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Mồi Admin
        User adminUser = new User();
        adminUser.setEmail("admin@gmail.com");
        adminUser.setPassword("AdminPass123!");
        adminUser.setFullname("Admin System");
        adminUser.setPhone("0912345678");
        adminUser.setActive(true);
        userRepository.save(adminUser);

        // 2. Mồi Event làm gốc để tạo vé
        Event event = new Event();
        event.setTitle("Đại Nhạc Hội Rap Việt");
        event.setLocation("Nhà thi đấu Quân Khu 7");
        event.setStartTime(LocalDateTime.now().plusDays(30).plusHours(19));
        event.setEndTime(LocalDateTime.now().plusDays(30).plusHours(23));
        event.setActive(true);
        event.setUser(adminUser);
        Event savedEvent = eventRepository.save(event);
        this.savedEventId = savedEvent.getId();

        TicketType ticketType = new TicketType();
        ticketType.setTitle("Vé GA (Đứng)");
        ticketType.setPrice(new BigDecimal("700000"));
        ticketType.setTotalQuantity(1000);
        ticketType.setSoldQuantity(0);
        ticketType.setStartTime(LocalDateTime.now().minusDays(1));
        ticketType.setEndTime(LocalDateTime.now().plusDays(10));
        ticketType.setStatus(StatusTicketType.AVAILABLE);
        ticketType.setEvent(savedEvent);

        TicketType savedType = ticketTypeRepository.save(ticketType);
        this.savedTicketTypeId = savedType.getId();
    }

    // ==========================================
    // 1. LUỒNG TEST TẠO VÉ (CREATE & VALIDATION)
    // ==========================================

    @Test
    @DisplayName("Tạo loại vé mới thành công (Admin)")
    @WithMockUser(username = "admin@gmail.com", authorities = "ADMIN")
    void testCreateTicketType_Success() throws Exception {
        TicketTypeReqDTO reqDTO = new TicketTypeReqDTO();
        reqDTO.setEventId(savedEventId); // Trỏ đúng vào Event mồi
        reqDTO.setTitle("Vé VVIP (Ngồi sát sân khấu)");
        reqDTO.setDescription("Tặng áo thun + Lightstick");
        reqDTO.setPrice(new BigDecimal("2500000"));
        reqDTO.setTotalQuantity(50);
        reqDTO.setStartTime(LocalDateTime.now().plusDays(1)); // @Future
        reqDTO.setEndTime(LocalDateTime.now().plusDays(15));

        mockMvc.perform(post("/api/v1/ticket-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Tạo vé thất bại: Validation @Min (Số lượng vé = 0)")
    @WithMockUser(username = "admin@gmail.com", authorities = "ADMIN")
    void testCreateTicketType_Fail_ZeroQuantity() throws Exception {
        TicketTypeReqDTO reqDTO = new TicketTypeReqDTO();
        reqDTO.setEventId(savedEventId);
        reqDTO.setTitle("Vé Lỗi");
        reqDTO.setPrice(new BigDecimal("100000"));
        reqDTO.setTotalQuantity(0); // Cố tình set 0 để vi phạm @Min(value=1)
        reqDTO.setStartTime(LocalDateTime.now().plusDays(1));
        reqDTO.setEndTime(LocalDateTime.now().plusDays(5));

        mockMvc.perform(post("/api/v1/ticket-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Tạo vé thất bại: Validation @ValidTimeRange (EndTime < StartTime)")
    @WithMockUser(username = "admin@gmail.com", authorities = "ADMIN")
    void testCreateTicketType_Fail_Validation_TimeLogic() throws Exception {
        TicketTypeReqDTO reqDTO = new TicketTypeReqDTO();
        reqDTO.setEventId(savedEventId);
        reqDTO.setTitle("Vé Lỗi Thời Gian");
        reqDTO.setPrice(new BigDecimal("100000"));
        reqDTO.setTotalQuantity(100);

        // Start time là 20h ngày mai
        reqDTO.setStartTime(LocalDateTime.now().plusDays(1).plusHours(20));
        // CỐ TÌNH GÀI BẪY: End time lại là 18h ngày mai
        reqDTO.setEndTime(LocalDateTime.now().plusDays(1).plusHours(18));

        mockMvc.perform(post("/api/v1/ticket-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isBadRequest())
                // Custom annotation của chúng ta hoạt động tốt!
                .andExpect(jsonPath("$.message[0]").value("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu!"));
    }

    // ==========================================
    // 2. LUỒNG TEST SỬA VÉ (UPDATE)
    // ==========================================

    @Test
    @DisplayName("Cập nhật loại vé thành công (Admin)")
    @WithMockUser(username = "admin@gmail.com", authorities = "ADMIN")
    void testUpdateTicketType_Success() throws Exception {
        TicketTypeReqDTO reqDTO = new TicketTypeReqDTO();
        reqDTO.setEventId(savedEventId);
        reqDTO.setTitle("Vé GA (Đã Update Giá)");
        reqDTO.setPrice(new BigDecimal("850000")); // Tăng giá lên
        reqDTO.setTotalQuantity(1000);
        reqDTO.setStartTime(LocalDateTime.now().plusDays(1));
        reqDTO.setEndTime(LocalDateTime.now().plusDays(10));

        mockMvc.perform(put("/api/v1/ticket-types/" + savedTicketTypeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 3. LUỒNG TEST LẤY DATA (GET / RBAC)
    // ==========================================

    @Test
    @DisplayName("Public API: Lấy danh sách vé ACTIVE của sự kiện thành công")
    void testGetTicketsByEvent_Public_Success() throws Exception {
        mockMvc.perform(get("/api/v1/events/" + savedEventId + "/ticket-types")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].title").value("Vé GA (Đứng)"));
    }

    @Test
    @DisplayName("Admin API: Lấy TOÀN BỘ vé thành công")
    @WithMockUser(authorities = "ADMIN")
    void testGetAllTicketsForAdmin_Success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events/" + savedEventId + "/ticket-types")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin API: Chặn User thường (Lỗi 403 Forbidden)")
    @WithMockUser(authorities = "USER")
    void testGetAllTicketsForAdmin_Fail_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events/" + savedEventId + "/ticket-types")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 4. LUỒNG TEST XÓA (DELETE)
    // ==========================================

    @Test
    @DisplayName("Hủy bán vé thành công (Soft Delete -> INACTIVE)")
    @WithMockUser(authorities = "ADMIN")
    void testDeleteTicketType_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/ticket-types/" + savedTicketTypeId))
                .andExpect(status().isOk());

        // Móc data từ DB lên kiểm tra cờ status
        TicketType deletedType = ticketTypeRepository.findById(savedTicketTypeId).get();
        assertEquals(StatusTicketType.UNAVAILABE, deletedType.getStatus(), "Lỗi: Trạng thái vé chưa chuyển sang INACTIVE!");

        mockMvc.perform(get("/api/v1/events/" + savedEventId + "/ticket-types")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }
}