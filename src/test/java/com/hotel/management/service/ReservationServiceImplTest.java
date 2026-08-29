package com.hotel.management.service;

import com.hotel.management.dto.ReservationCreateDto;
import com.hotel.management.dto.ReservationDto;
import com.hotel.management.entity.*;
import com.hotel.management.exception.ResourceNotFoundException;
import com.hotel.management.exception.RoomNotAvailableException;
import com.hotel.management.mapper.ReservationMapper;
import com.hotel.management.repository.ClientRepository;
import com.hotel.management.repository.ReservationRepository;
import com.hotel.management.repository.RoomRepository;
import com.hotel.management.service.impl.ReservationServiceImpl;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationServiceImpl - Tests unitaires")
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ReservationMapper reservationMapper;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Room room;
    private Client client;
    private Reservation reservation;
    private ReservationCreateDto createDto;

    @BeforeEach
    void setUp() {
        room = Room.builder().id(1L).roomNumber("101").status(RoomStatus.DISPONIBLE).build();
        client = Client.builder().id(1L).firstName("Jean").lastName("Rakoto").email("jean@mail.com").build();

        createDto = ReservationCreateDto.builder()
                .roomId(1L)
                .clientId(1L)
                .checkIn(LocalDate.now().plusDays(1))
                .checkOut(LocalDate.now().plusDays(3))
                .build();

        reservation = Reservation.builder()
                .id(1L)
                .room(room)
                .client(client)
                .checkIn(createDto.getCheckIn())
                .checkOut(createDto.getCheckOut())
                .status(ReservationStatus.CONFIRMEE)
                .build();
    }

    @Nested
    @DisplayName("Creation d'une reservation")
    class CreateReservation {

        @Test
        @DisplayName("Doit creer une reservation quand la chambre est disponible")
        void shouldCreateReservationSuccessfully() {
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(reservationRepository.findOverlappingReservations(1L, createDto.getCheckIn(), createDto.getCheckOut()))
                    .thenReturn(List.of());
            when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
            when(reservationMapper.toDto(reservation)).thenReturn(
                    ReservationDto.builder().id(1L).roomId(1L).clientId(1L).status(ReservationStatus.CONFIRMEE).build());

            ReservationDto result = reservationService.create(createDto);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMEE);
            assertThat(room.getStatus()).isEqualTo(RoomStatus.RESERVEE);
            verify(reservationRepository).save(any(Reservation.class));
        }

        @Test
        @DisplayName("Doit rejeter si la date de depart precede la date d'arrivee")
        void shouldRejectInvalidDateRange() {
            createDto.setCheckOut(createDto.getCheckIn().minusDays(1));

            assertThatThrownBy(() -> reservationService.create(createDto))
                    .isInstanceOf(ValidationException.class);

            verifyNoInteractions(roomRepository, clientRepository, reservationRepository);
        }

        @Test
        @DisplayName("Doit lever une exception si la chambre n'existe pas")
        void shouldThrowWhenRoomNotFound() {
            when(roomRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.create(createDto))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Doit lever une exception si le client n'existe pas")
        void shouldThrowWhenClientNotFound() {
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
            when(clientRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.create(createDto))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Doit rejeter si la chambre est deja reservee sur ces dates")
        void shouldRejectWhenRoomAlreadyBooked() {
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(reservationRepository.findOverlappingReservations(1L, createDto.getCheckIn(), createDto.getCheckOut()))
                    .thenReturn(List.of(reservation));

            assertThatThrownBy(() -> reservationService.create(createDto))
                    .isInstanceOf(RoomNotAvailableException.class);

            verify(reservationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Annulation d'une reservation")
    class CancelReservation {

        @Test
        @DisplayName("Doit annuler la reservation et liberer la chambre")
        void shouldCancelReservationAndFreeRoom() {
            room.setStatus(RoomStatus.RESERVEE);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
            when(reservationMapper.toDto(reservation)).thenReturn(
                    ReservationDto.builder().id(1L).status(ReservationStatus.ANNULEE).build());

            reservationService.cancel(1L);

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ANNULEE);
            assertThat(room.getStatus()).isEqualTo(RoomStatus.DISPONIBLE);
        }

        @Test
        @DisplayName("Doit lever une exception si la reservation n'existe pas")
        void shouldThrowWhenReservationNotFound() {
            when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.cancel(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
