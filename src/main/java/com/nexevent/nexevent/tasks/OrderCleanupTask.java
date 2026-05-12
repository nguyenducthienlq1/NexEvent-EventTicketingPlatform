package com.nexevent.nexevent.tasks;


import com.nexevent.nexevent.domains.entities.Order;
import com.nexevent.nexevent.domains.entities.OrderItem;
import com.nexevent.nexevent.domains.entities.TicketType;
import com.nexevent.nexevent.domains.enums.OrderStatus;
import com.nexevent.nexevent.domains.enums.StatusTicketType;
import com.nexevent.nexevent.repositories.OrderItemRepository;
import com.nexevent.nexevent.repositories.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderCleanupTask {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    public OrderCleanupTask(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository){
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }
    @Value("${nexevent.order.expired-time-order-pending}")
    private int expireTime;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredPendingOrder(){
        LocalDateTime expired = LocalDateTime.now().minusMinutes(expireTime);
        //Kiểm tra xem có đơn nào PENDING mà trên 15p chưa
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, expired);

        if (expiredOrders.isEmpty()){
            return;
        }
        log.info("[ORDER CLEANUP] Detected {} expired pending order(s). Canceling and restocking tickets...", expiredOrders.size());
        // Xử lý từng đơn hàng vi phạm
        // Chuyển tất cả Order sang hủy đơn hết
        for (Order order :expiredOrders){
            order.setStatus(OrderStatus.CANCELLED);
        }
        // Lấy tất cả Loại vé trong Đơn đặt hàng ra
        List<OrderItem> allExpiredItems = orderItemRepository.findByOrdersWithTicketType(expiredOrders);
        for (OrderItem orderItem : allExpiredItems){
            // Xét từng Loại vé
            TicketType ticketType = orderItem.getTicketType();
            int currentSold = ticketType.getSoldQuantity() == null ? 0 : ticketType.getSoldQuantity();
            // Trả lại số lượng đã bán cho Loại vé
            ticketType.setSoldQuantity(currentSold - orderItem.getQuantity());
            // Nếu loại vé đó SOLD_OUT thì chuyển lại thành AVAILABLE
            if (ticketType.getStatus() == StatusTicketType.SOLD_OUT){
                ticketType.setStatus(StatusTicketType.AVAILABLE);
            }
            log.info("[ORDER CLEANUP] Cleanup complete! Tickets successfully restored to inventory.");        }
    }
}
