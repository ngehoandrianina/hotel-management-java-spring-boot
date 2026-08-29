package com.hotel.management.dto;

import com.hotel.management.entity.RoomStatus;
import com.hotel.management.entity.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDto {
    private Long id;
    private String roomNumber;
    private RoomType type;
    private BigDecimal pricePerNight;
    private RoomStatus status;
    private Integer capacity;
    private Integer floor;
}
