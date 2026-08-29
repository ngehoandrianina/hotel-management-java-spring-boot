package com.hotel.management.repository;

import com.hotel.management.entity.Room;
import com.hotel.management.entity.RoomStatus;
import com.hotel.management.entity.RoomType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RoomRepository - Tests d'integration JPA (H2)")
class RoomRepositoryTest {

    @Autowired
    private RoomRepository roomRepository;

    @Test
    @DisplayName("Doit persister et retrouver une chambre par son numero")
    void shouldSaveAndFindByRoomNumber() {
        Room room = Room.builder()
                .roomNumber("205")
                .type(RoomType.SUITE)
                .pricePerNight(BigDecimal.valueOf(250))
                .status(RoomStatus.DISPONIBLE)
                .capacity(4)
                .floor(2)
                .build();

        roomRepository.save(room);

        Optional<Room> found = roomRepository.findByRoomNumber("205");

        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo(RoomType.SUITE);
    }

    @Test
    @DisplayName("existsByRoomNumber doit retourner false pour un numero inexistant")
    void shouldReturnFalseWhenRoomNumberDoesNotExist() {
        assertThat(roomRepository.existsByRoomNumber("999")).isFalse();
    }
}
