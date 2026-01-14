package com.example.PieJuega.repository;

import com.example.PieJuega.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);

    @Query("""
        SELECT u FROM User u
        WHERE u.email = :identifier
           OR u.phone = :identifier
    """)
    Optional<User> findByIdentifier(String identifier);
}
