package com.nexevent.nexevent.domains.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResCheckinStatsDTO {
    private Long totalSold;
    private Long totalCheckedIn;
    private Double checkinRate;
}
