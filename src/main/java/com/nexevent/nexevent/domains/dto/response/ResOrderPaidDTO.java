package com.nexevent.nexevent.domains.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ResOrderPaidDTO {
    private String orderCode;
    private String status;
    private int totalTicketsGenerated;
    private List<TicketInfo> tickets;

    @Data
    @Builder
    public static class TicketInfo {
        private String id;
        private String ticketTypeName;
        private String qrCode;
    }
}