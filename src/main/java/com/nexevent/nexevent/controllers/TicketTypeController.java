package com.nexevent.nexevent.controllers;

import com.nexevent.nexevent.domains.dto.request.TicketTypeReqDTO;
import com.nexevent.nexevent.domains.dto.response.TicketTypeResDTO;
import com.nexevent.nexevent.services.TicketTypeService;
import com.nexevent.nexevent.utils.ApiMessage;
import com.nexevent.nexevent.utils.exception.IdInvalidException;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("${nexevent.api-prefix}")
@Tag(name = "Ticket Type API", description = "Quản lý các loại vé của sự kiện")
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    public TicketTypeController(TicketTypeService ticketTypeService) {
        this.ticketTypeService = ticketTypeService;
    }


    @GetMapping("/events/{eventId}/ticket-types")
    @ApiMessage("Lấy danh sách vé của sự kiện thành công")
    @Operation(summary = "Xem vé sự kiện (Public)", description = "Lấy danh sách các loại vé ĐANG MỞ BÁN của 1 sự kiện (Có phân trang)")
    public ResponseEntity<Page<TicketTypeResDTO>> getTicketsByEvent(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok().body(ticketTypeService.getActiveTicketsByEvent(eventId, pageable));
    }

    @GetMapping("/admin/events/{eventId}/ticket-types")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ApiMessage("Lấy toàn bộ danh sách vé thành công")
    @Operation(summary = "Xem TOÀN BỘ vé sự kiện (Admin)", description = "Lấy tất cả vé (cả ẩn và hiện) của 1 sự kiện (Có phân trang)")
    public ResponseEntity<Page<TicketTypeResDTO>> getAllTicketsForAdmin(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok().body(ticketTypeService.getAllTicketsByEventForAdmin(eventId, pageable));
    }

    @PostMapping("/ticket-types")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ApiMessage("Tạo loại vé mới thành công")
    @Operation(summary = "Tạo loại vé (Admin)")
    public ResponseEntity<TicketTypeResDTO> createTicketType(@Valid @RequestBody TicketTypeReqDTO dto) throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketTypeService.createTicketType(dto));
    }

    @PutMapping("/ticket-types/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ApiMessage("Cập nhật loại vé thành công")
    @Operation(summary = "Cập nhật loại vé (Admin)")
    public ResponseEntity<TicketTypeResDTO> updateTicketType(
            @PathVariable Long id,
            @Valid @RequestBody TicketTypeReqDTO dto) throws IdInvalidException {
        return ResponseEntity.ok().body(ticketTypeService.updateTicketType(id, dto));
    }

    @DeleteMapping("/ticket-types/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ApiMessage("Hủy bán loại vé thành công")
    @Operation(summary = "Hủy bán loại vé (Admin)", description = "Soft delete: Chuyển trạng thái vé thành INACTIVE")
    public ResponseEntity<Void> deleteTicketType(@PathVariable Long id) throws IdInvalidException {
        ticketTypeService.deleteTicketType(id);
        return ResponseEntity.ok().build();
    }
}