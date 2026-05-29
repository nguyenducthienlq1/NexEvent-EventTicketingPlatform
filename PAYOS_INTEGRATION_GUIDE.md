# Hướng Dẫn Tích Hợp & Kiểm Thử Cổng Thanh Toán PayOS (NexEvent)

Tài liệu này hướng dẫn chi tiết cách thiết lập, cấu hình và kiểm thử luồng thanh toán qua mã VietQR sử dụng cổng thanh toán trung gian PayOS cho dự án NexEvent.

## 1. Tổng Quan Luồng Thanh Toán (Bao gồm SSE)

Hệ thống sử dụng cơ chế thanh toán bất đồng bộ, kết hợp Webhook và Server-Sent Events (SSE) để mang lại trải nghiệm Real-time hoàn chỉnh:

1. Khởi tạo: Khách hàng chọn vé và bấm nút thanh toán. Frontend gửi yêu cầu tới Backend. Backend gọi sang API của PayOS để tạo link thanh toán (chứa mã VietQR động).
2. Hiển thị & Chờ đợi: Frontend nhận link thanh toán, hiển thị mã QR Code lên màn hình cho khách quét. ĐỒNG THỜI, Frontend mở một kết nối ngầm (SSE) tới Backend thông qua Endpoint subscribe để hóng dữ liệu.
3. Thanh toán: Khách hàng mở ứng dụng ngân hàng hoặc ví điện tử (MoMo, ZaloPay, Viettel Money...) quét mã VietQR và xác nhận chuyển tiền thật.
4. Webhook kích hoạt: Ngay khi tiền ting ting vào tài khoản ngân hàng nguồn, hệ thống PayOS nhận diện được biến động số dư và âm thầm bắn một HTTP POST Request (Webhook) về Endpoint public của Backend.
5. Xử lý nghiệp vụ: Backend nhận Webhook, xác thực chữ ký bảo mật để tránh hacker fake request. Nếu chữ ký hợp lệ, Backend chạy hàm xử lý thanh toán, chuyển trạng thái đơn hàng sang PAID, và gọi sang TicketService để sinh mã QR cho các vé tương ứng.
6. Real-time UX: Backend lấy dữ liệu vé vừa sinh, ném vào đường ống SSE đang mở để đẩy thẳng về trình duyệt của khách hàng. Frontend tóm được event này, ngay lập tức render màn hình chúc mừng mua vé thành công mà khách hàng không cần bấm F5 để tải lại trang.

---

## 2. Đăng Ký Tài Khoản & Lấy API Key PayOS

Để Backend Spring Boot có thể liên lạc được với PayOS, bạn cần có một tài khoản và một kênh thanh toán hoạt động:

1. Truy cập trang chủ chính thức của PayOS và tiến hành đăng ký tài khoản cá nhân thông qua số điện thoại và số CCCD.
2. Đăng nhập vào trang quản trị (Dashboard), thực hiện liên kết tài khoản ngân hàng cá nhân (ví dụ: Techcombank, MB Bank, hoặc Viettel Money) để làm tài khoản nguồn nhận tiền mua vé.
3. Di chuyển đến menu Kênh thanh toán ở thanh điều hướng bên trái và chọn Tạo kênh thanh toán mới. Đặt tên gợi nhớ cho cổng, ví dụ: NexEvent.
4. Sau khi tạo thành công, bấm vào chi tiết kênh NexEvent để lấy thông tin. Tại đây, bạn  sẽ tìm thấy 3 mã khóa bí mật vô cùng quan trọng dùng để nhét vào source code:
    - Client ID
    - API Key
    - Checksum Key

---

## 3. Cấu Hơn Backend (Spring Boot)

### 3.1. Khai báo thông số môi trường
Mở file application.properties (hoặc application.yml) trong project Spring Boot của bạn  và dán 3 cấu khóa bí mật vừa lấy ở bước trước vào:
``` 
payos.client-id=YOUR_CLIENT_ID_HERE
payos.api-key=YOUR_API_KEY_HERE
payos.checksum-key=YOUR_CHECKSUM_KEY_HERE
```

