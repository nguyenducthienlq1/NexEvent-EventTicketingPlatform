package com.nexevent.nexevent.domains.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResLiveCheckinDTO {
    private String eventName;
    private String gate;
    private String ticketType;
    private String status;
    private String message;


}