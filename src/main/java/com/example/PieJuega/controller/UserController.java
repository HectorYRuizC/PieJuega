package com.example.PieJuega.controller;

import com.example.PieJuega.dto.request.ChangePasswordRequestDTO;
import com.example.PieJuega.dto.request.UpdateLocationRequestDTO;
import com.example.PieJuega.dto.response.UserResponseDTO;
import com.example.PieJuega.dto.request.UserUpdateRequestDTO;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.service.UserService;
import jakarta.validation.Valid;
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
            @Valid @RequestBody UserUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(
                userService.updateProfile(userDetails.getId(), request)
        );
    }

    @PutMapping("/me/location")
    public ResponseEntity<UserResponseDTO> updateLocation(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateLocationRequestDTO request
    ) {
        return ResponseEntity.ok(userService.updateLocation(userDetails.getId(), request));
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
