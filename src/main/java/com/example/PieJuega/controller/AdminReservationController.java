package com.example.PieJuega.controller;

import com.example.PieJuega.dto.request.RejectReservationRequestDTO;
import com.example.PieJuega.dto.response.ReservationResponseDTO;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.service.BookingService;
import com.example.PieJuega.util.ReservationStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
public class AdminReservationController {

    private final BookingService bookingService;

    @GetMapping
    public List<ReservationResponseDTO> reservations(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(required = false) ReservationStatus status
    ) {
        return bookingService.getAdminReservations(user.getId(), status);
    }

    @GetMapping("/{reservationId}")
    public ReservationResponseDTO reservation(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long reservationId
    ) {
        return bookingService.getReservation(reservationId, user.getId());
    }

    @PatchMapping("/{reservationId}/approve")
    public ReservationResponseDTO approve(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long reservationId
    ) {
        return bookingService.approveReservation(reservationId, user.getId());
    }

    @PatchMapping("/{reservationId}/reject")
    public ReservationResponseDTO reject(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long reservationId,
            @Valid @RequestBody RejectReservationRequestDTO request
    ) {
        return bookingService.rejectReservation(reservationId, user.getId(), request.reason());
    }
}
