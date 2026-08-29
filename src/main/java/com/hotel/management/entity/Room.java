package com.hotel.management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(columnNames = "room_number"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank(message = "Le numero de chambre est obligatoire")
    @Column(name = "room_number", nullable = false, unique = true)
    private String roomNumber;

    @NotNull(message = "Le type de chambre est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType type;

    @NotNull(message = "Le prix par nuit est obligatoire")
    @Positive(message = "Le prix doit etre positif")
    @Column(name = "price_per_night", nullable = false)
    private BigDecimal pricePerNight;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RoomStatus status = RoomStatus.DISPONIBLE;

    private Integer capacity;

    private Integer floor;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();
}
