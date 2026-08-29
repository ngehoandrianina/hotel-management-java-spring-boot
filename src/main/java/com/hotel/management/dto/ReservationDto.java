package com.hotel.management.dto;

import com.hotel.management.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationDto {
    private Long id;
    private Long roomId;
    private String roomNumber;
    private Long clientId;
    private String clientFullName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private ReservationStatus status;
}
