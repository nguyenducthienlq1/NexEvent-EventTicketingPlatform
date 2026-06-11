package com.nexevent.nexevent.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexevent.nexevent.BaseIntegrationTest;
import com.nexevent.nexevent.domains.dto.request.TicketCheckInReqDTO;
import com.nexevent.nexevent.domains.entities.*;
import com.nexevent.nexevent.domains.enums.OrderStatus;
import com.nexevent.nexevent.domains.enums.StatusTicket;
import com.nexevent.nexevent.domains.enums.StatusTicketType;
import com.nexevent.nexevent.repositories.*;
import com.nexevent.nexevent.utils.TicketQrUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CheckinControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CheckinRepository checkinRepository;

    // Mock cái Utility giải mã QR để mình chủ động điều khiển kết quả giải mã ra ticketId trong môi trường Test
    @MockitoBean
    private TicketQrUtil ticketQrUtil;

    @Value("${nexevent.api-prefix}")
    private String apiPrefix;

    private User savedStaff;
    private Ticket validTicket;
    private Ticket usedTicket;
    private final String mockQrToken = "mock.jwt.qr.token.string";

    @BeforeEach
    void setupDatabase() {
        // Dọn dẹp sạch sẽ DB theo đúng thứ tự ràng buộc thực thể
        checkinRepository.deleteAll();
        ticketRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        ticketTypeRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Mồi Staff (Nhân viên soát vé có quyền CHECKER)
        User staff = new User();
        staff.setEmail("checker@gmail.com");
        staff.setPassword("CheckerPass123!");
        staff.setFullname("Staff Nguyen Van A");
        staff.setPhone("0988888888");
        staff.setActive(true);
        this.savedStaff = userRepository.save(staff);

        // 2. Mồi Khách hàng (Người mua vé)
        User customer = new User();
        customer.setEmail("customer@gmail.com");
        customer.setPassword("CustomerPass123!");
        customer.setFullname("Nguyen Duc Thien");
        customer.setPhone("0965620068");
        customer.setActive(true);
        userRepository.save(customer);

        // 3. Mồi Sự kiện
        Event event = new Event();
        event.setTitle("Anh Trai Say Hi Live Concert");
        event.setDescription("Đêm nhạc hoành tráng");
        event.setLocation("SVĐ Quân Khu 7");
        event.setStartTime(LocalDateTime.now().plusDays(2));
        event.setEndTime(LocalDateTime.now().plusDays(2).plusHours(4));
        event.setActive(true);
        event.setUser(staff); // Gán tạm người tạo
        eventRepository.save(event);

        // 4. Mồi Loại vé
        TicketType ticketType = new TicketType();
        ticketType.setTitle("Vé VIP Cận Sân Khấu");
        ticketType.setPrice(new BigDecimal("1500000"));
        ticketType.setTotalQuantity(500);
        ticketType.setSoldQuantity(10);
        ticketType.setStatus(StatusTicketType.AVAILABLE);
        ticketType.setEvent(event);
        ticketTypeRepository.save(ticketType);

        // 5. Mồi Đơn hàng & Chi tiết đơn hàng
        Order order = Order.builder()
                .user(customer)
                .orderCode("ORD-CHECKIN-TEST-01")
                .totalAmount(new BigDecimal("1500000"))
                .status(OrderStatus.PAID)
                .build();
        orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .ticketType(ticketType)
                .quantity(1)
                .unitPrice(ticketType.getPrice())
                .subtotal(ticketType.getPrice())
                .build();
        orderItemRepository.save(orderItem);

        // 6. Mồi 1 Vé hợp lệ (Trạng thái UNUSED - Chưa sử dụng)
        Ticket ticket1 = Ticket.builder()
                .id(UUID.randomUUID().toString())
                .orderItem(orderItem)
                .status(StatusTicket.UNUSED) // Sửa chuẩn Enum của dự án bro (UNUSED)
                .qrCode("http://nexevent.com/qrcode/ticket1")
                .isNew(true) // Đảm bảo Persistable hoạt động đúng khi lưu id tự sinh chuỗi
                .build();
        this.validTicket = ticketRepository.save(ticket1);

        // 7. Mồi 1 Vé đã sử dụng (Để test case Double-Checkin)
        Ticket ticket2 = Ticket.builder()
                .id(UUID.randomUUID().toString())
                .orderItem(orderItem)
                .status(StatusTicket.USED) // Vé đã quét vào cổng từ trước
                .qrCode("http://nexevent.com/qrcode/ticket2")
                .isNew(true)
                .build();
        this.usedTicket = ticketRepository.save(ticket2);
    }

    // =========================================================================
    // 1. LUỒNG HAPPY CASE (CHECK-IN THÀNH CÔNG)
    // =========================================================================

    @Test
    @DisplayName("Check-in thành công: Vé chưa dùng, Checker hợp lệ quét tại cổng G1")
    @WithMockUser(username = "checker@gmail.com", authorities = "CHECKER")
    void testCheckin_Success() throws Exception {
        // Gài bẫy Mock: Khi giải mã cái chuỗi mockQrToken, ép nó phải trả về đúng ID của cái vé hợp lệ dưới DB
        Mockito.when(ticketQrUtil.verifyAndGetTicketId(mockQrToken)).thenReturn(validTicket.getId());

        TicketCheckInReqDTO reqDTO = new TicketCheckInReqDTO();
        reqDTO.setQrToken(mockQrToken);
        reqDTO.setGate("G1");

        mockMvc.perform(post(apiPrefix + "/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isOk())
                // Xác minh thông tin Response JSON trả về đẹp đẽ theo cấu trúc DTO của bro
                .andExpect(jsonPath("$.data.ticketId").value(validTicket.getId()))
                .andExpect(jsonPath("$.data.customerName").value("Nguyen Duc Thien"))
                .andExpect(jsonPath("$.data.gate").value("G1"))
                .andExpect(jsonPath("$.data.staffName").value("Staff Nguyen Van A"));

        // Chốt hạ kiểm tra Database: Vé phải chuyển trạng thái sang USED
        Ticket updatedTicket = ticketRepository.findById(validTicket.getId()).get();
        assertEquals(StatusTicket.USED, updatedTicket.getStatus(), "Lỗi: Vé chưa bị đánh dấu thành USED sau khi check-in thành công!");

        // Chốt hạ kiểm tra bảng lịch sử Check-in: Phải phát sinh thêm 1 record
        long checkinRecordsCount = checkinRepository.findAll().stream()
                .filter(c -> c.getTicket().getId().equals(validTicket.getId()) && c.getGate().equals("G1"))
                .count();
        assertEquals(1, checkinRecordsCount, "Lỗi: Không tìm thấy bản ghi lịch sử quét vé lưu dưới DB!");
    }

    // =========================================================================
    // 2. LUỒNG EDGE CASES (CÁC TRƯỜNG HỢP LỖI NGHIỆP VỤ)
    // =========================================================================

    @Test
    @DisplayName("Check-in thất bại: Lỗi gian lận vé (Vé đã được sử dụng trước đó)")
    @WithMockUser(username = "checker@gmail.com", authorities = "CHECKER")
    void testCheckin_Fail_AlreadyUsed() throws Exception {
        // Ép Mock trả về ID của cái được gài sẵn trạng thái USED
        Mockito.when(ticketQrUtil.verifyAndGetTicketId(mockQrToken)).thenReturn(usedTicket.getId());

        TicketCheckInReqDTO reqDTO = new TicketCheckInReqDTO();
        reqDTO.setQrToken(mockQrToken);
        reqDTO.setGate("G2");

        mockMvc.perform(post(apiPrefix + "/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isBadRequest()) // Bắt buộc văng lỗi 400 từ GlobalException
                .andExpect(jsonPath("$.message").value("Ticket is already used"));
    }

    @Test
    @DisplayName("Check-in thất bại: Vé ma (ID vé giải mã ra không tồn tại trong hệ thống)")
    @WithMockUser(username = "checker@gmail.com", authorities = "CHECKER")
    void testCheckin_Fail_TicketNotFound() throws Exception {
        // Ép Mock trả về một chuỗi ID ngẫu nhiên không có thật trong DB ảo
        Mockito.when(ticketQrUtil.verifyAndGetTicketId(mockQrToken)).thenReturn("FAKE-ID-12345");

        TicketCheckInReqDTO reqDTO = new TicketCheckInReqDTO();
        reqDTO.setQrToken(mockQrToken);
        reqDTO.setGate("G1");

        mockMvc.perform(post(apiPrefix + "/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ticket does not exist"));
    }

    @Test
    @DisplayName("Check-in thất bại: Sai quyền hạn (Khách hàng thông thường cố tình gọi API checkin)")
    @WithMockUser(username = "customer@gmail.com", authorities = "USER") // Gài bẫy quyền USER thay vì CHECKER
    void testCheckin_Fail_Forbidden() throws Exception {
        TicketCheckInReqDTO reqDTO = new TicketCheckInReqDTO();
        reqDTO.setQrToken(mockQrToken);
        reqDTO.setGate("G1");

        mockMvc.perform(post(apiPrefix + "/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isForbidden()); // Bắt buộc chặn đứng bằng lỗi 403 Forbidden
    }

    @Test
    @DisplayName("Check-in thất bại: Validation trống dữ liệu đầu vào (Thiếu Gate / QR Token)")
    @WithMockUser(username = "checker@gmail.com", authorities = "CHECKER")
    void testCheckin_Fail_Validation_EmptyFields() throws Exception {
        TicketCheckInReqDTO reqDTO = new TicketCheckInReqDTO();
        reqDTO.setQrToken(null); // Cố tình bỏ trống trường bắt buộc @NotNull
        reqDTO.setGate("  ");

        mockMvc.perform(post(apiPrefix + "/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Check-in thất bại: Sự kiện đã kết thúc (Quá giờ soát vé)")
    @WithMockUser(username = "checker@gmail.com", authorities = "CHECKER")
    void testCheckin_Fail_EventEnded() throws Exception {
        // 1. Gài bẫy: Lấy sự kiện hiện tại ra và ép thời gian kết thúc về 1 tiếng trước ở quá khứ
        Event currentEvent = validTicket.getOrderItem().getTicketType().getEvent();
        currentEvent.setEndTime(LocalDateTime.now().minusHours(1));
        eventRepository.saveAndFlush(currentEvent);

        // 2. Mock giải mã QR trả về ID của cái vé nằm trong sự kiện quá hạn này
        Mockito.when(ticketQrUtil.verifyAndGetTicketId(mockQrToken)).thenReturn(validTicket.getId());

        TicketCheckInReqDTO reqDTO = new TicketCheckInReqDTO();
        reqDTO.setQrToken(mockQrToken);
        reqDTO.setGate("G1");

        // 3. Thực hiện bắn request và kỳ vọng lỗi 400 Bad Request
        mockMvc.perform(post(apiPrefix + "/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This event has already ended. Check-in closed!"));
    }
}