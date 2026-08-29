package com.hotel.management.controller;

import com.hotel.management.dto.RoomCreateDto;
import com.hotel.management.dto.RoomDto;
import com.hotel.management.entity.RoomStatus;
import com.hotel.management.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Tag(name = "Chambres", description = "Gestion des chambres d'hotel")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @Operation(summary = "Creer une nouvelle chambre")
    public ResponseEntity<RoomDto> create(@Valid @RequestBody RoomCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer une chambre par id")
    public ResponseEntity<RoomDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Lister toutes les chambres, avec filtre optionnel par statut")
    public ResponseEntity<List<RoomDto>> getAll(@RequestParam(required = false) RoomStatus status) {
        List<RoomDto> rooms = (status != null) ? roomService.getByStatus(status) : roomService.getAll();
        return ResponseEntity.ok(rooms);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre a jour une chambre")
    public ResponseEntity<RoomDto> update(@PathVariable Long id, @Valid @RequestBody RoomCreateDto dto) {
        return ResponseEntity.ok(roomService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Changer le statut d'une chambre")
    public ResponseEntity<RoomDto> updateStatus(@PathVariable Long id, @RequestParam RoomStatus status) {
        return ResponseEntity.ok(roomService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une chambre")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
