package com.nexevent.nexevent.controllers;


import com.nexevent.nexevent.domains.dto.request.*;
import com.nexevent.nexevent.domains.entities.User;
import com.nexevent.nexevent.services.UserService;
import com.nexevent.nexevent.utils.ApiMessage;
import com.nexevent.nexevent.utils.SecurityUtil;
import com.nexevent.nexevent.utils.exception.IdInvalidException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("${nexevent.api-prefix}/auth")
public class AuthController {
    private final UserService userService;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    public AuthController(UserService userService,
                          AuthenticationManagerBuilder authenticationManagerBuilder,
                          SecurityUtil securityUtil) {
        this.userService = userService;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.securityUtil = securityUtil;
    }
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    @Value("${refresh_token_endpoint}")
    public String REFRESH_TOKEN_ENDPOINT;

    @Value("${ducthien.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and sends a verification email to the provided address.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully, verification email sent"),
            @ApiResponse(responseCode = "400", description = "Invalid input or email already exists")
    })
    @ApiMessage("Tạo tài khoản thành công")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterDTO registerDTO) throws IdInvalidException {

        if(userService.existsByEmail(registerDTO.getEmail())){
            throw new IdInvalidException("User has been exists!");
        }
        User user= userService.createUser(registerDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    @PostMapping("/login")
    @ApiMessage("Đăng nhập thành công")
    @Operation(summary = "Authenticate user", description = "Logs in the user. Returns an Access Token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful or 2FA required"),
            @ApiResponse(responseCode = "400", description = "Account not activated or blocked"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody LoginDTO loginDTO,
                                             HttpServletRequest request) throws IdInvalidException {
        try {
            User user = (userService.getUserByEmail(loginDTO.getEmail()))
                    .orElseThrow(() -> new IdInvalidException("Email hoặc mật khẩu không chính xác!"));
            if (!user.isActive()){
                throw new IdInvalidException("Tài khoản chưa được kích hoạt. Vui lòng nhờ Admin kích hoạt lại");
            }

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword());
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);


            SecurityContextHolder.getContext().setAuthentication(authentication);

            var res = new ResLoginDTO();


            res.setUserLogin(new ResLoginDTO.UserLogin(user.getId(), user.getEmail(), user.getFullname()));

            String accessToken = this.securityUtil.createToken(authentication, res);
            res.setAccessToken(accessToken);


            String refreshToken = this.securityUtil.createRefreshToken(user.getEmail(), res);

            ResponseCookie resCookies = ResponseCookie
                    .from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .path(REFRESH_TOKEN_ENDPOINT)
                    .maxAge(Duration.ofSeconds(refreshTokenExpiration))
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                    .body(res);

        } catch (BadCredentialsException e) {
            throw new IdInvalidException("Email hoặc mật khẩu không chính xác!");
        }
    }
    @PostMapping("/password/change")
    @ApiMessage("Đổi mật khẩu thành công")
    @Operation(summary = "Change password", description = "Đổi mật khẩu mới (Quên mật khẩu)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đổi mật khẩu thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ (Ví dụ: Mật khẩu mới quá yếu, mã xác nhận/Token bị sai hoặc đã hết hạn)"),
    })
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordDTO changePasswordDTO){
        String email = SecurityUtil.getCurrentUserEmail();
        if (userService.changePassword(changePasswordDTO, email)){
            return ResponseEntity.ok().body("Password Changed Successfully!");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @PostMapping("/refresh")
    @ApiMessage("Làm mới token thành công")
    @Operation(summary = "Refresh access token", description = "Cấp lại Access Token mới dựa vào Refresh Token đính kèm trong Cookie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Làm mới token thành công"),
            @ApiResponse(responseCode = "400", description = "Token không hợp lệ hoặc đã hết hạn")
    })
    public ResponseEntity<ResLoginDTO> refreshToken(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, defaultValue = "") String refreshToken) throws IdInvalidException {
        if (refreshToken.isEmpty()) {
            throw new IdInvalidException("Không tìm thấy Refresh Token trong Cookie. Vui lòng đăng nhập lại!");
        }

        try {
            Jwt decodedToken = securityUtil.checkValidRefreshToken(refreshToken);
            String email = decodedToken.getSubject();
            User user = userService.getUserByEmail(email)
                    .orElseThrow(() -> new IdInvalidException("Tài khoản không tồn tại!"));

            if (!user.isActive()){
                throw new IdInvalidException("Tài khoản đã bị khóa!");
            }
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority(user.getRole().name())
            );
            Authentication authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);

            var res = new ResLoginDTO();
            res.setUserLogin(new ResLoginDTO.UserLogin(user.getId(), user.getEmail(), user.getFullname()));

            String newAccessToken = this.securityUtil.createToken(authentication, res);
            res.setAccessToken(newAccessToken);

            String newRefreshToken = this.securityUtil.createRefreshToken(user.getEmail(), res);

            ResponseCookie resCookies = ResponseCookie
                    .from(REFRESH_TOKEN_COOKIE_NAME, newRefreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .path(REFRESH_TOKEN_ENDPOINT)
                    .maxAge(Duration.ofSeconds(refreshTokenExpiration))
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                    .body(res);

        } catch (Exception e) {
            throw new IdInvalidException("Refresh Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại!");
        }
    }
    @PostMapping("/password/forgot-password")
    @ApiMessage("Yêu cầu gửi OTP thành công")
    @Operation(summary = "Forgot password", description = "Gửi mã OTP 6 số về email của người dùng")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mã OTP đã được gửi thành công"),
            @ApiResponse(responseCode = "400", description = "Email không hợp lệ hoặc không tồn tại trong hệ thống")
    })
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordReqDTO dto) throws IdInvalidException {
        userService.processForgotPassword(dto.getEmail());
        return ResponseEntity.ok().body("Mã OTP đã được gửi đến email của bạn.");
    }

    @PostMapping("/password/reset-password")
    @ApiMessage("Đặt lại mật khẩu thành công")
    @Operation(summary = "Reset password", description = "Xác nhận OTP và tiến hành đặt lại mật khẩu mới")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mật khẩu được đổi thành công"),
            @ApiResponse(responseCode = "400", description = "OTP sai, hết hạn, hoặc nhập sai quá nhiều lần")
    })
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordReqDTO dto) throws IdInvalidException {
        userService.processResetPassword(dto);
        return ResponseEntity.ok().body("Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại.");
    }
}
