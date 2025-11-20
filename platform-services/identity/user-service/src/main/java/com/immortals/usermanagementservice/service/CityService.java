package com.immortals.usermanagementservice.service;


import com.immortals.usermanagementservice.model.dto.CityDTO;
import com.immortals.usermanagementservice.model.entity.City;
import jakarta.transaction.Transactional;

import java.util.List;

public interface CityService {
    @Transactional(rollbackOn = Exception.class)
    CityDTO create(CityDTO dto);

    List<CityDTO> getAll();

    CityDTO getById(Long id);

    @Transactional(rollbackOn = Exception.class)
    CityDTO update(Long id, CityDTO dto);

    @Transactional
    void delete(Long id);

    CityDTO toDto(City city);

    City toEntity(CityDTO dto);
}

