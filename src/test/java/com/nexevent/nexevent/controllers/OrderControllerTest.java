package com.nexevent.nexevent.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexevent.nexevent.BaseIntegrationTest;
import com.nexevent.nexevent.domains.dto.request.OrderItemReqDTO;
import com.nexevent.nexevent.domains.dto.request.OrderReqDTO;
import com.nexevent.nexevent.domains.entities.Event;
import com.nexevent.nexevent.domains.entities.TicketType;
import com.nexevent.nexevent.domains.entities.User;
import com.nexevent.nexevent.domains.enums.StatusTicketType;
import com.nexevent.nexevent.repositories.EventRepository;
import com.nexevent.nexevent.repositories.OrderItemRepository;
import com.nexevent.nexevent.repositories.OrderRepository;
import com.nexevent.nexevent.repositories.TicketTypeRepository;
import com.nexevent.nexevent.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderControllerTest extends BaseIntegrationTest {

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
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User savedBuyer;
    private Event savedEvent;
    private TicketType normalTicketType;

    @BeforeEach
    void cleanAndSetupDatabase() {
        // Dọn sạch data theo đúng thứ tự ràng buộc khóa ngoại (Foreign Key)
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        ticketTypeRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Mồi User người mua
        User buyer = new User();
        buyer.setEmail("buyer@gmail.com");
        buyer.setPassword("BuyerPass123!");
        buyer.setFullname("Nguyen Duc Thien");
        buyer.setPhone("0965620068");
        buyer.setActive(true);
        this.savedBuyer = userRepository.save(buyer);

        // 2. Mồi Event hoạt động bình thường
        Event event = new Event();
        event.setTitle("Anh Trai Say Hi Live Concert");
        event.setDescription("Đêm nhạc bùng nổ");
        event.setLocation("SVĐ Quân Khu 7");
        event.setStartTime(LocalDateTime.now().plusDays(10).plusHours(19));
        event.setEndTime(LocalDateTime.now().plusDays(10).plusHours(23));
        event.setActive(true);
        event.setUser(savedBuyer); // Đóng vai trò admin tạo tạm
        this.savedEvent = eventRepository.save(event);

        // 3. Mồi TicketType bình thường (Còn hàng, trong thời gian bán)
        TicketType ticketType = new TicketType();
        ticketType.setTitle("Vé GA");
        ticketType.setPrice(new BigDecimal("500000"));
        ticketType.setTotalQuantity(100);
        ticketType.setSoldQuantity(0);
        ticketType.setStartTime(LocalDateTime.now().minusDays(1)); // Đang mở bán
        ticketType.setEndTime(LocalDateTime.now().plusDays(9));
        ticketType.setStatus(StatusTicketType.AVAILABLE);
        ticketType.setEvent(savedEvent);
        this.normalTicketType = ticketTypeRepository.save(ticketType);
    }

    // =========================================================================
    // PHẦN 1: TEST CÁC EDGE CASES (CẠNH RÌA - LỖI LOGIC DO DOANH NGHIỆP ĐẶT RA)
    // =========================================================================

    @Test
    @DisplayName("Đặt đơn thành công: Luồng lý tưởng (Happy Case)")
    @WithMockUser(username = "buyer@gmail.com", authorities = "USER")
    void testCreateOrder_Success() throws Exception {
        OrderItemReqDTO itemDTO = new OrderItemReqDTO();
        itemDTO.setTicketTypeId(normalTicketType.getId());
        itemDTO.setQuantity(2); // Mua 2 vé

        OrderReqDTO reqDTO = new OrderReqDTO();
        reqDTO.setItems(Collections.singletonList(itemDTO));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Đặt đơn thất bại: Vé đã hết hàng (Sold Out)")
    @WithMockUser(username = "buyer@gmail.com", authorities = "USER")
    void testCreateOrder_Fail_SoldOut() throws Exception {
        // Gài bẫy: Cập nhật số lượng vé đã bán bằng đúng số lượng phát hành
        normalTicketType.setSoldQuantity(100);
        ticketTypeRepository.save(normalTicketType);

        OrderItemReqDTO itemDTO = new OrderItemReqDTO();
        itemDTO.setTicketTypeId(normalTicketType.getId());
        itemDTO.setQuantity(1); // Cố tình mua thêm 1 vé

        OrderReqDTO reqDTO = new OrderReqDTO();
        reqDTO.setItems(Collections.singletonList(itemDTO));

        // Mong đợi lỗi đầu ra là 400 Bad Request từ GlobalException
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Đặt đơn thất bại: Sự kiện đã bị Admin xóa mềm (active = false)")
    @WithMockUser(username = "buyer@gmail.com", authorities = "USER")
    void testCreateOrder_Fail_EventDeleted() throws Exception {
        // Gài bẫy: Ẩn sự kiện
        savedEvent.setActive(false);
        eventRepository.save(savedEvent);

        OrderItemReqDTO itemDTO = new OrderItemReqDTO();
        itemDTO.setTicketTypeId(normalTicketType.getId());
        itemDTO.setQuantity(1);

        OrderReqDTO reqDTO = new OrderReqDTO();
        reqDTO.setItems(Collections.singletonList(itemDTO));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Đặt đơn thất bại: Loại vé đã hết hạn bán (quá đóng bán)")
    @WithMockUser(username = "buyer@gmail.com", authorities = "USER")
    void testCreateOrder_Fail_TicketExpired() throws Exception {
        // Gài bẫy: Cho thời gian kết thúc bán vé nằm ở quá khứ
        normalTicketType.setEndTime(LocalDateTime.now().minusHours(1));
        ticketTypeRepository.save(normalTicketType);

        OrderItemReqDTO itemDTO = new OrderItemReqDTO();
        itemDTO.setTicketTypeId(normalTicketType.getId());
        itemDTO.setQuantity(1);

        OrderReqDTO reqDTO = new OrderReqDTO();
        reqDTO.setItems(Collections.singletonList(itemDTO));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // PHẦN 2: ĐỒNG THỜI (CONCURRENCY)
    // =========================================================================

    @Test
    @DisplayName("Race Condition: Chỉ còn đúng 1 vé, 4 máy của 4 người dùng cùng lúc đâm vào mua")
    void testConcurrent_OnlyOneTicketLeft() throws Exception {
        // Gài bẫy kịch tính: Kho chỉ còn lại đúng 1 vé duy nhất!
        normalTicketType.setTotalQuantity(100);
        normalTicketType.setSoldQuantity(99);
        ticketTypeRepository.saveAndFlush(normalTicketType);

        // Tạo sẵn 4 User khác nhau dưới DB
        String[] buyersEmails = {"user1@gmail.com", "user2@gmail.com", "user3@gmail.com", "user4@gmail.com"};
        for (String email : buyersEmails) {
            User u = new User();
            u.setEmail(email);
            u.setPassword("Pass123!");
            u.setFullname("Gamer " + email);
            u.setPhone("0965620000");
            u.setActive(true);
            userRepository.save(u);
        }
        userRepository.flush();

        int numberOfParallelRequests = 4;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfParallelRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfParallelRequests);

        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger failCounter = new AtomicInteger(0);

        OrderItemReqDTO itemDTO = new OrderItemReqDTO();
        itemDTO.setTicketTypeId(normalTicketType.getId());
        itemDTO.setQuantity(1);

        OrderReqDTO reqDTO = new OrderReqDTO();
        reqDTO.setItems(Collections.singletonList(itemDTO));
        String jsonPayload = objectMapper.writeValueAsString(reqDTO);

        for (int i = 0; i < numberOfParallelRequests; i++) {
            final String currentUserEmail = buyersEmails[i];
            executorService.execute(() -> {
                try {
                    startLatch.await(); // Chờ súng lệnh

                    MvcResult result = mockMvc.perform(post("/api/v1/orders")
                                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext(
                                            org.springframework.security.core.context.SecurityContextHolder.createEmptyContext()
                                    ))
                                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(currentUserEmail).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("USER")))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonPayload))
                            .andReturn();

                    int httpStatus = result.getResponse().getStatus();
                    String responseBody = result.getResponse().getContentAsString();

                    System.out.println(">>>  DEBUG - Luồng của " + currentUserEmail + " trả về Status: " + httpStatus + " | Body: " + responseBody);

                    if (httpStatus == 201) {
                        successCounter.incrementAndGet();
                    } else {
                        failCounter.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCounter.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // BẮN SÚNG LỆNH!
        doneLatch.await();

        Thread.sleep(300); // Chờ DB ảo ổn định kết nối hoàn toàn

        System.out.println(">>> KẾT QUẢ - Thành công: " + successCounter.get() + " | Thất bại: " + failCounter.get());

        // CHỐT HẠ BÀI TEST
        assertEquals(1, successCounter.get(), "LỖI CHÍ MẠNG: Hệ thống dính lỗi Overbooking hoặc chặn nhầm luồng!");
        assertEquals(3, failCounter.get(), "LỖI LOGIC: Các luồng sau không bị chặn đứng!");

        TicketType dbTicketCheck = ticketTypeRepository.findById(normalTicketType.getId()).get();
        assertEquals(100, dbTicketCheck.getSoldQuantity(), "Data Integrity Error: Số lượng bán ra dưới DB bị sai lệch!");
    }

    // =========================================================================
    // PHẦN 3: TEST REDIS DISTRIBUTED LOCK (CHỐNG SPAM CLICK)
    // =========================================================================

    @Test
    @DisplayName("Redis Lock: Một người dùng click mua nhanh 2 lần cùng lúc, hệ thống chỉ chấp nhận 1 đơn")
    void testConcurrent_SpamClickSameUser_BlockedByRedisLock() throws Exception {
        int numberOfParallelRequests = 2; // 2 request đồng thời từ CÙNG 1 người dùng
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfParallelRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfParallelRequests);

        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger failCounter = new AtomicInteger(0);
        AtomicInteger redisLockBlockCounter = new AtomicInteger(0);

        OrderItemReqDTO itemDTO = new OrderItemReqDTO();
        itemDTO.setTicketTypeId(normalTicketType.getId());
        itemDTO.setQuantity(1);

        OrderReqDTO reqDTO = new OrderReqDTO();
        reqDTO.setItems(Collections.singletonList(itemDTO));
        String jsonPayload = objectMapper.writeValueAsString(reqDTO);

        // Kích hoạt 2 luồng bắn phá đồng thời, dùng chung tài khoản buyer@gmail.com
        for (int i = 0; i < numberOfParallelRequests; i++) {
            executorService.execute(() -> {
                try {
                    startLatch.await(); // Đứng chờ súng lệnh

                    MvcResult result = mockMvc.perform(post("/api/v1/orders")
                                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext(
                                            org.springframework.security.core.context.SecurityContextHolder.createEmptyContext()
                                    ))
                                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("buyer@gmail.com").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("USER")))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonPayload))
                            .andReturn();

                    int httpStatus = result.getResponse().getStatus();
                    String responseBody = result.getResponse().getContentAsString();

                    if (httpStatus == 201) {
                        successCounter.incrementAndGet();
                    } else {
                        failCounter.incrementAndGet();
                        if (responseBody.contains("You're working too fast!")) {
                            redisLockBlockCounter.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failCounter.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // BẮN SÚNG LỆNH!
        doneLatch.await();

        // CHỐT HẠ KIỂM TRA CHẤT LƯỢNG LOCK
        // Do dùng chung 1 User, bắt buộc chỉ có 1 request được xử lý thành công, request kia bị block ngay lập tức
        assertEquals(1, successCounter.get(), "LỖI: Đáng lẽ chỉ được có duy nhất 1 đơn hàng tạo thành công!");
        assertEquals(1, failCounter.get(), "LỖI: Request spam thứ hai đã không bị chặn lại!");
        assertEquals(1, redisLockBlockCounter.get(), "LỖI: Request bị chặn không phải do Redis Distributed Lock trả về!");
    }
}