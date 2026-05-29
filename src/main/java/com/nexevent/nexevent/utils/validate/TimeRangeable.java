package com.nexevent.nexevent.utils.validate;

import java.time.LocalDateTime;

public interface TimeRangeable {
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
}