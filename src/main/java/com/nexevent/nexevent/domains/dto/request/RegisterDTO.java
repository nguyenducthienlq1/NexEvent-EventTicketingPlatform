package com.nexevent.nexevent.domains.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDTO {

    @NotBlank(message = "Khong duoc de trong UserName")
    @Email
    @Schema(example = "abcxyz@gmail.com")
    private String email;

    @NotBlank(message = "Khong duoc de trong password")
    @Size(min = 8, message = "Mat khau qua ngan, it nhat 8 ki tu tro len")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9!@#\\$%\\^&\\*]{8,}$",
            message = "Password phải có ít nhất 8 ký tự, bao gồm chữ số, chữ thường, chữ hoa và không chứa khoảng trắng hoặc ký tự đặc biệt không cho phép")
    @Schema(example = "Ducthienlq1")
    private String password;

    @NotBlank(message = "Khong duoc de trong fullname")
    @Schema(example = "Nguyen Duc Thien")
    private String fullname;

    @NotBlank(message = "Khong duoc de trong SDT")
    @Schema(example = "0965620068")
    private String phone;

//    @Schema(description = "Nhập vào vai trò của tài khoản, chỉ được ADMIN, CHECKER hoặc USER", example = "USER")
//    private Role role;
}
