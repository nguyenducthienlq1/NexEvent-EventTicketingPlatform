package com.nexevent.nexevent.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexevent.nexevent.domains.dto.response.RestResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException, IOException {
        // 1. Thiết lập Header trả về là JSON
        response.setStatus(HttpStatus.FORBIDDEN.value()); // 403
        response.setContentType("application/json;charset=UTF-8");

        // 2. Tạo nội dung thông báo lỗi
        RestResponse<Object> res = new RestResponse();
        res.setStatusCode(HttpStatus.FORBIDDEN.value());
        res.setError("Forbidden");
        res.setMessage("Bạn không có quyền truy cập vào tài nguyên này");

        // 3. Ghi JSON ra response
        mapper.writeValue(response.getWriter(), res);
    }
}