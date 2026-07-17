package com.example.PieJuega.controller;

import com.example.PieJuega.dto.request.RegisterPushDeviceRequestDTO;
import com.example.PieJuega.dto.request.UnregisterPushDeviceRequestDTO;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.service.PushDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/devices")
@RequiredArgsConstructor
public class PushDeviceController {

    private final PushDeviceService pushDeviceService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody RegisterPushDeviceRequestDTO request
    ) {
        pushDeviceService.register(
                user.getId(),
                request.token(),
                request.platform(),
                request.soundEnabled(),
                request.vibrationEnabled()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/unregister")
    public ResponseEntity<Void> unregister(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody UnregisterPushDeviceRequestDTO request
    ) {
        pushDeviceService.unregister(user.getId(), request.token());
        return ResponseEntity.noContent().build();
    }
}
