package com.nexevent.nexevent.repositories;

import com.nexevent.nexevent.domains.entities.Order;
import com.nexevent.nexevent.domains.entities.User;
import com.nexevent.nexevent.domains.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByUserAndStatus(User user, OrderStatus status);
}
