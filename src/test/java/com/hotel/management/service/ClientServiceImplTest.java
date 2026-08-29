package com.hotel.management.service;

import com.hotel.management.dto.ClientDto;
import com.hotel.management.entity.Client;
import com.hotel.management.exception.DuplicateResourceException;
import com.hotel.management.exception.ResourceNotFoundException;
import com.hotel.management.mapper.ClientMapper;
import com.hotel.management.repository.ClientRepository;
import com.hotel.management.service.impl.ClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientServiceImpl - Tests unitaires")
class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ClientMapper clientMapper;
    @InjectMocks
    private ClientServiceImpl clientService;

    private Client client;
    private ClientDto clientDto;

    @BeforeEach
    void setUp() {
        client = Client.builder().id(1L).firstName("Jean").lastName("Rakoto").email("jean@mail.com").build();
        clientDto = ClientDto.builder().id(1L).firstName("Jean").lastName("Rakoto").email("jean@mail.com").build();
    }

    @Test
    @DisplayName("Doit creer un client quand l'email n'existe pas deja")
    void shouldCreateClientSuccessfully() {
        when(clientRepository.existsByEmail("jean@mail.com")).thenReturn(false);
        when(clientMapper.toEntity(clientDto)).thenReturn(client);
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(clientMapper.toDto(client)).thenReturn(clientDto);

        ClientDto result = clientService.create(clientDto);

        assertThat(result.getEmail()).isEqualTo("jean@mail.com");
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    @DisplayName("Doit lever une exception si l'email existe deja")
    void shouldThrowWhenEmailAlreadyExists() {
        when(clientRepository.existsByEmail("jean@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> clientService.create(clientDto))
                .isInstanceOf(DuplicateResourceException.class);

        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Doit lever une exception si le client n'existe pas")
    void shouldThrowWhenClientNotFound() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Doit supprimer un client existant")
    void shouldDeleteExistingClient() {
        when(clientRepository.existsById(1L)).thenReturn(true);

        clientService.delete(1L);

        verify(clientRepository).deleteById(1L);
    }
}
