package com.hotel.management.service;

import com.hotel.management.dto.ClientDto;

import java.util.List;

public interface ClientService {
    ClientDto create(ClientDto dto);
    ClientDto getById(Long id);
    List<ClientDto> getAll();
    ClientDto update(Long id, ClientDto dto);
    void delete(Long id);
}
