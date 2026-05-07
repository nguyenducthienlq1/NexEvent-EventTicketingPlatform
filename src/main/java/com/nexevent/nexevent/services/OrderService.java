package com.nexevent.nexevent.services;

import com.nexevent.nexevent.domains.dto.request.OrderItemReqDTO;
import com.nexevent.nexevent.domains.dto.request.OrderReqDTO;
import com.nexevent.nexevent.domains.entities.Order;
import com.nexevent.nexevent.domains.entities.OrderItem;
import com.nexevent.nexevent.domains.entities.TicketType;
import com.nexevent.nexevent.domains.entities.User;
import com.nexevent.nexevent.domains.enums.OrderStatus;
import com.nexevent.nexevent.domains.enums.StatusTicket;
import com.nexevent.nexevent.repositories.OrderItemRepository;
import com.nexevent.nexevent.repositories.OrderRepository;
import com.nexevent.nexevent.repositories.TicketTypeRepository;
import com.nexevent.nexevent.repositories.UserRepository;
import com.nexevent.nexevent.utils.exception.IdInvalidException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final TicketTypeRepository ticketTypeRepository;
    private final UserRepository userRepository;
    public OrderService(OrderRepository orderRepository,
                        RedisTemplate<String, String> redisTemplate,
                        OrderItemRepository orderItemRepository,
                        TicketTypeRepository ticketTypeRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.redisTemplate = redisTemplate;
        this.orderItemRepository = orderItemRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.userRepository = userRepository;
    }
    private String generateOrderCode() {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String redisKey = "nexevent:order_seq:" + currentDate;
        Long sequenceNumber = redisTemplate.opsForValue().increment(redisKey);
        if (sequenceNumber != null && sequenceNumber == 1) {
            redisTemplate.expire(redisKey, 24, TimeUnit.HOURS);
        }
        return String.format("ORD-%s-%04d", currentDate, sequenceNumber);
    }

    @Transactional
    public Order createOrder(OrderReqDTO dto, String userEmail) throws IdInvalidException {
        // 1. Lấy thông tin User
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IdInvalidException("Người dùng không tồn tại"));

        // ==========================================
        // BƯỚC TỐI ƯU 1: CHỐNG HACKER GỬI TRÙNG ID VÉ
        // Gom nhóm: Nếu FE gửi [{id:1, qty:2}, {id:1, qty:3}], tự động gộp thành {id:1, qty:5}
        // ==========================================
        Map<Long, Integer> ticketQuantityMap = dto.getItems().stream()
                .collect(Collectors.groupingBy(
                        OrderItemReqDTO::getTicketTypeId,
                        Collectors.summingInt(OrderItemReqDTO::getQuantity)
                ));

        // ==========================================
        // BƯỚC TỐI ƯU 2: GIẢI QUYẾT N+1 QUERY
        // Lấy tất cả vé từ DB chỉ bằng 1 câu query (SELECT * FROM ticket_types WHERE id IN (...))
        // ==========================================
        List<TicketType> ticketTypes = ticketTypeRepository.findAllById(ticketQuantityMap.keySet());

        // Nếu số lượng vé lấy lên không bằng số lượng ID khách truyền -> Có vé giả mạo
        if (ticketTypes.size() != ticketQuantityMap.size()) {
            throw new IdInvalidException("Một số loại vé không tồn tại trong hệ thống!");
        }

        // Ép List sang Map để chốc nữa Get ra cho nhanh (Độ phức tạp O(1))
        Map<Long, TicketType> ticketTypeMap = ticketTypes.stream()
                .collect(Collectors.toMap(TicketType::getId, t -> t));

        // 3. Tạo vỏ Order (Cái này là tạo mới hoàn toàn nên BẮT BUỘC phải save 1 lần để lấy ID)
        Order newOrder = Order.builder()
                .user(user)
                .orderCode(generateOrderCode())
                .totalAmount(BigDecimal.ZERO)
                .status(OrderStatus.PENDING)
                .build();
        newOrder = orderRepository.save(newOrder);

        BigDecimal totalOrderAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // 4. Xử lý logic trên danh sách đã gom nhóm
        for (Map.Entry<Long, Integer> entry : ticketQuantityMap.entrySet()) {
            Long ticketId = entry.getKey();
            Integer quantityToBuy = entry.getValue();
            TicketType ticketType = ticketTypeMap.get(ticketId);

            // Kiểm tra trạng thái
            if (ticketType.getStatus() != StatusTicket.AVAILABLE) { // Nhớ sửa chính tả chữ AVAILABLE nhé
                throw new IdInvalidException("Vé '" + ticketType.getTitle() + "' hiện không mở bán.");
            }

            // Kiểm tra số lượng tồn
            int remainQty = ticketType.getTotalQuantity() - ticketType.getSoldQuantity();
            if (remainQty < quantityToBuy) {
                throw new IdInvalidException("Vé '" + ticketType.getTitle() + "' chỉ còn " + remainQty + " vé, không đủ số lượng!");
            }

            // Tính tiền
            BigDecimal itemSubtotal = ticketType.getPrice().multiply(BigDecimal.valueOf(quantityToBuy));
            totalOrderAmount = totalOrderAmount.add(itemSubtotal);

            // CẬP NHẬT DỮ LIỆU BỘ NHỚ (Không cần gọi .save() nhờ Dirty Checking)
            ticketType.setSoldQuantity(ticketType.getSoldQuantity() + quantityToBuy);
            if (ticketType.getSoldQuantity().equals(ticketType.getTotalQuantity())) {
                ticketType.setStatus(StatusTicket.SOLD_OUT);
            }

            // Tạo Order Item
            OrderItem orderItem = OrderItem.builder()
                    .order(newOrder)
                    .ticketType(ticketType)
                    .quantity(quantityToBuy)
                    .unitPrice(ticketType.getPrice())
                    .subtotal(itemSubtotal)
                    .build();
            orderItems.add(orderItem);
        }

        // 5. Lưu toàn bộ Items xuống DB (1 cục)
        orderItemRepository.saveAll(orderItems);

        // 6. Cập nhật lại tổng tiền. KHÔNG CẦN gọi orderRepository.save(newOrder) nữa.
        // Khi hàm @Transactional kết thúc, Hibernate tự biết newOrder bị thay đổi TotalAmount và tự UPDATE.
        newOrder.setTotalAmount(totalOrderAmount);

        return newOrder;
    }
}
