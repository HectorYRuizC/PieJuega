package com.example.PieJuega.dto.response;

import com.example.PieJuega.util.PaymentMethod;
import com.example.PieJuega.util.ReservationStatus;
import com.example.PieJuega.util.TeamFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponseDTO(
        Long id,
        Long fieldId,
        String fieldName,
        TeamFormat fieldFormat,
        String fieldAddress,
        String fieldCity,
        String fieldImageUrl,
        BigDecimal totalPrice,
        Long userId,
        String userName,
        String userPhotoUrl,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String contactName,
        String contactPhone,
        PaymentMethod paymentMethod,
        String note,
        ReservationStatus status,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
