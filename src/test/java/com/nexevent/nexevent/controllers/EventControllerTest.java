package com.nexevent.nexevent.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexevent.nexevent.BaseIntegrationTest;
import com.nexevent.nexevent.domains.dto.request.EventReqDTO;
import com.nexevent.nexevent.domains.entities.Event;
import com.nexevent.nexevent.domains.entities.User;
import com.nexevent.nexevent.repositories.EventRepository;
import com.nexevent.nexevent.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EventControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;


    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private Long savedEventId;

    // --- BƯỚC CHUẨN BỊ MÔI TRƯỜNG ---
    @BeforeEach
    void cleanDatabase() {
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // Mồi 1 Admin User vào DB (Vì API tạo Event gọi SecurityUtil.getCurrentUserEmail())
        User adminUser = new User();
        adminUser.setEmail("admin@gmail.com");
        adminUser.setPassword("AdminPass123!");
        adminUser.setFullname("Admin System");
        adminUser.setPhone("0965620011");
        adminUser.setActive(true);

        userRepository.save(adminUser);

        // Mồi 1 Event hợp lệ vào DB để test các luồng Get/Update/Delete
        Event event = new Event();
        event.setTitle("Đêm Nhạc EDM Mùa Hè");
        event.setDescription("Quẩy tung chảo");
        event.setLocation("Phố đi bộ Nguyễn Huệ");
        event.setStartTime(LocalDateTime.now().plusDays(30).plusHours(18));
        event.setEndTime(LocalDateTime.now().plusDays(30).plusHours(23));
        event.setActive(true);
        event.setUser(adminUser);

        Event savedEvent = eventRepository.save(event);
        this.savedEventId = savedEvent.getId();
    }

    // ==========================================
    // 1. LUỒNG TEST TẠO SỰ KIỆN (RBAC & VALIDATION)
    // ==========================================

    @Test
    @DisplayName("Tạo sự kiện thành công: Admin tạo trả về 201 Created")
    // Giả lập user đã đăng nhập khớp với user mồi trong DB
    @WithMockUser(username = "admin@gmail.com", authorities = "ADMIN")
    void testCreateEvent_Success_Admin() throws Exception {
        // 1. Arrange: Chuẩn bị DTO chuẩn chỉ
        EventReqDTO reqDTO = new EventReqDTO();
        reqDTO.setTitle("Lễ Hội Bắn Pháo Hoa");
        reqDTO.setDescription("Mừng năm mới");
        reqDTO.setLocation("Sân vận động Mỹ Đình");
        reqDTO.setStartTime(LocalDateTime.now().plusDays(15).plusHours(19));
        reqDTO.setEndTime(LocalDateTime.now().plusDays(15).plusHours(22));

        // 2. Act & Assert
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isCreated());

        // 3. Assert Data: Kiểm tra dưới PostgreSQL ảo
        boolean isExist = eventRepository.findAll().stream()
                .anyMatch(e -> e.getTitle().equals("Lễ Hội Bắn Pháo Hoa"));
        assertTrue(isExist, "Lỗi: Sự kiện không được lưu vào Database!");
    }

    @Test
    @DisplayName("Tạo thất bại: Lỗi 401 do chưa đăng nhập (Không có Token)")
        // KHÔNG dùng @WithMockUser ở đây để giả lập khách vãng lai
    void testCreateEvent_Fail_Unauthorized() throws Exception {
        EventReqDTO reqDTO = new EventReqDTO();
        reqDTO.setTitle("Sự kiện ma");

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isUnauthorized()); // Bắt buộc văng 401
    }

    @Test
    @DisplayName("Tạo thất bại: Validation (Rác input - Bỏ trống, null)")
    @WithMockUser(username = "admin@gmail.com", authorities = "ADMIN")
    void testCreateEvent_Fail_Validation_EmptyFields() throws Exception {
        EventReqDTO reqDTO = new EventReqDTO();
        reqDTO.setTitle("   "); // Khoảng trắng
        reqDTO.setLocation(null); // Null

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Tạo sự kiện thất bại: User thường bị chặn (403 Forbidden)")
    @WithMockUser(username = "user@gmail.com", authorities = "USER") // Gài bẫy quyền USER
    void testCreateEvent_Fail_Forbidden() throws Exception {
        EventReqDTO reqDTO = new EventReqDTO();
        reqDTO.setTitle("Sự kiện trái phép");
        reqDTO.setLocation("Hà Nội");
        reqDTO.setStartTime(LocalDateTime.now().plusDays(5));
        reqDTO.setEndTime(LocalDateTime.now().plusDays(5).plusHours(2));

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tạo sự kiện thất bại: Validation @Future (Ngày ở quá khứ)")
    @WithMockUser(username = "admin@gmail.com", authorities = "ADMIN")
    void testCreateEvent_Fail_PastDate() throws Exception {
        EventReqDTO reqDTO = new EventReqDTO();
        reqDTO.setTitle("Sự kiện quá khứ");
        // Cố tình set ngày hôm qua để bắt lỗi Validation @Future
        reqDTO.setLocation("Hà Nội");
        reqDTO.setStartTime(LocalDateTime.now().minusDays(1));
        reqDTO.setEndTime(LocalDateTime.now().minusDays(1).plusHours(2));

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Tạo thất bại: Validation @ValidEventTime (Thời gian kết thúc nhỏ hơn bắt đầu)")
    @WithMockUser(username = "admin@gmail.com", authorities = "ADMIN")
    void testCreateEvent_Fail_Validation_TimeLogic() throws Exception {
        EventReqDTO reqDTO = new EventReqDTO();
        reqDTO.setTitle("Sự kiện test logic thời gian");
        reqDTO.setLocation("Sài Gòn");

        // Start time là 20h ngày mai
        reqDTO.setStartTime(LocalDateTime.now().plusDays(1).plusHours(20));

        // CỐ TÌNH GÀI BẪY: End time lại là 18h ngày mai (Tức là kết thúc TRƯỚC khi bắt đầu)
        reqDTO.setEndTime(LocalDateTime.now().plusDays(1).plusHours(18));

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message[0]").value("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu!"));
    }

    // ==========================================
    // 2. LUỒNG TEST LẤY DATA (GET) & XÓA (DELETE)
    // ==========================================

    @Test
    @DisplayName("Lấy danh sách sự kiện public thành công (Phân trang)")
    void testGetAllEvents_Success() throws Exception {
        mockMvc.perform(get("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                // DB đang có 1 sự kiện mồi sẵn ở @BeforeEach
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("Tìm kiếm sự kiện theo từ khóa thành công")
    void testSearchEvents_Success() throws Exception {
        mockMvc.perform(get("/api/v1/events/search")
                        .param("keyword", "EDM") // Cắt 1 chữ trong title mồi
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Đêm Nhạc EDM Mùa Hè"));
    }

    @Test
    @DisplayName("Xóa sự kiện thành công: Chuyển cờ active thành false (Soft Delete)")
    @WithMockUser(authorities = "ADMIN")
    void testDeleteEvent_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/events/" + savedEventId))
                .andExpect(status().isOk());

        // Kiểm tra dưới Database xem cờ active đã bị tắt chưa
        Event deletedEvent = eventRepository.findById(savedEventId).get();
        assertFalse(deletedEvent.isActive(), "Lỗi: Xóa mềm thất bại, cờ active vẫn bằng true!");
    }

    // ==========================================
    // 3. LUỒNG TEST CẬP NHẬT (UPDATE)
    // ==========================================

    @Test
    @DisplayName("Cập nhật sự kiện thành công (Admin)")
    @WithMockUser(username = "admin@gmail.com", authorities = "ADMIN")
    void testUpdateEvent_Success() throws Exception {
        EventReqDTO reqDTO = new EventReqDTO();
        reqDTO.setTitle("Đêm Nhạc EDM Mùa Hè (Đã đổi tên)");
        reqDTO.setLocation("Phố đi bộ Nguyễn Huệ");
        reqDTO.setStartTime(LocalDateTime.now().plusDays(40).plusHours(18));
        reqDTO.setEndTime(LocalDateTime.now().plusDays(40).plusHours(23));

        mockMvc.perform(patch("/api/v1/events/" + savedEventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Đêm Nhạc EDM Mùa Hè (Đã đổi tên)"));
    }

    @Test
    @DisplayName("Cập nhật thất bại: Trả về lỗi 400 nếu truyền ID không tồn tại")
    @WithMockUser(username = "admin@gmail.com", authorities = "ADMIN")
    void testUpdateEvent_Fail_NotFoundId() throws Exception {
        EventReqDTO reqDTO = new EventReqDTO();
        reqDTO.setTitle("Sự kiện ma");
        reqDTO.setLocation("Hà Nội");
        reqDTO.setStartTime(LocalDateTime.now().plusDays(10).plusHours(18));
        reqDTO.setEndTime(LocalDateTime.now().plusDays(10).plusHours(23));

        mockMvc.perform(patch("/api/v1/events/99999") // ID ma
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isBadRequest()); // Phụ thuộc vào IdInvalidException của bro
    }

    // ==========================================
    // 4. LUỒNG TEST LẤY DATA (GET / GET BY ID)
    // ==========================================

    @Test
    @DisplayName("Lấy chi tiết 1 sự kiện thành công")
    void testGetEventById_Success() throws Exception {
        mockMvc.perform(get("/api/v1/events/" + savedEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Đêm Nhạc EDM Mùa Hè"));
    }

    // ==========================================
    // 5. LUỒNG TEST XÓA (DELETE) VÀ BẢO MẬT DỮ LIỆU
    // ==========================================

    @Test
    @DisplayName("Xóa sự kiện thành công VÀ đảm bảo không bị rò rỉ ra API Public")
    @WithMockUser(authorities = "ADMIN")
    void testDeleteEvent_And_CheckLeak() throws Exception {
        // 1. Thực hiện xóa mềm
        mockMvc.perform(delete("/api/v1/events/" + savedEventId))
                .andExpect(status().isOk());

        // 2. Chốt QA: Đảm bảo cờ active = false dưới DB
        Event deletedEvent = eventRepository.findById(savedEventId).get();
        assertFalse(deletedEvent.isActive(), "Lỗi: Xóa mềm thất bại, cờ active vẫn bằng true!");

        // 3. CHỐT CỰC KỲ QUAN TRỌNG: Gọi lại API lấy danh sách Public
        // Phải đảm bảo sự kiện vừa xóa KHÔNG CÒN xuất hiện trong danh sách này nữa
        mockMvc.perform(get("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0)); // Phải trả về mảng rỗng
    }

}