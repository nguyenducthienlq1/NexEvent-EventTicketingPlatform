package com.nexevent.nexevent.utils.validate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;

public class TimeRangeValidator implements ConstraintValidator<ValidTimeRange, TimeRangeable> {

    @Override
    public boolean isValid(TimeRangeable dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        LocalDateTime start = dto.getStartTime();
        LocalDateTime end = dto.getEndTime();

        if (start == null || end == null) {
            return true;
        }

        return end.isAfter(start);
    }
}