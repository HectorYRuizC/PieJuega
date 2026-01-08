package com.example.PieJuega.controller;

import com.example.PieJuega.dto.UserRegisterDTO;
import com.example.PieJuega.model.User;
import com.example.PieJuega.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody  UserRegisterDTO dto) {
        try {
            User user = userService.registerUser(
                    dto.getUsername(),
                    dto.getEmail(),
                    dto.getPassword(),
                    dto.isAdmin()
            );
            return ResponseEntity.ok("Usuario registrado con ID: " + user.getId());
        }catch (RuntimeException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
