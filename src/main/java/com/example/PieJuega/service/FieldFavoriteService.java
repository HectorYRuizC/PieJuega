package com.example.PieJuega.service;

import com.example.PieJuega.dto.response.FieldResponseDTO;
import com.example.PieJuega.exception.ResourceNotFoundException;
import com.example.PieJuega.mapper.FieldMapper;
import com.example.PieJuega.model.FieldFavorite;
import com.example.PieJuega.model.FootballField;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.FieldFavoriteRepository;
import com.example.PieJuega.repository.FootballFieldRepository;
import com.example.PieJuega.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FieldFavoriteService {

    private final FieldFavoriteRepository favoriteRepository;
    private final FootballFieldRepository fieldRepository;
    private final UserRepository userRepository;
    private final FieldMapper fieldMapper;

    @Transactional(readOnly = true)
    public List<FieldResponseDTO> getFavorites(
            Long userId,
            Double latitude,
            Double longitude
    ) {
        requireUser(userId);
        return favoriteRepository.findActiveByUserId(userId).stream()
                .map(FieldFavorite::getField)
                .map(field -> fieldMapper.toResponse(field, latitude, longitude))
                .toList();
    }

    @Transactional
    public FieldResponseDTO addFavorite(
            Long userId,
            Long fieldId,
            Double latitude,
            Double longitude
    ) {
        User user = requireUser(userId);
        var existing = favoriteRepository.findByUser_IdAndField_Id(userId, fieldId);
        if (existing.isPresent()) {
            return fieldMapper.toResponse(existing.get().getField(), latitude, longitude);
        }

        FootballField field = fieldRepository.findById(fieldId)
                .filter(FootballField::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));
        favoriteRepository.save(FieldFavorite.builder()
                .user(user)
                .field(field)
                .build());
        return fieldMapper.toResponse(field, latitude, longitude);
    }

    @Transactional
    public void removeFavorite(Long userId, Long fieldId) {
        requireUser(userId);
        favoriteRepository.findByUser_IdAndField_Id(userId, fieldId)
                .ifPresent(favoriteRepository::delete);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}
