package com.nexevent.nexevent.domains.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(name = "Event Response", description = "Dữ liệu trả về chứa thông tin chi tiết của một sự kiện")
public class EventResDTO {

    @Schema(description = "Mã định danh (ID) duy nhất của sự kiện", example = "101")
    private Long id;

    @Schema(description = "Tên của sự kiện", example = "Lễ hội Âm nhạc Mùa hè NexEvent 2026")
    private String title;

    @Schema(description = "Mô tả chi tiết về sự kiện", example = "Sự kiện âm nhạc EDM lớn nhất năm...")
    private String description;

    @Schema(description = "Ngày chính thức diễn ra sự kiện", example = "2026-12-24T00:00:00")
    private LocalDateTime date;

    @Schema(description = "Đường dẫn URL ảnh bìa (Cover/Banner)", example = "https://example.com/images/nexevent-edm-2026.jpg")
    private String cover;

    @Schema(description = "Địa điểm tổ chức", example = "Sân vận động Mỹ Đình, Hà Nội")
    private String location;

    @Schema(description = "Thời gian bắt đầu", example = "2026-12-24T18:00:00")
    private LocalDateTime startTime;

    @Schema(description = "Thời gian kết thúc", example = "2026-12-24T23:30:00")
    private LocalDateTime endTime;

    @Schema(description = "Trạng thái hoạt động (true: Đang hiển thị, false: Đã bị ẩn/hủy)", example = "true")
    private boolean active;

    @Schema(description = "Họ tên của Admin/Người tổ chức đã tạo sự kiện này", example = "Nguyễn Đức Thiện")
    private String organizerName;
}