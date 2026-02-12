package com.example.PieJuega.repository;

import com.example.PieJuega.model.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    List<PasswordResetCode> findByEmailAndUsedFalseAndRevokedFalse(String email);

    Optional<PasswordResetCode> findFirstByEmailAndUsedFalseAndRevokedFalse(String email);

    Optional<PasswordResetCode> findByEmailAndCodeAndUsedFalseAndRevokedFalse(
            String email, String code
    );

    Optional<PasswordResetCode> findByTokenAndUsedFalseAndRevokedFalse(
            String token
    );


}
