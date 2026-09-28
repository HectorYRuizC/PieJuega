package com.example.PieJuega.repository;

import com.example.PieJuega.model.FavoriteField;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteFieldRepository extends JpaRepository<FavoriteField, Long> {

    @EntityGraph(attributePaths = "field")
    List<FavoriteField> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<FavoriteField> findByUserIdAndField_Id(Long userId, Long fieldId);

    boolean existsByUserIdAndField_Id(Long userId, Long fieldId);
}
