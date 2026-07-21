package com.example.PieJuega.controller;

import com.example.PieJuega.dto.response.FieldResponseDTO;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.service.FieldFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorites/fields")
@RequiredArgsConstructor
public class FieldFavoriteController {

    private final FieldFavoriteService favoriteService;

    @GetMapping
    public List<FieldResponseDTO> favorites(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        return favoriteService.getFavorites(user.getId(), latitude, longitude);
    }

    @PostMapping("/{fieldId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FieldResponseDTO add(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long fieldId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        return favoriteService.addFavorite(
                user.getId(), fieldId, latitude, longitude
        );
    }

    @DeleteMapping("/{fieldId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long fieldId
    ) {
        favoriteService.removeFavorite(user.getId(), fieldId);
    }
}
