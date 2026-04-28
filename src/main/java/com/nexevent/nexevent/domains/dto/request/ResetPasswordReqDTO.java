package com.nexevent.nexevent.domains.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordReqDTO {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Schema(example = "example@gmail.com")
    private String email;

    @NotBlank(message = "Mã OTP không được để trống")
    @Schema(example = "123456")
    private String otp;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9!@#\\$%\\^&\\*]{8,}$",
            message = "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường và số")
    @Schema(example = "Ducthienlq1")
    private String newPassword;

    @NotBlank(message = "Mật khẩu xác nhận không được để trống")
    @Schema(example = "Ducthienlq1")
    private String confirmPassword;
}