### 3.2. Lưu ý "chí mạng" về độ dài nội dung chuyển khoản
Hệ thống Core Banking của các ngân hàng và PayOS giới hạn trường nội dung chuyển khoản (description) cực kỳ nghiêm ngặt: Tối đa 25 ký tự và Tuyệt đối không chứa tiếng Việt có dấu hoặc ký tự đặc biệt.
Khi bạn  thiết lập biến description trong CreatePaymentLinkRequest, phải tối ưu chuỗi ký tự thật ngắn gọn.
- Sai: .description("Thanh toán hóa đơn mua vé đơn hàng " + order.get().getOrderCode()) -> Quá 25 ký tự, PayOS trả về lỗi 400 Bad Request ngay lập tức.
- Đúng: .description("Order: " + order.get().getOrderCode()) -> Ngắn gọn, an toàn và đảm bảo dưới 25 ký tự.

---

## 4. Thiết Lập Ngrok & Webhook (Môi Trường Local)

Khi bạn  chạy ứng dụng ở máy cá nhân (localhost:8080), máy chủ của PayOS ở trên mạng Internet không có cách nào liên lạc hay bắn Webhook trực tiếp về máy bạn  được. Do đó, cần sử dụng công cụ Ngrok để đào một đường hầm bảo mật.
Nếu trên môi trường Production, bạn sẽ đưa API được host của webhook vào PayOS WebhookURL nên sẽ không cần bước này.
### 4.1. Cài đặt và khởi động Ngrok
1. Tải phần mềm Ngrok về máy tính (Khuyên dùng bản cài đặt qua Microsoft Store để tự động nhận biến môi trường toàn cục).
2. Đăng nhập vào trang quản trị của Ngrok, sao chép mã AuthToken cá nhân của bạn .
3. Mở Terminal hoặc Command Prompt trên máy tính lên, gõ lệnh sau để xác thực tài khoản (chỉ cần làm một lần duy nhất):
   ngrok config add-authtoken <MÃ_AUTHTOKEN_CỦA_BRO>
4. Kích hoạt đường hầm thông thẳng vào cổng port Backend Spring Boot bằng câu lệnh:
   ngrok http 8080
