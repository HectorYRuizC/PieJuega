package com.example.PieJuega.controller;

import com.example.PieJuega.dto.response.FieldResponseDTO;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.service.FavoriteFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/me/favorites")
@RequiredArgsConstructor
public class FavoriteFieldController {

    private final FavoriteFieldService favoriteFieldService;

    @GetMapping
    public List<FieldResponseDTO> myFavorites(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return favoriteFieldService.listFavorites(userDetails.getId());
    }

    @GetMapping("/{fieldId}")
    public boolean isFavorite(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long fieldId
    ) {
        return favoriteFieldService.isFavorite(userDetails.getId(), fieldId);
    }

    @PutMapping("/{fieldId}")
    public ResponseEntity<Void> addFavorite(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long fieldId
    ) {
        favoriteFieldService.addFavorite(userDetails.getId(), fieldId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{fieldId}")
    public ResponseEntity<Void> removeFavorite(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long fieldId
    ) {
        favoriteFieldService.removeFavorite(userDetails.getId(), fieldId);
        return ResponseEntity.noContent().build();
    }
}
