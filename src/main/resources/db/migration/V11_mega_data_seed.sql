-- =========================================================================
-- 1. SEED TICKET_TYPES CHO EVENT ID = 2 (Vietnam Web Summit 2026)
-- =========================================================================
INSERT INTO ticket_types (title, price, total_quantity, sold_quantity, status, start_time, end_time, event_id, description)
VALUES
    (
        'Vé Standard (Developer)',
        350000.00,
        500,
        120,
        'AVAILABLE',
        '2026-05-01 00:00:00',
        '2026-09-09 23:59:59',
        2,
        'Quyền tham gia tất cả các hội trường chính, nhận bộ Kit quà tặng từ nhà tài trợ và tiếp cận khu vực tuyển dụng.'
    ),
    (
        'Vé VIP (Tech Lead / Manager)',
        1200000.00,
        100,
        45,
        'AVAILABLE',
        '2026-05-01 00:00:00',
        '2026-09-09 23:59:59',
        2,
        'Gồm toàn bộ quyền lợi vé Standard, cộng thêm quyền vào phòng chờ VIP, ăn trưa buffet cao cấp và tham gia buổi tiệc Networking giới hạn với diễn giả.'
    ),
    (
        'Vé Early Bird (Vé sớm)',
        200000.00,
        150,
        150,
        'SOLD_OUT',
        '2026-04-01 00:00:00',
        '2026-04-30 23:59:59',
        2,
        'Hạng vé ưu đãi mở bán sớm dành cho những người đăng ký đầu tiên. Đã bán hết.'
    );

-- =========================================================================
-- 2. SEED TICKET_TYPES CHO EVENT ID = 3 (Sài Gòn Tếu Live)
-- =========================================================================
INSERT INTO ticket_types (title, price, total_quantity, sold_quantity, status, start_time, end_time, event_id, description)
VALUES
    (
        'Ghế Hàng Đầu (Cận Cảnh)',
        450000.00,
        50,
        50,
        'SOLD_OUT',
        '2026-06-01 00:00:00',
        '2026-07-19 23:59:59',
        3,
        'Hàng ghế VIP sát sân khấu, cơ hội cực cao bị các diễn viên hài "tương tác" và "cà khịa" trực tiếp trong show.'
    ),
    (
        'Ghế Tiêu Chuẩn',
        250000.00,
        200,
        88,
        'AVAILABLE',
        '2026-06-01 00:00:00',
        '2026-07-19 23:59:59',
        3,
        'Khu vực ghế ngồi bao quát toàn bộ sân khấu, âm thanh rõ ràng, đảm bảo cười thả ga.'
    );

-- =========================================================================
-- 3. SEED TICKET_TYPES CHO EVENT ID = 4 (Ravolution Music Festival)
-- =========================================================================
INSERT INTO ticket_types (title, price, total_quantity, sold_quantity, status, start_time, end_time, event_id, description)
VALUES
    (
        'Vé GA - Mở Bán Đợt 1',
        890000.00,
        1500,
        1500,
        'SOLD_OUT',
        '2026-07-01 00:00:00',
        '2026-07-31 23:59:59',
        4,
        'Vé phổ thông đợt đầu tiên với mức giá ưu đãi dành cho các Raver chân chính.'
    ),
    (
        'Vé GA - Mở Bán Đợt 2',
        1200000.00,
        2000,
        650,
        'AVAILABLE',
        '2026-08-01 00:00:00',
        '2026-10-04 23:59:59',
        4,
        'Vé phổ thông đợt chính thức, số lượng có hạn. Nhanh tay đặt ngay để không bỏ lỡ bữa tiệc âm nhạc điện tử lớn nhất năm.'
    ),
    (
        'Vé SVIP Thượng Uyển',
        4500000.00,
        80,
        22,
        'AVAILABLE',
        '2026-07-01 00:00:00',
        '2026-10-04 23:59:59',
        4,
        'Khu vực sàn nâng VIP ngắm trọn vẹn sân khấu, quầy Bar phục vụ Free-flow đồ uống cao cấp và có lối đi ưu tiên riêng.'
    );

-- =========================================================================
-- 4. SEED TICKET_TYPES CHO EVENT ID = 5 (VnExpress Marathon)
-- =========================================================================
INSERT INTO ticket_types (title, price, total_quantity, sold_quantity, status, start_time, end_time, event_id, description)
VALUES
    (
        'Cự ly 5KM / 10KM',
        550000.00,
        1000,
        420,
        'AVAILABLE',
        '2026-08-01 00:00:00',
        '2026-11-15 23:59:59',
        5,
        'Trọn bộ Race Kit (Áo đấu, BIB chạy, Huy chương hoàn thành và quà tặng từ các nhà tài trợ).'
    ),
    (
        'Cự ly 21KM / 42KM (Full Marathon)',
        950000.00,
        800,
        310,
        'AVAILABLE',
        '2026-08-01 00:00:00',
        '2026-11-15 23:59:59',
        5,
        'Dành cho các runner chuyên nghiệp thách thức giới hạn bản thân. Bao gồm bảo hiểm và dịch vụ y tế, tiếp tế chuyên sâu dọc cung đường chạy.'
    );

