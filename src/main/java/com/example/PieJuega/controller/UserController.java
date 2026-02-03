package com.example.PieJuega.controller;

import com.example.PieJuega.dto.ChangePasswordRequestDTO;
import com.example.PieJuega.dto.UserResponseDTO;
import com.example.PieJuega.dto.UserUpdateRequestDTO;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(userService.getProfile(userDetails.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody UserUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(
                userService.updateProfile(userDetails.getId(), request)
        );
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody ChangePasswordRequestDTO request
    ) {
        userService.changePassword(userDetails.getId(), request);
        return ResponseEntity.noContent().build();
    }
}
