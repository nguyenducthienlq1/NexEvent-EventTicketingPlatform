package com.nexevent.nexevent.controllers;

import com.nexevent.nexevent.domains.dto.request.TicketCheckInReqDTO;
import com.nexevent.nexevent.domains.dto.response.ResTicketCheckInDTO;
import com.nexevent.nexevent.services.CheckinService;
import com.nexevent.nexevent.utils.ApiMessage;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${nexevent.api-prefix}/checkin")
public class CheckinController {
    private final CheckinService checkinService;
    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CHECKER')")
    @ApiMessage("Quét QRCode vào cổng thành công")
    @Operation(summary = "Quét QR vào cổng", description = "User đưa qrcode của vé ra, Checker quét vé và nhập gate, sau đó gửi api về đây để kiểm soát")
    public ResponseEntity<ResTicketCheckInDTO> checkin(@Valid @RequestBody TicketCheckInReqDTO checkinDTO) {
        ResTicketCheckInDTO res = checkinService.checkinMethod(checkinDTO);
        return ResponseEntity.ok(res);
    }
}
