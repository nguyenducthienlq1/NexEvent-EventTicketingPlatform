package com.nexevent.nexevent.services;

import com.nexevent.nexevent.domains.dto.request.ChangePasswordDTO;
import com.nexevent.nexevent.domains.dto.request.RegisterDTO;
import com.nexevent.nexevent.domains.entities.User;
import com.nexevent.nexevent.domains.enums.Role;
import com.nexevent.nexevent.repositories.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public  UserService( UserRepository userRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        // 1. Kiểm tra User tồn tại
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
}
