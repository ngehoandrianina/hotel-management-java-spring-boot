package com.hotel.management.service;

import com.hotel.management.dto.RoomCreateDto;
import com.hotel.management.dto.RoomDto;
import com.hotel.management.entity.RoomStatus;

import java.util.List;

public interface RoomService {
    RoomDto create(RoomCreateDto dto);
    RoomDto getById(Long id);
    List<RoomDto> getAll();
    List<RoomDto> getByStatus(RoomStatus status);
    RoomDto update(Long id, RoomCreateDto dto);
    RoomDto updateStatus(Long id, RoomStatus status);
    void delete(Long id);
}
