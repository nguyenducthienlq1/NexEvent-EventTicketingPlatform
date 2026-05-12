package com.nexevent.nexevent.controllers;

import com.nexevent.nexevent.domains.dto.request.OrderReqDTO;
import com.nexevent.nexevent.domains.dto.response.OrderResDTO;
import com.nexevent.nexevent.domains.dto.response.ResOrderPaidDTO;
import com.nexevent.nexevent.services.OrderService;
import com.nexevent.nexevent.utils.ApiMessage;
import com.nexevent.nexevent.utils.SecurityUtil;
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
    public ResponseEntity<OrderResDTO> createOrder(@Valid @RequestBody OrderReqDTO dto) {
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();

        OrderResDTO res = orderService.createOrder(dto, currentUserEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PatchMapping("/{idOrder}")
    @PreAuthorize("hasAuthority('USER')")
    @ApiMessage("Hủy đơn thành công")
    @Operation(summary = "Hủy đơn", description = "User hủy đơn đặt hàng")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long idOrder) {
        orderService.cancelOrder(idOrder);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{idOrder}/complete")
    @PreAuthorize("hasAuthority('USER')")
    @ApiMessage("Đặt vé thành công")
    @Operation(summary = "Hoàn tất Order", description = "User thanh toán Order đang Pending")
    public ResponseEntity<ResOrderPaidDTO> payOrder(@PathVariable Long idOrder) {
        ResOrderPaidDTO data = orderService.processPayment(idOrder);
        return ResponseEntity.ok(data);
    }
}