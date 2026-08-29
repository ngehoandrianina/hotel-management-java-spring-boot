package com.hotel.management.service.impl;

import com.hotel.management.dto.ReservationCreateDto;
import com.hotel.management.dto.ReservationDto;
import com.hotel.management.entity.Client;
import com.hotel.management.entity.Reservation;
import com.hotel.management.entity.ReservationStatus;
import com.hotel.management.entity.Room;
import com.hotel.management.entity.RoomStatus;
import com.hotel.management.exception.ResourceNotFoundException;
import com.hotel.management.exception.RoomNotAvailableException;
import com.hotel.management.mapper.ReservationMapper;
import com.hotel.management.repository.ClientRepository;
import com.hotel.management.repository.ReservationRepository;
import com.hotel.management.repository.RoomRepository;
import com.hotel.management.service.ReservationService;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final ClientRepository clientRepository;
    private final ReservationMapper reservationMapper;

    @Override
    public ReservationDto create(ReservationCreateDto dto) {
        if (!dto.getCheckOut().isAfter(dto.getCheckIn())) {
            throw new ValidationException("La date de depart doit etre posterieure a la date d'arrivee");
        }

        Room room = roomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Chambre introuvable avec l'id : " + dto.getRoomId()));

        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable avec l'id : " + dto.getClientId()));

        List<Reservation> overlaps = reservationRepository.findOverlappingReservations(
                room.getId(), dto.getCheckIn(), dto.getCheckOut());
        if (!overlaps.isEmpty()) {
            throw new RoomNotAvailableException(
                    "La chambre " + room.getRoomNumber() + " n'est pas disponible pour ces dates");
        }

        Reservation reservation = Reservation.builder()
                .room(room)
                .client(client)
                .checkIn(dto.getCheckIn())
                .checkOut(dto.getCheckOut())
                .status(ReservationStatus.CONFIRMEE)
                .build();

        room.setStatus(RoomStatus.RESERVEE);
        roomRepository.save(room);

        return reservationMapper.toDto(reservationRepository.save(reservation));
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationDto getById(Long id) {
        return reservationMapper.toDto(findReservationOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationDto> getAll() {
        return reservationRepository.findAll().stream()
                .map(reservationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationDto> getByClient(Long clientId) {
        return reservationRepository.findByClientId(clientId).stream()
                .map(reservationMapper::toDto)
                .toList();
    }

    @Override
    public ReservationDto cancel(Long id) {
        Reservation reservation = findReservationOrThrow(id);
        reservation.setStatus(ReservationStatus.ANNULEE);

        Room room = reservation.getRoom();
        room.setStatus(RoomStatus.DISPONIBLE);
        roomRepository.save(room);

        return reservationMapper.toDto(reservationRepository.save(reservation));
    }

    @Override
    public void delete(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Reservation introuvable avec l'id : " + id);
        }
        reservationRepository.deleteById(id);
    }

    private Reservation findReservationOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation introuvable avec l'id : " + id));
    }
}
