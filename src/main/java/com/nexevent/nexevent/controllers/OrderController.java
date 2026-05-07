package com.nexevent.nexevent.controllers;

import com.nexevent.nexevent.domains.dto.request.OrderReqDTO;
import com.nexevent.nexevent.domains.dto.response.OrderResDTO;
import com.nexevent.nexevent.domains.entities.Order;
import com.nexevent.nexevent.services.OrderService;
import com.nexevent.nexevent.utils.ApiMessage;
import com.nexevent.nexevent.utils.SecurityUtil;
import com.nexevent.nexevent.utils.exception.IdInvalidException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${nexevent.api-prefix}/orders")
@Tag(name = "Order API", description = "Quản lý Đặt vé & Đơn hàng")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER')")
    @ApiMessage("Tạo đơn đặt vé thành công")
    @Operation(summary = "Đặt vé (Checkout)", description = "User gửi danh sách vé muốn mua để tạo đơn hàng")
    public ResponseEntity<OrderResDTO> createOrder(@Valid @RequestBody OrderReqDTO dto) throws IdInvalidException {
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();

        Order order = orderService.createOrder(dto, currentUserEmail);

        OrderResDTO res = OrderResDTO.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }
}