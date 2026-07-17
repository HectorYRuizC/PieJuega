package com.example.PieJuega.repository;

import com.example.PieJuega.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);

    @Query("""
        SELECT u FROM User u
        WHERE u.email = :identifier
           OR u.phone = :identifier
    """)
    Optional<User> findByIdentifier(String identifier);

    @Query("""
        SELECT u FROM User u
        WHERE u.id <> :currentUserId
          AND (
            :query = ''
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY u.username ASC
    """)
    List<User> searchPlayers(Long currentUserId, String query, Pageable pageable);
}
