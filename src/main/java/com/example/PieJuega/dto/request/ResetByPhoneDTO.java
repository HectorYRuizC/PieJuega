package com.example.PieJuega.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    /** ID token de Firebase emitido tras verificar el código SMS. Obligatorio. */
    @JsonAlias("firebaseIdToken")
    @NotBlank(message = "El token de verificación de Firebase es requerido")
    private String idToken;
}
