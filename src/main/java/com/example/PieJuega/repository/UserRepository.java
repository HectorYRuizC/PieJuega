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
        SELECT DISTINCT u FROM User u
        JOIN u.roles role
        WHERE role.name = 'ROLE_ADMIN'
    """)
    List<User> findAdministrators();

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
            u.cityCode = :cityCode
            OR (u.cityCode IS NULL AND LOWER(u.city) = LOWER(:cityName))
          )
          AND (
            :query = ''
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY u.username ASC
    """)
    List<User> searchPlayers(
            Long currentUserId,
            String query,
            String cityCode,
            String cityName,
            Pageable pageable
    );
}
