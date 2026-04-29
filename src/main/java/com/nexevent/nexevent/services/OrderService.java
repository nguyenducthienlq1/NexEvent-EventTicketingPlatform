package com.nexevent.nexevent.services;

import com.nexevent.nexevent.repositories.OrderRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final RedisTemplate<String, String> redisTemplate;
    public OrderService(OrderRepository orderRepository,
                        RedisTemplate<String, String> redisTemplate) {
        this.orderRepository = orderRepository;
        this.redisTemplate = redisTemplate;
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
}
