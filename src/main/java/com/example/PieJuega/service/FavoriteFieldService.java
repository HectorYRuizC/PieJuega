package com.example.PieJuega.service;

import com.example.PieJuega.dto.response.FieldResponseDTO;
import com.example.PieJuega.exception.ResourceNotFoundException;
import com.example.PieJuega.mapper.FieldMapper;
import com.example.PieJuega.model.FavoriteField;
import com.example.PieJuega.model.FootballField;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.FavoriteFieldRepository;
import com.example.PieJuega.repository.FootballFieldRepository;
import com.example.PieJuega.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteFieldService {

    private final FavoriteFieldRepository favoriteFieldRepository;
    private final FootballFieldRepository fieldRepository;
    private final UserRepository userRepository;
    private final FieldMapper fieldMapper;

    @Transactional(readOnly = true)
    public List<FieldResponseDTO> listFavorites(Long userId) {
        return favoriteFieldRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(FavoriteField::getField)
                .map(field -> fieldMapper.toResponse(field, null, null))
                .toList();
    }

    @Transactional
    public void addFavorite(Long userId, Long fieldId) {
        if (favoriteFieldRepository.existsByUserIdAndField_Id(userId, fieldId)) {
            return; // idempotent: favoriting twice is a no-op
        }

        FootballField field = fieldRepository.findById(fieldId)
                .filter(FootballField::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));

        User user = userRepository.getReferenceById(userId);
        favoriteFieldRepository.save(FavoriteField.builder()
                .user(user)
                .field(field)
                .build());
    }

    @Transactional
    public void removeFavorite(Long userId, Long fieldId) {
        favoriteFieldRepository.findByUserIdAndField_Id(userId, fieldId)
                .ifPresent(favoriteFieldRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long fieldId) {
        return favoriteFieldRepository.existsByUserIdAndField_Id(userId, fieldId);
    }
}
