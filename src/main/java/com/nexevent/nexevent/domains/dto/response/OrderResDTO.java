package com.nexevent.nexevent.domains.dto.response;

import com.nexevent.nexevent.domains.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class OrderResDTO {
    private Long id;
    private String orderCode;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;

}