package com.nexevent.nexevent.domains.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderReqDTO {

    @NotEmpty(message = "Giỏ hàng không được để trống")
    @Valid
    private List<OrderItemReqDTO> items;
}