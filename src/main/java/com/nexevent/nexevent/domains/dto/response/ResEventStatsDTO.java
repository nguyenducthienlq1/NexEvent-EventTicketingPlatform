package com.nexevent.nexevent.domains.dto.response;


import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResEventStatsDTO {
    private Double totalRevenue;
    private Integer totalIssued;
    private Integer totalSold;
    private Integer totalRemaining;

    // Chi tiết từng loại (VIP, Thường...) để vẽ Pie Chart
    private List<TicketTypeStat> ticketTypes;

    @Data
    @Builder
    public static class TicketTypeStat {
        private String ticketTypeName;
        private Integer issued;
        private Integer sold;
        private Integer remaining;
    }
}
