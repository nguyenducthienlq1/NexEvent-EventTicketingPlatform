package com.nexevent.nexevent.domains.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemReqDTO {

    @NotNull(message = "ID loại vé không được để trống")
    private Long ticketTypeId;

    @NotNull(message = "Số lượng vé không được để trống")
    @Min(value = 1, message = "Số lượng mua ít nhất là 1")
    private Integer quantity;
}