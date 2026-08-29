package com.hotel.management.service.impl;

import com.hotel.management.dto.ClientDto;
import com.hotel.management.entity.Client;
import com.hotel.management.exception.DuplicateResourceException;
import com.hotel.management.exception.ResourceNotFoundException;
import com.hotel.management.mapper.ClientMapper;
import com.hotel.management.repository.ClientRepository;
import com.hotel.management.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Override
    public ClientDto create(ClientDto dto) {
        if (clientRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Un client avec l'email " + dto.getEmail() + " existe deja");
        }
        Client client = clientMapper.toEntity(dto);
        return clientMapper.toDto(clientRepository.save(client));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDto getById(Long id) {
        return clientMapper.toDto(findClientOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientDto> getAll() {
        return clientRepository.findAll().stream()
                .map(clientMapper::toDto)
                .toList();
    }

    @Override
    public ClientDto update(Long id, ClientDto dto) {
        Client client = findClientOrThrow(id);

        if (!client.getEmail().equals(dto.getEmail()) && clientRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Un client avec l'email " + dto.getEmail() + " existe deja");
        }

        clientMapper.updateEntityFromDto(dto, client);
        return clientMapper.toDto(clientRepository.save(client));
    }

    @Override
    public void delete(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Client introuvable avec l'id : " + id);
        }
        clientRepository.deleteById(id);
    }

    private Client findClientOrThrow(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable avec l'id : " + id));
    }
}
