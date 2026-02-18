package com.example.PieJuega.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetByPhoneDTO {
    @NotBlank
    private String phone;
    @NotBlank
    @Size(min = 8)
    private String newPassword;
    @NotBlank
    private String confirmNewPassword;
}
