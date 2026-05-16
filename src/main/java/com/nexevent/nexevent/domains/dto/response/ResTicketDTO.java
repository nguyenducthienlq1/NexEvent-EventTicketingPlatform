package com.nexevent.nexevent.domains.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResTicketDTO {
    private String ticketId;
    private String eventName;
    private LocalDateTime eventStartTime;
    private String eventLocation;
    private String ticketTypeName;
    private String status;
    private String qrCode;
}