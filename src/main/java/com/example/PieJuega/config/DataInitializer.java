package com.example.PieJuega.config;

import com.example.PieJuega.model.Role;
import com.example.PieJuega.repository.RoleRepository;
import com.example.PieJuega.util.RoleName;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            for (RoleName roleName : RoleName.values()) {
                // Si no existe en la DB, lo crea
                if (roleRepository.findByName(roleName.name()).isEmpty()) {
                    roleRepository.save(Role.builder()
                            .name(roleName.name())
                            .build());
                    System.out.println("Role creado: " + roleName.name());
                }
            }
        };
    }


}
