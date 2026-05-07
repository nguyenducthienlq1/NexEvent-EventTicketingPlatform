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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @Value("${nexevent.order.max-ticket-per-order}")
    private int maxTicketPerOrder;

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
    public Order createOrder(OrderReqDTO dto, String userEmail) {
        //Lấy thông tin người dùng
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IdInvalidException("Người dùng không tồn tại"));

        // Dùng Redis khóa request spam, lỡ bấm nhanh quá
        String lockKey = "lock:create_order:" + user.getId();
        Boolean isLocked = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 5, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(isLocked)) {
            throw new IdInvalidException("Bạn thao tác quá nhanh! Vui lòng chờ vài giây.");
        }
        try {
            // Bắt buộc thanh toán Order trước khi tạo Order mới
            if (orderRepository.existsByUserAndStatus(user, OrderStatus.PENDING)) {
                throw new IdInvalidException("Bạn đang có một đơn hàng chưa thanh toán! Vui lòng hoàn tất hoặc hủy đơn cũ.");
            }

            // Chỉ cho phép mua số lượng vé giới hạn
            int totalTicket = dto.getItems().stream().mapToInt(OrderItemReqDTO::getQuantity).sum();
            if (totalTicket > maxTicketPerOrder) { // Lưu ý tên biến maxTicketsPerOrder
                throw new IdInvalidException("Giới hạn mua quá mức! Bạn chỉ được mua tối đa " + maxTicketPerOrder + " vé.");
            }

            // Gom nhóm tránh Hacker gửi trùng ID vé
            Map<Long, Integer> ticketQuantityMap = dto.getItems().stream()
                    .collect(Collectors.groupingBy(
                            OrderItemReqDTO::getTicketTypeId,
                            Collectors.summingInt(OrderItemReqDTO::getQuantity)
                    ));

            // Lấy các loại vé (TicketType)
            List<TicketType> ticketTypes = ticketTypeRepository.findTicketsWithEventByIds(ticketQuantityMap.keySet());
            if (ticketTypes.size() != ticketQuantityMap.size()) {
                throw new IdInvalidException("Một số loại vé không tồn tại trong hệ thống!");
            }

            Map<Long, TicketType> ticketTypeMap = ticketTypes.stream()
                    .collect(Collectors.toMap(TicketType::getId, t -> t));

            Order newOrder = Order.builder()
                    .user(user)
                    .orderCode(generateOrderCode())
                    .totalAmount(BigDecimal.ZERO)
                    .status(OrderStatus.PENDING)
                    .build();
            newOrder = orderRepository.save(newOrder);

            BigDecimal totalOrderAmount = BigDecimal.ZERO;
            List<OrderItem> orderItems = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (Map.Entry<Long, Integer> entry : ticketQuantityMap.entrySet()) {
                Long ticketId = entry.getKey();
                Integer quantityToBuy = entry.getValue();
                TicketType ticketType = ticketTypeMap.get(ticketId);

                if (ticketType.getStatus() != StatusTicket.AVAILABLE) {
                    throw new IdInvalidException("Vé '" + ticketType.getTitle() + "' hiện không mở bán.");
                }

                if (ticketType.getStartTime() != null && now.isBefore(ticketType.getStartTime())) {
                    throw new IdInvalidException("Vé '" + ticketType.getTitle() + "' chưa tới giờ mở bán!");
                }

                if (ticketType.getEndTime() != null && now.isAfter(ticketType.getEndTime())) {
                    throw new IdInvalidException("Vé '" + ticketType.getTitle() + "' đã kết thúc thời gian mở bán!");
                }

                if (ticketType.getEvent() == null || !ticketType.getEvent().isActive()) { // Chú ý hàm isActive()
                    throw new IdInvalidException("Sự kiện của loại vé này đã bị hủy hoặc ngừng hoạt động!");
                }

                // Tính toán số lượng vé còn lại
                int currentSold = ticketType.getSoldQuantity() == null ? 0 : ticketType.getSoldQuantity();
                int remainQty = ticketType.getTotalQuantity() - currentSold;

                if (remainQty < quantityToBuy) {
                    throw new IdInvalidException("Vé '" + ticketType.getTitle() + "' chỉ còn " + remainQty + " vé!");
                }

                BigDecimal itemSubtotal = ticketType.getPrice().multiply(BigDecimal.valueOf(quantityToBuy));
                totalOrderAmount = totalOrderAmount.add(itemSubtotal);

                // Cập nhật số lượng vé
                ticketType.setSoldQuantity(currentSold + quantityToBuy);
                if (ticketType.getSoldQuantity().equals(ticketType.getTotalQuantity())) {
                    ticketType.setStatus(StatusTicket.SOLD_OUT);
                }

                OrderItem orderItem = OrderItem.builder()
                        .order(newOrder)
                        .ticketType(ticketType)
                        .quantity(quantityToBuy)
                        .unitPrice(ticketType.getPrice())
                        .subtotal(itemSubtotal)
                        .build();
                orderItems.add(orderItem);
            }

            orderItemRepository.saveAll(orderItems);
            newOrder.setTotalAmount(totalOrderAmount);
            return newOrder;

        } finally {
            // Mở khóa Redis ngay lập tức sau khi chạy xong (dù thành công hay thất bại)
            redisTemplate.delete(lockKey);
        }
    }
}
