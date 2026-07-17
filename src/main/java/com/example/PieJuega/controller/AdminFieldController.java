package com.example.PieJuega.controller;

import com.example.PieJuega.dto.request.UpsertFieldRequestDTO;
import com.example.PieJuega.dto.response.FieldResponseDTO;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.service.AdminFieldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/fields")
@RequiredArgsConstructor
public class AdminFieldController {

    private final AdminFieldService fieldService;

    @GetMapping
    public List<FieldResponseDTO> fields(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) Boolean active
    ) {
        return fieldService.getFields(user.getId(), query, active);
    }

    @GetMapping("/{fieldId}")
    public FieldResponseDTO field(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long fieldId
    ) {
        return fieldService.getField(user.getId(), fieldId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FieldResponseDTO create(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody UpsertFieldRequestDTO request
    ) {
        return fieldService.createField(user.getId(), request);
    }

    @PutMapping("/{fieldId}")
    public FieldResponseDTO update(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long fieldId,
            @Valid @RequestBody UpsertFieldRequestDTO request
    ) {
        return fieldService.updateField(user.getId(), fieldId, request);
    }

    @PatchMapping("/{fieldId}/active")
    public FieldResponseDTO setActive(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long fieldId,
            @RequestBody Map<String, Boolean> request
    ) {
        Boolean active = request.get("active");
        if (active == null) {
            throw new IllegalArgumentException("El estado de la cancha es obligatorio");
        }
        return fieldService.setActive(user.getId(), fieldId, active);
    }
}
