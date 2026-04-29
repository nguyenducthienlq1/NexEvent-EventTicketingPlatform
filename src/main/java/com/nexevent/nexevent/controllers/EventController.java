package com.nexevent.nexevent.controllers;

import com.nexevent.nexevent.domains.dto.request.EventReqDTO;
import com.nexevent.nexevent.domains.dto.response.EventResDTO;
import com.nexevent.nexevent.services.EventService;
import com.nexevent.nexevent.utils.ApiMessage;
import com.nexevent.nexevent.utils.SecurityUtil;
import com.nexevent.nexevent.utils.exception.IdInvalidException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${nexevent.api-prefix}/events")
@Tag(name = "Event API", description = "Quản lý sự kiện (Ticketbox Style)")
public class EventController {

    private final EventService eventService;
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }
    @GetMapping
    @ApiMessage("Lấy danh sách sự kiện thành công")
    @Operation(summary = "Lấy tất cả sự kiện", description = "Lấy danh sách sự kiện đang mở bán (có phân trang)")
    public ResponseEntity<Page<EventResDTO>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok().body(eventService.getAllActiveEvents(pageable));
    }

    @GetMapping("/search")
    @ApiMessage("Tìm kiếm sự kiện thành công")
    @Operation(summary = "Tìm kiếm sự kiện", description = "Tìm kiếm sự kiện theo tiêu đề")
    public ResponseEntity<Page<EventResDTO>> searchEvents(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok().body(eventService.searchActiveEvents(keyword, pageable));
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy chi tiết sự kiện thành công")
    @Operation(summary = "Lấy chi tiết 1 sự kiện", description = "Xem chi tiết thông tin của 1 sự kiện bằng ID")
    public ResponseEntity<EventResDTO> getEventById(@PathVariable Long id) throws IdInvalidException {
        return ResponseEntity.ok().body(eventService.getEventById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')") // CHẶN: Chỉ ai mang Role ADMIN mới chui qua được
    @ApiMessage("Tạo sự kiện thành công")
    @Operation(summary = "Tạo sự kiện mới (ADMIN)", description = "Thêm mới một sự kiện vào hệ thống")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sự kiện được tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ (thời gian sai, thiếu field)"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập (Không phải Admin)")
    })
    public ResponseEntity<EventResDTO> createEvent(@Valid @RequestBody EventReqDTO dto) throws IdInvalidException {
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        EventResDTO res = eventService.createEvent(dto, currentUserEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ApiMessage("Cập nhật sự kiện thành công")
    @Operation(summary = "Cập nhật sự kiện (ADMIN)", description = "Chỉnh sửa thông tin sự kiện")
    public ResponseEntity<EventResDTO> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventReqDTO dto) throws IdInvalidException {

        EventResDTO res = eventService.updateEvent(id, dto);
        return ResponseEntity.ok().body(res);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ApiMessage("Xóa sự kiện thành công")
    @Operation(summary = "Xóa sự kiện (ADMIN)", description = "Hủy (Soft-delete) một sự kiện khỏi hệ thống")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) throws IdInvalidException {
        eventService.deleteEvent(id);
        return ResponseEntity.ok().build();
    }
}