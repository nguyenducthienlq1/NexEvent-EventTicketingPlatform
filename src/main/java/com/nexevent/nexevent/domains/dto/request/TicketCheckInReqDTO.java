package com.nexevent.nexevent.domains.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "Ticket Checkin", description = "Dữ liệu quét vé vào cổng")
public class TicketCheckInReqDTO {
    @NotNull(message = "Mã QR của vé không được để trống")
    @Schema(description = "Mã QR của vé ")
    String qrToken;
    @NotNull(message = "Cổng kiểm soát vé của sự kiện, không được để trống")
    @Schema(description = "Cổng kiểm soát vé sự kiện", example = "G1")
    String gate;
}
