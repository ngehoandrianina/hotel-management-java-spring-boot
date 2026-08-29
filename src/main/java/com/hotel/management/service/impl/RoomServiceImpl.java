package com.hotel.management.service.impl;

import com.hotel.management.dto.RoomCreateDto;
import com.hotel.management.dto.RoomDto;
import com.hotel.management.entity.Room;
import com.hotel.management.entity.RoomStatus;
import com.hotel.management.exception.DuplicateResourceException;
import com.hotel.management.exception.ResourceNotFoundException;
import com.hotel.management.mapper.RoomMapper;
import com.hotel.management.repository.RoomRepository;
import com.hotel.management.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;

    @Override
    public RoomDto create(RoomCreateDto dto) {
        if (roomRepository.existsByRoomNumber(dto.getRoomNumber())) {
            throw new DuplicateResourceException(
                    "Une chambre avec le numero " + dto.getRoomNumber() + " existe deja");
        }
        Room room = roomMapper.toEntity(dto);
        room.setStatus(RoomStatus.DISPONIBLE);
        return roomMapper.toDto(roomRepository.save(room));
    }

    @Override
    @Transactional(readOnly = true)
    public RoomDto getById(Long id) {
        return roomMapper.toDto(findRoomOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDto> getAll() {
        return roomRepository.findAll().stream()
                .map(roomMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDto> getByStatus(RoomStatus status) {
        return roomRepository.findByStatus(status).stream()
                .map(roomMapper::toDto)
                .toList();
    }

    @Override
    public RoomDto update(Long id, RoomCreateDto dto) {
        Room room = findRoomOrThrow(id);

        if (!room.getRoomNumber().equals(dto.getRoomNumber())
                && roomRepository.existsByRoomNumber(dto.getRoomNumber())) {
            throw new DuplicateResourceException(
                    "Une chambre avec le numero " + dto.getRoomNumber() + " existe deja");
        }

        roomMapper.updateEntityFromDto(dto, room);
        return roomMapper.toDto(roomRepository.save(room));
    }

    @Override
    public RoomDto updateStatus(Long id, RoomStatus status) {
        Room room = findRoomOrThrow(id);
        room.setStatus(status);
        return roomMapper.toDto(roomRepository.save(room));
    }

    @Override
    public void delete(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Chambre introuvable avec l'id : " + id);
        }
        roomRepository.deleteById(id);
    }

    private Room findRoomOrThrow(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre introuvable avec l'id : " + id));
    }
}
