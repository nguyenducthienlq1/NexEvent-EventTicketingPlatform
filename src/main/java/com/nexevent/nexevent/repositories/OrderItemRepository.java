package com.nexevent.nexevent.repositories;

import com.nexevent.nexevent.domains.entities.Order;
import com.nexevent.nexevent.domains.entities.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("select oi from OrderItem oi join fetch oi.ticketType where oi.order In (:orders)")
    List<OrderItem> findByOrdersWithTicketType(@Param("orders")List<Order> orders);

    @Query("select oi from OrderItem oi where oi.order.id = :idOrder")
    List<OrderItem> findByOrderId(@Param("idOrder")Long idOrder);
}
