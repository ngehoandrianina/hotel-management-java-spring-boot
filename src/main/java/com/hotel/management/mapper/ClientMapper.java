package com.hotel.management.mapper;

import com.hotel.management.dto.ClientDto;
import com.hotel.management.entity.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClientMapper {

    ClientDto toDto(Client client);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    Client toEntity(ClientDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    void updateEntityFromDto(ClientDto dto, @MappingTarget Client client);
}
