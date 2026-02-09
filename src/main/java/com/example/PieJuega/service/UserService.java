package com.example.PieJuega.service;

import com.example.PieJuega.dto.request.ChangePasswordRequestDTO;
import com.example.PieJuega.dto.response.UserResponseDTO;
import com.example.PieJuega.dto.request.UserUpdateRequestDTO;
import com.example.PieJuega.exception.InvalidCredentialsException;
import com.example.PieJuega.exception.ResourceAlreadyExistsException;
import com.example.PieJuega.mapper.UserMapper;
import com.example.PieJuega.model.Role;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.RoleRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.util.AuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDTO getProfile(Long userId) {
        User user = getUserOrThrow(userId);
        return UserMapper.toDTO(user);
    }

    @Transactional
    public User registerUser(String username, String email, String password, String phone, LocalDate dateBirth, boolean admin) {

        if (userRepository.findByEmail(email).isPresent())
            throw new ResourceAlreadyExistsException("Email already exists");
        if (userRepository.findByPhone(phone).isPresent())
            throw new ResourceAlreadyExistsException("phone already exists");

        User user = User.builder()
                .username(username)
                .email(email)
                .phone(phone)
                .dateBirth(dateBirth)
                .authProvider(AuthProvider.LOCAL) //
                .password(passwordEncoder.encode(password))
                .build();

        Set<Role> roles = new HashSet<>();
        roleRepository.findByName("ROLE_USER").ifPresent(roles::add);

        if (admin)
            roleRepository.findByName("ROLE_ADMIN").ifPresent(roles::add);

        user.setRoles(roles);

        return userRepository.save(user);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }



    @Transactional
    public UserResponseDTO updateProfile(Long userId, UserUpdateRequestDTO request) {

        User user = getUserOrThrow(userId);

        validateEmailUpdate(userId, request.getEmail());
        validatePhoneUpdate(userId, request.getPhone());

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        if (request.getPhone() != null ) {
            user.setPhone(request.getPhone());
        }

        if (request.getDateBirth() != null) {
            user.setDateBirth(request.getDateBirth());
        }

        if (request.getPhotoUrl() != null) {
            user.setPhotoUrl(request.getPhotoUrl());
        }

        return UserMapper.toDTO(userRepository.save(user));
    }

    private void validateEmailUpdate(Long userId, String newEmail) {

        if (newEmail == null || newEmail.isBlank()) {
            return;
        }

        userRepository.findByEmail(newEmail)
                .filter(u -> !u.getId().equals(userId))
                .ifPresent(u -> {
                    throw new IllegalStateException("El correo electrónico ya está en uso");
                });
    }


    private void validatePhoneUpdate(Long userId, String newPhone) {

        if (newPhone == null || newPhone.isBlank()) {
            return;
        }

        userRepository.findByPhone(newPhone)
                .filter(u -> !u.getId().equals(userId))
                .ifPresent(u -> {
                    throw new IllegalStateException("El número de teléfono ya está en uso");
                });
    }


    public void changePassword(Long userId, ChangePasswordRequestDTO request) {
        User user = getUserOrThrow(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Contraseña actual incorrecta");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new InvalidCredentialsException("Las contraseñas no coinciden");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }



}
