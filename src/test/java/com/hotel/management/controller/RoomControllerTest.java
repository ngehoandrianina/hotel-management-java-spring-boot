package com.hotel.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.management.dto.RoomCreateDto;
import com.hotel.management.dto.RoomDto;
import com.hotel.management.entity.RoomStatus;
import com.hotel.management.entity.RoomType;
import com.hotel.management.exception.DuplicateResourceException;
import com.hotel.management.exception.ResourceNotFoundException;
import com.hotel.management.service.RoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
@DisplayName("RoomController - Tests d'integration MockMvc")
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoomService roomService;

    @Test
    @DisplayName("POST /api/v1/rooms doit retourner 201 avec une chambre valide")
    void shouldCreateRoomAndReturn201() throws Exception {
        RoomCreateDto createDto = RoomCreateDto.builder()
                .roomNumber("101")
                .type(RoomType.DOUBLE)
                .pricePerNight(BigDecimal.valueOf(120))
                .capacity(2)
                .floor(1)
                .build();

        RoomDto responseDto = RoomDto.builder()
                .id(1L)
                .roomNumber("101")
                .type(RoomType.DOUBLE)
                .pricePerNight(BigDecimal.valueOf(120))
                .status(RoomStatus.DISPONIBLE)
                .build();

        when(roomService.create(any(RoomCreateDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomNumber").value("101"))
                .andExpect(jsonPath("$.status").value("DISPONIBLE"));
    }

    @Test
    @DisplayName("POST /api/v1/rooms doit retourner 400 quand le payload est invalide")
    void shouldReturn400WhenPayloadInvalid() throws Exception {
        RoomCreateDto invalidDto = RoomCreateDto.builder().build();

        mockMvc.perform(post("/api/v1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/rooms doit retourner 409 en cas de doublon")
    void shouldReturn409WhenDuplicateRoomNumber() throws Exception {
        RoomCreateDto createDto = RoomCreateDto.builder()
                .roomNumber("101")
                .type(RoomType.DOUBLE)
                .pricePerNight(BigDecimal.valueOf(120))
                .build();

        when(roomService.create(any(RoomCreateDto.class)))
                .thenThrow(new DuplicateResourceException("Une chambre avec le numero 101 existe deja"));

        mockMvc.perform(post("/api/v1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/v1/rooms/{id} doit retourner 200 quand la chambre existe")
    void shouldReturnRoomWhenExists() throws Exception {
        RoomDto responseDto = RoomDto.builder().id(1L).roomNumber("101").status(RoomStatus.DISPONIBLE).build();
        when(roomService.getById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/rooms/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/rooms/{id} doit retourner 404 quand la chambre n'existe pas")
    void shouldReturn404WhenRoomNotFound() throws Exception {
        when(roomService.getById(99L)).thenThrow(new ResourceNotFoundException("Chambre introuvable avec l'id : 99"));

        mockMvc.perform(get("/api/v1/rooms/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/rooms doit filtrer par statut")
    void shouldFilterRoomsByStatus() throws Exception {
        RoomDto responseDto = RoomDto.builder().id(1L).roomNumber("101").status(RoomStatus.DISPONIBLE).build();
        when(roomService.getByStatus(eq(RoomStatus.DISPONIBLE))).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/v1/rooms").param("status", "DISPONIBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DISPONIBLE"));
    }

    @Test
    @DisplayName("DELETE /api/v1/rooms/{id} doit retourner 204")
    void shouldDeleteRoomAndReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/rooms/1"))
                .andExpect(status().isNoContent());
    }
}
