package com.nexevent.nexevent.services;

import com.nexevent.nexevent.domains.dto.response.ResOrderPaidDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PaymentSseService {

    // Lần này ta map theo orderCode. Mỗi đơn hàng chỉ có 1 Emitter (1 khách đang chờ)
    private final Map<Long, SseEmitter> orderEmitters = new ConcurrentHashMap<>();

    // Frontend gọi hàm này để mở kết nối chờ thanh toán
    public SseEmitter subscribe(Long orderCode) {
        // Set timeout lâu một chút (VD: 15 phút) vì thao tác chuyển khoản tốn thời gian
        SseEmitter emitter = new SseEmitter(15 * 60 * 1000L);

        orderEmitters.put(orderCode, emitter);

        emitter.onCompletion(() -> orderEmitters.remove(orderCode));
        emitter.onTimeout(() -> orderEmitters.remove(orderCode));
        emitter.onError((e) -> orderEmitters.remove(orderCode));

        log.info("Khách hàng đang chờ thanh toán cho đơn: {}", orderCode);
        return emitter;
    }

    // Webhook gọi hàm này để đẩy vé về cho khách
    public void sendPaymentSuccess(Long orderCode, ResOrderPaidDTO orderPaidInfo) {
        SseEmitter emitter = orderEmitters.get(orderCode);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("payment-success")
                        .data(orderPaidInfo));

                emitter.complete();
            } catch (IOException e) {
                log.error("Lỗi khi gửi SSE vé cho đơn {}: ", orderCode, e);
                orderEmitters.remove(orderCode);
            }
        } else {
            log.warn("Nhận được tiền đơn {} nhưng khách đã đóng trình duyệt mất rồi!", orderCode);
        }
    }
}