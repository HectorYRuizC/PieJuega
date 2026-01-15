package com.example.PieJuega.service;

import com.example.PieJuega.exception.InvalidCredentialsException;
import com.example.PieJuega.exception.ResourceAlreadyExistsException;
import com.example.PieJuega.model.Role;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.RoleRepository;
import com.example.PieJuega.repository.UserRepository;
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
                .password(passwordEncoder.encode(password))
                .build();

        Set<Role> roles = new HashSet<>();
        roleRepository.findByName("ROLE_USER").ifPresent(roles::add);

        if (admin)
            roleRepository.findByName("ROLE_ADMIN").ifPresent(roles::add);

        user.setRoles(roles);

        return userRepository.save(user);
    }
}