-- =========================================================================
-- 5. SEED TICKET_TYPES CHO EVENT ID = 6 (Workshop Pha Chế Cà Phê)
-- =========================================================================
INSERT INTO ticket_types (title, price, total_quantity, sold_quantity, status, start_time, end_time, event_id, description)
VALUES
    (
        'Vé Trải Nghiệm Thường',
        300000.00,
        30,
        15,
        'AVAILABLE',
        '2026-06-15 00:00:00',
        '2026-08-04 23:59:59',
        6,
        'Tham dự lắng nghe chia sẻ kiến thức lý thuyết về hạt Specialty Coffee và thưởng thức 3 loại cà phê do Barista biểu diễn.'
    ),
    (
        'Vé Thực Hành Premium',
        650000.00,
        10,
        10,
        'SOLD_OUT',
        '2026-06-15 00:00:00',
        '2026-08-04 23:59:59',
        6,
        'Chỉ giới hạn 10 người. Được trực tiếp đứng quầy sử dụng máy pha hiện đại và bộ dụng cụ Pour-over cao cấp dưới sự hướng dẫn 1 kèm 1.'
    );

-- =========================================================================
-- 6. SEED TICKET_TYPES CHO EVENT ID = 7 (Ngày Xửa Ngày Xưa 36)
-- =========================================================================
INSERT INTO ticket_types (title, price, total_quantity, sold_quantity, status, start_time, end_time, event_id, description)
VALUES
    (
        'Lầu A (Khán đài tầng 1)',
        400000.00,
        300,
        300,
        'SOLD_OUT',
        '2026-05-01 00:00:00',
        '2026-06-30 23:59:59',
        7,
        'Vé khu vực tầng trệt, nhìn rõ nét biểu cảm của các nghệ sĩ Thành Lộc, Hữu Châu... Đã hết sạch vé sau 5 phút mở bán.'
    ),
    (
        'Lầu B (Khán đài tầng 2)',
        250000.00,
        400,
        382,
        'AVAILABLE',
        '2026-05-01 00:00:00',
        '2026-06-30 23:59:59',
        7,
        'Khu vực lầu bệ trên cao, góc nhìn bao quát toàn bộ hiệu ứng ánh sáng hoành tráng của vở kịch.'
    );

-- =========================================================================
-- 7. SEED TICKET_TYPES CHO EVENT ID = 8 (Triển Lãm Nghệ Thuật Đương Đại)
-- =========================================================================
INSERT INTO ticket_types (title, price, total_quantity, sold_quantity, status, start_time, end_time, event_id, description)
VALUES
    (
        'Vé Vào Cổng Tự Do',
        800000.00,
        2000,
        450,
        'AVAILABLE',
        '2026-08-01 00:00:00',
        '2026-09-24 23:59:59',
        8,
        'Vé tham quan trọn gói tất cả các gian trưng bày nghệ thuật tranh vẽ và điêu khắc nghệ thuật trong ngày.'
    );

-- =========================================================================
-- 8. SEED TICKET_TYPES CHO EVENT ID = 9 (Chung Kết Đấu Trường Danh Vọng)
-- =========================================================================
INSERT INTO ticket_types (title, price, total_quantity, sold_quantity, status, start_time, end_time, event_id, description)
VALUES
    (
        'Hạng vé Challenger (Sát Sân Khấu)',
        500000.00,
        150,
        150,
        'SOLD_OUT',
        '2026-09-01 00:00:00',
        '2026-10-29 23:59:59',
        9,
        'Khu vực ghế ngồi sát vách kính thi đấu của các tuyển thủ, tặng kèm Giftcode trang phục giới hạn và áo đấu Esports.'
    ),
    (
        'Hạng vé Diamond (Khán Đài)',
        200000.00,
        800,
        512,
        'AVAILABLE',
        '2026-09-01 00:00:00',
        '2026-10-29 23:59:59',
        9,
        'Khu vực khán đài cổ vũ cuồng nhiệt, màn hình LED siêu lớn truyền hình trực tiếp trận đấu rõ nét.'
    );

-- =========================================================================
-- 9. SEED TICKET_TYPES CHO EVENT ID = 10 (Sự kiện ĐÃ HỦY - Fan Meeting)
-- =========================================================================
INSERT INTO ticket_types (title, price, total_quantity, sold_quantity, status, start_time, end_time, event_id, description)
VALUES
    (
        'Vé Giao Lưu Toàn Diện',
        3000000.00,
        100,
        0,
        'AVAILABLE',
        '2026-11-01 00:00:00',
        '2026-12-09 23:59:59',
        10,
        'Hạng vé giao lưu và ký tặng trực tiếp cùng các huyền thoại bóng đá Ngoại Hạng Anh.'
    );