package com.nexevent.nexevent.controllers;


import com.nexevent.nexevent.domains.dto.response.ResTicketDTO;
import com.nexevent.nexevent.domains.dto.response.RestResponse;
import com.nexevent.nexevent.domains.enums.StatusTicket;
import com.nexevent.nexevent.services.TicketService;
import com.nexevent.nexevent.utils.ApiMessage;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${nexevent.api-prefix}/ticket")
public class TicketController {
    private final TicketService ticketService;
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('USER')")
    @ApiMessage("Lấy toàn bộ danh sách vé của một người theo nhiều tiêu chí")
    @Operation(summary = "Xem vé sự kiện (USER)", description = "Lấy tất cả vé của 1 người theo nhiều tiêu chí (Có phân trang)")
    public ResponseEntity<Page<ResTicketDTO>> getMyTickets(
            @RequestParam(required = false) StatusTicket status,
            @ParameterObject @PageableDefault(size = 10, sort = "orderItem.ticketType.event.startTime", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<ResTicketDTO> data = ticketService.getMyTickets(status, pageable);

        return ResponseEntity.ok(data);
    }
}
