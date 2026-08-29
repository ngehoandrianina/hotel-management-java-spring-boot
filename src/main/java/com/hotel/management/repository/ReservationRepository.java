package com.hotel.management.repository;

import com.hotel.management.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByRoomId(Long roomId);

    List<Reservation> findByClientId(Long clientId);

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.room.id = :roomId
        AND r.status <> com.hotel.management.entity.ReservationStatus.ANNULEE
        AND r.checkIn < :checkOut
        AND r.checkOut > :checkIn
        """)
    List<Reservation> findOverlappingReservations(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut);
}
