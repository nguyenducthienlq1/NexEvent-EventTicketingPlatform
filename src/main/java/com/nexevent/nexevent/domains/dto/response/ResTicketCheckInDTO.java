package com.nexevent.nexevent.domains.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin phản hồi sau khi quét vé thành công")
public class ResTicketCheckInDTO {

    @Schema(description = "ID của vé (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String ticketId;

    @Schema(description = "Tên sự kiện", example = "Đêm nhạc NexEvent 2026")
    private String eventName;

    @Schema(description = "Loại vé", example = "VVIP - Khu vực sát sân khấu")
    private String ticketTypeName;

    @Schema(description = "Tên khách hàng sở hữu vé", example = "Nguyễn Đức Thiện")
    private String customerName;

    @Schema(description = "Email khách hàng (dùng để đối soát nhanh)", example = "thien@example.com")
    private String customerEmail;

    @Schema(description = "Thời điểm quét vé thành công")
    private LocalDateTime checkedInAt;

    @Schema(description = "Cổng mà khách hàng vừa đi qua", example = "Cổng A - Cửa số 1")
    private String gate;

    @Schema(description = "Tên nhân viên thực hiện soát vé", example = "Bảo vệ Nguyễn Văn A")
    private String staffName;
}