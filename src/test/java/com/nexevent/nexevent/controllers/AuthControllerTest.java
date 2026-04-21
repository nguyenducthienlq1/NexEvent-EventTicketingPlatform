package com.nexevent.nexevent.controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexevent.nexevent.BaseIntegrationTest;
import com.nexevent.nexevent.domains.dto.request.LoginDTO;
import com.nexevent.nexevent.domains.dto.request.RegisterDTO;
import com.nexevent.nexevent.domains.entities.User;
import com.nexevent.nexevent.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Kế thừa BaseIntegrationTest để tự động gọi PostgreSQL từ Docker lên
public class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc; // Công cụ đóng giả Postman

    private ObjectMapper objectMapper = new ObjectMapper();// Công cụ biến Object Java (DTO) thành chuỗi JSON

    @Autowired
    private UserRepository userRepository; // Dùng để soi Database xem data có vào thật không

    @Autowired
    private PasswordEncoder passwordEncoder; // Dùng để băm mật khẩu lúc tạo data test


    // --- BƯỚC CHUẨN BỊ MÔI TRƯỜNG ---
    // @BeforeEach sẽ chạy TRƯỚC MỖI hàm @Test.
    // Việc này giúp dọn dẹp sạch Database ảo, đảm bảo các bài test không bị "đá" data của nhau.
    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }


    // ==========================================
    // 1. CÁC KỊCH BẢN TEST CHO LUỒNG REGISTER
    // ==========================================

    @Test
    @DisplayName("Đăng ký thành công: Lưu user mới vào Database")
    void testRegister_Success() throws Exception {
        // 1. Arrange (Chuẩn bị data gửi lên)
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setEmail("thien.new@gmail.com");
        registerDTO.setPassword("Thien123!@#");
        registerDTO.setFullname("Nguyen Duc Thien");
        registerDTO.setPhone("0965620068");

        // 2. Act (Bắn API) & 3. Assert (Kiểm tra HTTP Response)
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))

                // Kỳ vọng API trả về mã 201 (Created)
                .andExpect(status().isCreated())

                // Kỳ vọng body trả về có chứa email vừa đăng ký
                .andExpect(jsonPath("$.data.email").value("thien.new@gmail.com"));

        // 4. Assert Data (Kiểm tra tận đáy Database)
        boolean isExist = userRepository.existsByEmail("thien.new@gmail.com");
        assertTrue(isExist, "Lỗi: User đăng ký xong nhưng không có mặt trong Database!");
    }

    @Test
    @DisplayName("Đăng ký thất bại: Báo lỗi 400 nếu Email đã bị trùng")
    void testRegister_Fail_DuplicateEmail() throws Exception {
        // 1. Arrange: Gài bẫy bằng cách nhét sẵn 1 user vào DB ảo trước
        User existingUser = new User();
        existingUser.setEmail("duplicate@gmail.com");
        existingUser.setPassword("SomePass123");
        existingUser.setFullname("Old User");
        existingUser.setPhone("0965620068");
        existingUser.setActive(true);
        userRepository.save(existingUser); // Lưu thẳng xuống PostgreSQL ảo

        // Chuẩn bị DTO với email CỐ TÌNH bị trùng
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setEmail("duplicate@gmail.com");
        registerDTO.setPassword("NewPass123!@#");
        registerDTO.setFullname("New User");
        registerDTO.setPhone("0965620068");

        // 2. Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "short",           // Quá ngắn (< 8 ký tự)
            "alllowercase123", // Không có chữ hoa
            "ALLUPPERCASE123", // Không có chữ thường
            "NoNumberHere!!",  // Không có số
            "Thien 123!@#",    // Chứa khoảng trắng
            "Thien123😊😊"       // Chứa Icon/Emoji
    })
    @DisplayName("Đăng ký thất bại: Password vi phạm các quy tắc bảo mật")
    void testRegister_Fail_InvalidPassword(String invalidPassword) throws Exception {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setEmail("test.valid@gmail.com");
        registerDTO.setFullname("Nguyen Duc Thien");
        registerDTO.setPhone("0965620068");
        registerDTO.setPassword(invalidPassword);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({
            "khong-phai-email, Thien123!@#, Nguyen Duc Thien, 0965620068", // Sai định dạng email
            "test@gmail.com, Thien123!@#, '', 0965620068",                 // Bỏ trống Fullname
            "test@gmail.com, Thien123!@#, Nguyen Duc Thien, ''"            // Bỏ trống Số điện thoại
    })
    @DisplayName("Đăng ký thất bại: Bỏ trống trường dữ liệu hoặc sai định dạng Email")
    void testRegister_Fail_Validation(String email, String password, String fullname, String phone) throws Exception {

        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setEmail(email);
        registerDTO.setPassword(password);
        registerDTO.setFullname(fullname);
        registerDTO.setPhone(phone);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                // Kỳ vọng lỗi 400 Bad Request
                .andExpect(status().isBadRequest());
    }


    // ==========================================
    // 2. CÁC KỊCH BẢN TEST CHO LUỒNG LOGIN
    // ==========================================

    @Test
    @DisplayName("Đăng nhập thành công: Trả về AccessToken và Cookie chứa RefreshToken")
    void testLogin_Success() throws Exception {
        // 1. Arrange: Mồi sẵn một User HỢP LỆ vào DB ảo để lát nữa đăng nhập
        User validUser = new User();
        validUser.setEmail("login.test@gmail.com");
        // LƯU Ý: Phải băm mật khẩu trước khi lưu, nếu không Spring Security sẽ không nhận dạng được lúc login
        validUser.setPassword(passwordEncoder.encode("ThienPass123!"));
        validUser.setFullname("Nguyen Duc Thien");
        validUser.setPhone("0965620068");
        validUser.setActive(true); // Tài khoản đang mở khóa
        userRepository.save(validUser);

        // Chuẩn bị payload gửi lên API
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("login.test@gmail.com");
        loginDTO.setPassword("ThienPass123!"); // Truyền mật khẩu gốc (chưa băm) để đăng nhập

        // 2. Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))

                // Kỳ vọng đăng nhập thành công (Mã 200)
                .andExpect(status().isOk())

                // Kỳ vọng Body JSON trả ra phải có trường accessToken
                .andExpect(jsonPath("$.data.access_token").exists())

                // KỲ VỌNG QUAN TRỌNG NHẤT: Phải sinh ra Cookie cho Refresh Token
                .andExpect(cookie().exists("refresh_token"))

                // Đảm bảo Cookie này đã được gắn cờ bảo mật HttpOnly
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    @DisplayName("Đăng nhập thất bại: Báo lỗi 400 nếu sai Mật khẩu")
    void testLogin_Fail_WrongPassword() throws Exception {
        // 1. Arrange: Mồi user vào DB
        User validUser = new User();
        validUser.setEmail("wrongpass@gmail.com");
        validUser.setPassword(passwordEncoder.encode("CorrectPass123!"));
        validUser.setFullname("Test User");
        validUser.setPhone("0965620068");
        validUser.setActive(true);
        userRepository.save(validUser);

        // 2. Act: Cố tình truyền sai pass
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("wrongpass@gmail.com");
        loginDTO.setPassword("SaiMatKhauRoi!");

        // 3. Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))

                // Phải báo lỗi 400
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Đăng nhập thất bại: Báo lỗi 400 nếu Email chưa được đăng ký")
    void testLogin_Fail_EmailNotFound() throws Exception {

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("not.exist@gmail.com");
        loginDTO.setPassword("SomePass123!");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Đăng nhập thất bại: Báo lỗi 400 nếu Tài khoản đang bị khóa (isActive = false)")
    void testLogin_Fail_AccountLocked() throws Exception {
        User lockedUser = new User();
        lockedUser.setEmail("locked@gmail.com");
        lockedUser.setPassword(passwordEncoder.encode("ValidPass123!"));
        lockedUser.setFullname("Locked User");
        lockedUser.setPhone("0111222333");
        lockedUser.setActive(false);
        userRepository.save(lockedUser);

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("locked@gmail.com");
        loginDTO.setPassword("ValidPass123!");

        // 2. Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                // Dù pass đúng, nhưng bị khóa thì vẫn phải trả về 400
                .andExpect(status().isBadRequest());
    }
}