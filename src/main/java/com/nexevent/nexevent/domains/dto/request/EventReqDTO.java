package com.nexevent.nexevent.domains.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(name = "Event Request", description = "Payload yêu cầu tạo mới hoặc cập nhật thông tin sự kiện")
public class EventReqDTO {

    @NotBlank(message = "Tên sự kiện không được để trống")
    @Schema(description = "Tên của sự kiện", example = "Lễ hội Âm nhạc Mùa hè NexEvent 2026")
    private String title;

    @Schema(description = "Mô tả chi tiết về sự kiện (có thể chứa HTML)", example = "Sự kiện âm nhạc EDM lớn nhất năm với sự góp mặt của các DJ hàng đầu thế giới.")
    private String description;

    @NotNull(message = "Ngày diễn ra không được để trống")
    @Future(message = "Ngày diễn ra sự kiện phải ở trong tương lai")
    @Schema(description = "Ngày chính thức diễn ra sự kiện", example = "2026-12-24T00:00:00")
    private LocalDateTime date;

    @Schema(description = "Đường dẫn URL ảnh bìa (Cover/Banner) của sự kiện", example = "https://example.com/images/nexevent-edm-2026.jpg")
    private String cover;

    @NotBlank(message = "Địa điểm không được để trống")
    @Schema(description = "Địa điểm tổ chức sự kiện", example = "Sân vận động Mỹ Đình, Hà Nội")
    private String location;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    @Schema(description = "Thời gian mở cửa đón khách / Bắt đầu chương trình", example = "2026-12-24T18:00:00")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    @Schema(description = "Thời gian dự kiến kết thúc chương trình", example = "2026-12-24T23:30:00")
    private LocalDateTime endTime;
}