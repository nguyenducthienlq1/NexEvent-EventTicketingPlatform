package com.nexevent.nexevent.domains.dto.response;

import com.nexevent.nexevent.domains.enums.StatusTicket;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class TicketTypeResDTO {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private Integer totalQuantity;
    private Integer soldQuantity;
    private Integer remainQuantity;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private StatusTicket status;
}