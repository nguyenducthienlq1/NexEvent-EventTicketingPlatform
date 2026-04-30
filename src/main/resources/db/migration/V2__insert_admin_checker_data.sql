-- Tạo tài khoản ADMIN
INSERT INTO users (id, email, password, full_name, phone, is_active, role, created_at, updated_at)
VALUES (
           '11111111-1111-1111-1111-111111111111',
           'admin@gmail.com',
           '$2a$10$Txr3xwNdCHRHAnTSeIuszuarnd/lTv.ExbFu4mw9XH4laSS6SbUmW',
           'Nguyễn Đức Thiện (Admin)',
           '0987654321',
           true,
           'ADMIN',
           CURRENT_TIMESTAMP,
           CURRENT_TIMESTAMP
       );

-- Tạo tài khoản CHECKER (Nhân viên soát vé)
INSERT INTO users (id, email, password, full_name, phone, is_active, role, created_at, updated_at)
VALUES (
           '22222222-2222-2222-2222-222222222222',
           'checker@gmail.com',
           '$2a$10$Txr3xwNdCHRHAnTSeIuszuarnd/lTv.ExbFu4mw9XH4laSS6SbUmW', -- Hash của chữ Ducthienlq1
           'Nhân viên Soát vé 1',
           '0123456789',
           true,
           'CHECKER',
           CURRENT_TIMESTAMP,
           CURRENT_TIMESTAMP
       );