package com.nexevent.nexevent.services;

import com.nexevent.nexevent.domains.dto.request.ChangePasswordDTO;
import com.nexevent.nexevent.domains.dto.request.RegisterDTO;
import com.nexevent.nexevent.domains.dto.request.ResetPasswordReqDTO;
import com.nexevent.nexevent.domains.entities.User;
import com.nexevent.nexevent.domains.enums.Role;
import com.nexevent.nexevent.repositories.UserRepository;

import com.nexevent.nexevent.utils.exception.IdInvalidException;

import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;

    public  UserService( UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                         RedisTemplate<String, String> redisTemplate,
                         EmailService emailService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;

    }
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    @Transactional
    public User createUser(RegisterDTO loginDTO) {
        User user = User.builder()
                .email(loginDTO.getEmail())
                .fullname(loginDTO.getFullname())
                .phone(loginDTO.getPhone())
                .password(passwordEncoder.encode(loginDTO.getPassword()))
                .isActive(true)
                .role(Role.USER)
                .build();

        this.userRepository.save(user);
        return user;
    }
    public boolean changePassword(ChangePasswordDTO changePasswordDTO, String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        if (passwordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPassword())) {
            if (changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmPassword())) {
                user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
                userRepository.save(user);
                return true;
            }
            else{
                return false;
            }
        }
        return false;
    }
    private static final long OTP_EXPIRATION_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;

    public void processForgotPassword(String email) throws IdInvalidException {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy tài khoản với Email này!"));
        String otp = String.format("%06d", new SecureRandom().nextInt(999999));

        String otpKey = "reset_pw:otp:" + email;
        String attemptKey = "reset_pw:attempts:" + email;
        redisTemplate.opsForValue().set(otpKey, otp, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(attemptKey, "0", OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        emailService.sendOtpEmail(email, otp);
    }

    @Transactional
    public void processResetPassword(ResetPasswordReqDTO dto) throws IdInvalidException {
        // 1. Check xác nhận mật khẩu
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new IdInvalidException("Mật khẩu xác nhận không trùng khớp!");
        }

        String email = dto.getEmail();
        String otpKey = "reset_pw:otp:" + email;
        String attemptKey = "reset_pw:attempts:" + email;

        // 2. Lấy OTP từ Redis
        String savedOtp = redisTemplate.opsForValue().get(otpKey);
        if (savedOtp == null) {
            throw new IdInvalidException("Mã OTP đã hết hạn hoặc không tồn tại. Vui lòng yêu cầu lại!");
        }

        // 3. Check số lần nhập sai
        String attemptsStr = redisTemplate.opsForValue().get(attemptKey);
        int attempts = (attemptsStr != null) ? Integer.parseInt(attemptsStr) : 0;
        if (attempts >= MAX_OTP_ATTEMPTS) {
            redisTemplate.delete(otpKey);
            throw new IdInvalidException("Nhập sai quá " + MAX_OTP_ATTEMPTS + " lần. OTP đã bị hủy để bảo mật!");
        }

        // 4. Đối chiếu OTP
        if (!savedOtp.equals(dto.getOtp())) {
            redisTemplate.opsForValue().increment(attemptKey);
            throw new IdInvalidException("Mã OTP không chính xác!");
        }

        // 5. Cập nhật mật khẩu mới xuống Database
        User user = userRepository.findByEmail(email).get();
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        // 6. Xóa OTP khỏi Redis
        redisTemplate.delete(otpKey);
        redisTemplate.delete(attemptKey);
    }


}
