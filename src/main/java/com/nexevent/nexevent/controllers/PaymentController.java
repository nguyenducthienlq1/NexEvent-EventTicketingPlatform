package com.nexevent.nexevent.controllers;

import com.nexevent.nexevent.domains.dto.response.ResOrderPaidDTO;
import com.nexevent.nexevent.domains.entities.Order;
import com.nexevent.nexevent.domains.enums.OrderStatus;
import com.nexevent.nexevent.services.OrderService;
import com.nexevent.nexevent.services.PaymentSseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.payos.PayOS;
import com.nexevent.nexevent.domains.dto.response.RestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.util.Optional;


@RestController
@RequestMapping("${nexevent.api-prefix}/payments")
@Slf4j
public class PaymentController {
    private final PayOS payOS;
    private final OrderService orderService;
    private final PaymentSseService paymentSseService;
    public PaymentController(PayOS payOS, OrderService orderService,
                             PaymentSseService paymentSseService) {
        this.payOS = payOS;
        this.orderService = orderService;
        this.paymentSseService = paymentSseService;
    }

    @Value("${nexevent.api-frontend}")
    private String url_frontend;

    @PostMapping("/create-payment-link/{orderId}")
    public ResponseEntity<RestResponse<Object>> createPaymentLink(@PathVariable Long orderId) {
        try {
            Optional<Order> order = orderService.findById(orderId);
            if(order.get().getStatus().equals(OrderStatus.PAID)) {
                     throw new RuntimeException("Đơn hàng này đã được thanh toán!");
            }
            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name("Order #" + order.get().getOrderCode())
                    .quantity(1)
                    .price(order.get().getTotalAmount().longValue())
                    .build();
            // Demo tạo 1 đơn hàng (Thực tế bro lấy data từ Cart của khách)
            CreatePaymentLinkRequest paymentRequest = CreatePaymentLinkRequest.builder()
                    .orderCode(order.get().getId())
                    .amount(order.get().getTotalAmount().longValue())
                    .description("Thanh toán đơn hàng " + order.get().getOrderCode())
                    .cancelUrl(url_frontend + "/cancel")
                    .returnUrl(url_frontend + "/success")
                    .item(item)
                    .build();

            var paymentLink = payOS.paymentRequests().create(paymentRequest);

            RestResponse<Object> res = new RestResponse<>();
            res.setStatusCode(HttpStatus.OK.value());
            res.setMessage("Tạo link thanh toán thành công");
            res.setData(paymentLink);

            return ResponseEntity.ok(res);

        } catch (Exception e) {
            log.error("Lỗi tạo thanh toán cho order {}: ", orderId, e);
            throw new RuntimeException("Lỗi tạo thanh toán: " + e.getMessage());
        }
    }

    @GetMapping("/subscribe/{orderCode}")
    public SseEmitter subscribePaymentEvent(@PathVariable Long orderCode) {
        return paymentSseService.subscribe(orderCode);
    }

    // 2. API HỨNG WEBHOOK (PayOS sẽ âm thầm gọi cái này khi tiền "ting ting")
    @PostMapping("/webhook")
    public ResponseEntity<RestResponse<Object>> handlePayOSWebhook(@RequestBody Webhook webhook) {
        RestResponse<Object> res = new RestResponse<>();
        try {
            // QUAN TRỌNG: Xác thực chữ ký để chống bị hack/gọi fake
            WebhookData data = payOS.webhooks().verify(webhook);
            log.info("PayOS báo: Đã nhận được tiền cho mã đơn: {}", data.getOrderCode());
            ResOrderPaidDTO orderPaidInfo = orderService.processPayment(data.getOrderCode());


            // 2. Bắn SSE Realtime: sseService.sendPaymentSuccessEvent(data.getOrderCode());
            paymentSseService.sendPaymentSuccess(data.getOrderCode(), orderPaidInfo);

            res.setStatusCode(HttpStatus.OK.value());
            res.setMessage("Webhook processed successfully");
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            log.error("Lỗi xử lý Webhook: ", e);
            res.setStatusCode(HttpStatus.BAD_REQUEST.value());
            res.setError("Invalid Webhook");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
        }
    }
}
