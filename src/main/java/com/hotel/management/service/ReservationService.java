package com.hotel.management.service;

import com.hotel.management.dto.ReservationCreateDto;
import com.hotel.management.dto.ReservationDto;

import java.util.List;

public interface ReservationService {
    ReservationDto create(ReservationCreateDto dto);
    ReservationDto getById(Long id);
    List<ReservationDto> getAll();
    List<ReservationDto> getByClient(Long clientId);
    ReservationDto cancel(Long id);
    void delete(Long id);
}
