package com.hotel.management.mapper;

import com.hotel.management.dto.ReservationDto;
import com.hotel.management.entity.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReservationMapper {

    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomNumber", source = "room.roomNumber")
    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientFullName", expression = "java(reservation.getClient().getFirstName() + \" \" + reservation.getClient().getLastName())")
    ReservationDto toDto(Reservation reservation);
}
