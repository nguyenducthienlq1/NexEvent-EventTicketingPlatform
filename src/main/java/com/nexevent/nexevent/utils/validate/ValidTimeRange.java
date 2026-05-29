package com.nexevent.nexevent.utils.validate;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TimeRangeValidator.class)
@Target({ElementType.TYPE}) // Dán lên đỉnh class DTO
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTimeRange {

    String message() default "Thời gian kết thúc phải diễn ra sau thời gian bắt đầu!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}