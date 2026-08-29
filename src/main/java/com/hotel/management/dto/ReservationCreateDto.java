package com.hotel.management.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationCreateDto {

    @NotNull(message = "L'identifiant de la chambre est obligatoire")
    private Long roomId;

    @NotNull(message = "L'identifiant du client est obligatoire")
    private Long clientId;

    @NotNull(message = "La date d'arrivee est obligatoire")
    @FutureOrPresent(message = "La date d'arrivee ne peut pas etre dans le passe")
    private LocalDate checkIn;

    @NotNull(message = "La date de depart est obligatoire")
    private LocalDate checkOut;
}
