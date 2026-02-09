package com.example.PieJuega.repository;

import com.example.PieJuega.model.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {


    Optional<PasswordResetCode> findByEmailAndCodeAndUsedFalse(
            String email,
            String code
    );

    List<PasswordResetCode> findByEmailAndUsedFalse(String email);
}
