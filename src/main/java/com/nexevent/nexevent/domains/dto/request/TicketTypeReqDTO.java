package com.nexevent.nexevent.domains.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(name = "Ticket Type Request", description = "Dữ liệu tạo mới hoặc cập nhật loại vé")
public class TicketTypeReqDTO {

    @NotNull(message = "ID của sự kiện không được để trống")
    @Schema(description = "ID của sự kiện chứa vé này", example = "1")
    private Long eventId;

    @NotBlank(message = "Tên loại vé không được để trống")
    @Schema(description = "Tên loại vé", example = "Vé VIP (Sát sân khấu)")
    private String title;

    @Schema(description = "Mô tả quyền lợi vé", example = "- Được giao lưu cùng thần tượng\n- Tặng 1 lightstick")
    private String description;

    @NotNull(message = "Giá vé không được để trống")
    @Min(value = 0, message = "Giá vé không được âm")
    @Schema(description = "Giá vé (VND)", example = "1500000")
    private BigDecimal price;

    @NotNull(message = "Tổng số lượng vé không được để trống")
    @Min(value = 1, message = "Số lượng vé phải lớn hơn 0")
    @Schema(description = "Tổng số lượng vé phát hành", example = "500")
    private Integer totalQuantity;

    @NotNull(message = "Thời gian mở bán không được để trống")
    @Schema(description = "Thời gian bắt đầu mở bán", example = "2026-10-01T08:00:00")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian đóng bán không được để trống")
    @Schema(description = "Thời gian kết thúc bán vé", example = "2026-12-23T23:59:59")
    private LocalDateTime endTime;
}