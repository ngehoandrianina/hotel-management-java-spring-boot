package com.hotel.management.mapper;

import com.hotel.management.dto.RoomCreateDto;
import com.hotel.management.dto.RoomDto;
import com.hotel.management.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoomMapper {

    RoomDto toDto(Room room);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    Room toEntity(RoomCreateDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    void updateEntityFromDto(RoomCreateDto dto, @MappingTarget Room room);
}
