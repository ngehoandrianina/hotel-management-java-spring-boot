package com.hotel.management.service;

import com.hotel.management.dto.RoomCreateDto;
import com.hotel.management.dto.RoomDto;
import com.hotel.management.entity.Room;
import com.hotel.management.entity.RoomStatus;
import com.hotel.management.entity.RoomType;
import com.hotel.management.exception.DuplicateResourceException;
import com.hotel.management.exception.ResourceNotFoundException;
import com.hotel.management.mapper.RoomMapper;
import com.hotel.management.repository.RoomRepository;
import com.hotel.management.service.impl.RoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomServiceImpl - Tests unitaires")
class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Room room;
    private RoomDto roomDto;
    private RoomCreateDto roomCreateDto;

    @BeforeEach
    void setUp() {
        room = Room.builder()
                .id(1L)
                .roomNumber("101")
                .type(RoomType.DOUBLE)
                .pricePerNight(BigDecimal.valueOf(120))
                .status(RoomStatus.DISPONIBLE)
                .capacity(2)
                .floor(1)
                .build();

        roomDto = RoomDto.builder()
                .id(1L)
                .roomNumber("101")
                .type(RoomType.DOUBLE)
                .pricePerNight(BigDecimal.valueOf(120))
                .status(RoomStatus.DISPONIBLE)
                .capacity(2)
                .floor(1)
                .build();

        roomCreateDto = RoomCreateDto.builder()
                .roomNumber("101")
                .type(RoomType.DOUBLE)
                .pricePerNight(BigDecimal.valueOf(120))
                .capacity(2)
                .floor(1)
                .build();
    }

    @Nested
    @DisplayName("Creation d'une chambre")
    class CreateRoom {

        @Test
        @DisplayName("Doit creer une chambre quand le numero n'existe pas deja")
        void shouldCreateRoomSuccessfully() {
            when(roomRepository.existsByRoomNumber("101")).thenReturn(false);
            when(roomMapper.toEntity(roomCreateDto)).thenReturn(room);
            when(roomRepository.save(any(Room.class))).thenReturn(room);
            when(roomMapper.toDto(room)).thenReturn(roomDto);

            RoomDto result = roomService.create(roomCreateDto);

            assertThat(result).isNotNull();
            assertThat(result.getRoomNumber()).isEqualTo("101");
            verify(roomRepository).save(any(Room.class));
        }

        @Test
        @DisplayName("Doit lever une exception si le numero de chambre existe deja")
        void shouldThrowWhenRoomNumberAlreadyExists() {
            when(roomRepository.existsByRoomNumber("101")).thenReturn(true);

            assertThatThrownBy(() -> roomService.create(roomCreateDto))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("101");

            verify(roomRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Recuperation d'une chambre")
    class GetRoom {

        @Test
        @DisplayName("Doit retourner la chambre quand elle existe")
        void shouldReturnRoomWhenExists() {
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
            when(roomMapper.toDto(room)).thenReturn(roomDto);

            RoomDto result = roomService.getById(1L);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Doit lever une exception quand la chambre n'existe pas")
        void shouldThrowWhenRoomNotFound() {
            when(roomRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roomService.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Doit retourner toutes les chambres")
        void shouldReturnAllRooms() {
            when(roomRepository.findAll()).thenReturn(List.of(room));
            when(roomMapper.toDto(room)).thenReturn(roomDto);

            List<RoomDto> result = roomService.getAll();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Doit filtrer les chambres par statut")
        void shouldReturnRoomsByStatus() {
            when(roomRepository.findByStatus(RoomStatus.DISPONIBLE)).thenReturn(List.of(room));
            when(roomMapper.toDto(room)).thenReturn(roomDto);

            List<RoomDto> result = roomService.getByStatus(RoomStatus.DISPONIBLE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(RoomStatus.DISPONIBLE);
        }
    }

    @Nested
    @DisplayName("Mise a jour du statut")
    class UpdateStatus {

        @Test
        @DisplayName("Doit mettre a jour le statut d'une chambre")
        void shouldUpdateRoomStatus() {
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
            when(roomRepository.save(any(Room.class))).thenReturn(room);
            when(roomMapper.toDto(room)).thenReturn(roomDto);

            roomService.updateStatus(1L, RoomStatus.EN_MAINTENANCE);

            verify(roomRepository).save(room);
            assertThat(room.getStatus()).isEqualTo(RoomStatus.EN_MAINTENANCE);
        }
    }

    @Nested
    @DisplayName("Suppression d'une chambre")
    class DeleteRoom {

        @Test
        @DisplayName("Doit supprimer une chambre existante")
        void shouldDeleteExistingRoom() {
            when(roomRepository.existsById(1L)).thenReturn(true);

            roomService.delete(1L);

            verify(roomRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Doit lever une exception si la chambre n'existe pas")
        void shouldThrowWhenDeletingNonExistentRoom() {
            when(roomRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> roomService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(roomRepository, never()).deleteById(any());
        }
    }
}