5. Ngrok sẽ hiển thị một giao diện dòng lệnh màu đen. Hãy tìm đến mục Forwarding và sao chép đường link công khai có đuôi .ngrok-free.dev hoặc .ngrok-free.app (Ví dụ: https://salami-gratified-phobia.ngrok-free.dev).
6. Lưu ý đặc biệt: Hãy giữ cho cửa sổ Terminal chạy Ngrok này mở liên tục trong suốt quá trình test. Nếu tắt đi bật lại, Ngrok bản miễn phí sẽ sinh ra một đường link public hoàn toàn mới.

### 4.2. Khai báo URL Webhook lên Dashboard PayOS
1. Quay trở lại trang giao diện quản trị PayOS, bấm vào Kênh thanh toán -> Chọn Chỉnh sửa thông tin kênh NexEvent.
2. Tìm đến ô Webhook Url, dán đường link public của Ngrok vừa sao chép vào, đồng thời bắt buộc phải nối thêm đuôi API xử lý của Controller vào phía sau.
    - Định dạng chuẩn xác: https://<ngrok-id>.ngrok-free.dev/api/v1/payments/webhook
3. Bấm nút Lưu để hoàn tất.

*Mẹo vượt chốt chặn test của PayOS:* Khi bạn  bấm nút Lưu, hệ thống PayOS sẽ bắn ngay lập tức một request ảo để kiểm tra xem URL Webhook này có hoạt động thật không. Nhờ kỹ thuật bọc lót khối logic nghiệp vụ bằng try-catch phụ lồng nhau mà anh em mình đã triển khai trong PaymentController, Server Spring Boot của bạn  sẽ bỏ qua lỗi "không tìm thấy đơn hàng ảo" và luôn trả về trạng thái HTTP 200 OK mượt mà cho PayOS, giúp lưu URL thành công 100% kèm tick xanh rực rỡ!

---

## 5. Kịch Bản Test End-to-End (Bằng Postman / Swagger & Trình Duyệt)

Vì chưa làm Frontend, bạn  hoàn toàn có thể test "chay" luồng xử lý tiền nong real-time cực kỳ chuyên nghiệp theo các bước sau:

### Bước 1: Giả lập Frontend gọi API tạo đơn hàng lấy QR Code
- Đăng nhập vào hệ thống để lấy Token hợp lệ (Vì API này có gắn @PreAuthorize).
- Mở Swagger UI hoặc Postman, thực hiện gửi request: POST http://localhost:8080/api/v1/payments/create-payment-link/{orderId} (Với orderId là ID của một đơn hàng đang ở trạng thái PENDING có sẵn trong PostgreSQL của bạn ).
- Nhìn vào Response body trả về, tìm đến trường dữ liệu checkoutUrl nằm trong đối tượng data và sao chép cái link đó lại.

### Bước 2: Đóng vai Frontend mở giao diện quét mã và cắm ống nghe SSE
- Tab số 1 trên Chrome (Giao diện hiển thị QR): Dán cái đường link checkoutUrl vừa copy vào thanh địa chỉ rồi nhấn Enter. Trình duyệt sẽ hiển thị trang thanh toán chính chủ của PayOS có chứa thông tin số tiền (Ví dụ: 2,000 VND) kèm mã QR Code VietQR động. Hãy giữ nguyên tab này, đừng quét vội!
- Tab số 2 trên Chrome (Giả lập ngầm ống nghe SSE của khách): Mở một tab trình duyệt hoàn toàn mới, gõ vào đường dẫn: GET http://localhost:8080/api/v1/payments/subscribe/{orderId} (Nhớ điền đúng ID đơn hàng bạn  vừa tạo ở Bước 1) rồi nhấn Enter. Hoặc bạn có thể test trực tiếp trên Postman, tạo request như bình thường và bấm, trang response sẽ xoay vòng tròn và chờ dữ liệu được đẩy qua.
- Hiện tượng chuẩn chỉ: Bro sẽ thấy Tab số 2 này cứ xoay vòng liên tục (loading) và trang hiển thị trắng tinh. Đây là hiện tượng hoàn toàn chính xác, chứng tỏ đường ống SSE đang được giữ mở toang để hóng dữ liệu từ Backend đẩy về.

### Bước 3: Thực hiện quét mã chuyển tiền thật
- Cầm điện thoại cá nhân lên, mở ứng dụng ngân hàng (Techcombank) hoặc ví điện tử (MoMo) ra.
- Chọn tính năng Quét mã QR, đưa camera lên quét cái mã VietQR đang hiển thị ở Tab số 1 trên màn hình máy tính.
- Hệ thống sẽ tự động điền đúng số tiền và nội dung chuyển khoản không dấu dạng "Order ORD...". Tiến hành bấm xác nhận chuyển khoản tiền thật.

### Bước 4: Thưởng thức kết quả xử lý tự động (Magic Time)
Chỉ khoảng từ 3 đến 5 giây sau khi ứng dụng ngân hàng trên điện thoại báo chuyển khoản thành công, bạn  hãy hướng mắt nhìn vào màn hình máy tính để thấy phép màu của code:
1. Tại Console log của Spring Boot: Bật lên dòng chữ thông báo cực kỳ uy tín: "PayOS báo: Đã nhận được tiền cho mã đơn: ..." theo sau là dòng chữ "Xử lý đơn hàng và bắn vé thành công cho đơn: ...".
2. Tại Tab số 2 trên Chrome (SSE): Trình duyệt đột ngột dừng xoay vòng loading, ngay lập tức phụt ra một chuỗi JSON dài chứa toàn bộ thông tin vé, loại vé và chuỗi qrcode của vé mà hàm processPayment vừa sinh ra dưới Database PostgreSQL.
3. Tại Tab số 1 trên Chrome (PayOS Checkout): Màn hình quét mã tự động chuyển thành dấu tick xanh khổng lồ báo "Thanh toán thành công" và tự động chuyển hướng (Redirect) người dùng về trang thành công (http://localhost:5174/success) đúng như luồng nghiệp vụ thực tế của các hệ thống thương mại lớn.