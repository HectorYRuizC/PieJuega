package com.example.PieJuega.repository;

import com.example.PieJuega.model.FieldFavorite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FieldFavoriteRepository extends JpaRepository<FieldFavorite, Long> {

    @EntityGraph(attributePaths = {"field", "field.features", "field.openDays"})
    @Query("""
        SELECT favorite FROM FieldFavorite favorite
        WHERE favorite.user.id = :userId
          AND favorite.field.active = true
        ORDER BY favorite.createdAt DESC
    """)
    List<FieldFavorite> findActiveByUserId(Long userId);

    @EntityGraph(attributePaths = {"field", "field.features", "field.openDays"})
    Optional<FieldFavorite> findByUser_IdAndField_Id(Long userId, Long fieldId);
}
