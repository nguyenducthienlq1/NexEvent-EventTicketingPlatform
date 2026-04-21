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
public class LoginDTO {

    @NotBlank(message = "Khong duoc de trong username")
    @Email
    @Schema(example = "nguyenducthienlq1@gmail.com")
    private String email;

    @NotBlank(message = "Khong duoc de trong password")
    @Size(min = 8, message = "Mat khau qua ngan, it nhat 8 ki tu tro len")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*$",
            message = "password phai co chu so, chu thuong va chu hoa")
    @Schema(example = "Ducthienlq1")
    private String password;
}