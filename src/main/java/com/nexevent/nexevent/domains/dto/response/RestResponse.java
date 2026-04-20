package com.nexevent.nexevent.domains.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"statusCode", "error", "message", "data"})
public class RestResponse<T> {

    private int statusCode;
    private String error;

    private Object message;
    private T data;
}