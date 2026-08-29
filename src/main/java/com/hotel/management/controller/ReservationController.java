package com.hotel.management.controller;

import com.hotel.management.dto.ReservationCreateDto;
import com.hotel.management.dto.ReservationDto;
import com.hotel.management.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Gestion des reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Creer une nouvelle reservation")
    public ResponseEntity<ReservationDto> create(@Valid @RequestBody ReservationCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer une reservation par id")
    public ResponseEntity<ReservationDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Lister toutes les reservations, avec filtre optionnel par client")
    public ResponseEntity<List<ReservationDto>> getAll(@RequestParam(required = false) Long clientId) {
        List<ReservationDto> reservations = (clientId != null)
                ? reservationService.getByClient(clientId)
                : reservationService.getAll();
        return ResponseEntity.ok(reservations);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Annuler une reservation")
    public ResponseEntity<ReservationDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancel(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une reservation")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
