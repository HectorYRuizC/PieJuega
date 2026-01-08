package com.example.PieJuega.service;

import com.example.PieJuega.model.Role;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.RoleRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.util.RoleName;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public User registerUser(String username, String email, String password, boolean admin) {
        // Verificar si el username o email ya existen
        if (userRepository.findByUsername(username).isPresent()){
            throw new RuntimeException("Username ya existe");
        }
        if(userRepository.findByEmail(email).isPresent()){
            throw new RuntimeException("Email ya existe");
        }
        // Crear el usuario
        User user = User.builder()
                .username(username)
                .email(email)
                .password(password)
                .build();
        // Asignar roles
        Set<Role> roles = new HashSet<>();
        Optional<Role> userRole = roleRepository.findByName(RoleName.ROLE_USER.name());
        userRole.ifPresent(roles::add);

        if (admin) {
            Optional<Role> adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN.name());
            adminRole.ifPresent(roles::add);
        }

        // Guardar usuario en la DB
        user.setRoles(roles);
        return userRepository.save(user);

    }
}
