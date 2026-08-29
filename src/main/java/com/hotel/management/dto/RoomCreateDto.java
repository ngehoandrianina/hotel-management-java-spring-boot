package com.hotel.management.dto;

import com.hotel.management.entity.RoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomCreateDto {

    @NotBlank(message = "Le numero de chambre est obligatoire")
    private String roomNumber;

    @NotNull(message = "Le type de chambre est obligatoire")
    private RoomType type;

    @NotNull(message = "Le prix par nuit est obligatoire")
    @Positive(message = "Le prix doit etre positif")
    private BigDecimal pricePerNight;

    private Integer capacity;

    private Integer floor;
}
