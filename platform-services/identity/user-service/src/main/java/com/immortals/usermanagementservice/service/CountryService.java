package com.immortals.usermanagementservice.service;

import com.immortals.usermanagementservice.model.dto.CountryDTO;
import com.immortals.usermanagementservice.model.entity.Country;

import java.util.List;
import java.util.Set;

public interface CountryService {
    String create(Set<CountryDTO> dto);
    List<CountryDTO> getAll();
    CountryDTO getById(Long id);

    CountryDTO update(Long id, CountryDTO dto);

    void delete(Long id);

    Country toEntity(CountryDTO dto);

}
