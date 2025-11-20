package com.immortals.authapp.service.user;

import com.immortals.platform.domain.dto.CountryDTO;
import com.immortals.platform.domain.entity.Country;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CountryService {
    List<CountryDTO> getAll();

    CountryDTO getById(Long id);

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = {Exception.class})
    CountryDTO create(CountryDTO dto);

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = {Exception.class})
    CountryDTO update(Long id, CountryDTO dto);

    @Transactional
    void delete(Long id);

    Country toEntity(CountryDTO dto);
}
