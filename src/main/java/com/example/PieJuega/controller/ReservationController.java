package com.example.PieJuega.controller;

import com.example.PieJuega.dto.request.CreateReservationRequestDTO;
import com.example.PieJuega.dto.response.ReservationResponseDTO;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final BookingService bookingService;

    @GetMapping("/mine")
    public List<ReservationResponseDTO> mine(@AuthenticationPrincipal UserDetailsImpl user) {
        return bookingService.getMyReservations(user.getId());
    }

    @GetMapping("/{reservationId}")
    public ReservationResponseDTO reservation(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long reservationId
    ) {
        return bookingService.getReservation(reservationId, user.getId());
    }

    @PostMapping
    public ResponseEntity<ReservationResponseDTO> create(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody CreateReservationRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createReservation(user.getId(), request));
    }

    @PatchMapping("/{reservationId}/cancel")
    public ReservationResponseDTO cancel(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long reservationId
    ) {
        return bookingService.cancelReservation(reservationId, user.getId());
    }
}
