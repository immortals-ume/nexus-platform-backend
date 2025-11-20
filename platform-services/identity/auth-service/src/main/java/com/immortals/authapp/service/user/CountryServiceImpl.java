package com.immortals.authapp.service.user;

import com.immortals.authapp.annotation.ReadOnly;
import com.immortals.authapp.annotation.WriteOnly;
import com.immortals.platform.domain.dto.CountryDTO;
import com.immortals.platform.domain.entity.Country;
import com.immortals.platform.domain.enums.UserTypes;
import com.immortals.authapp.repository.CountryRepository;
import com.immortals.authapp.service.exception.ResourceNotFoundException;
import com.immortals.authapp.utils.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CountryServiceImpl implements CountryService {

    private final CountryRepository countryRepository;

    @ReadOnly
    @Override
    public List<CountryDTO> getAll() {
        return countryRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CountryDTO getById(Long id) {
        return countryRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Country", id));
    }

    @WriteOnly
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = {Exception.class})
    @Override
    public CountryDTO create(CountryDTO dto) {
        try {
            log.info("Creating country: {}", dto.name());
            Country entity = toEntity(dto);
            return toDto(countryRepository.saveAndFlush(entity));
        } catch (Exception ex) {
            log.error("Failed to create country: {}", dto.name(), ex);
            throw new RuntimeException("Failed to create country: " + dto.name(), ex);
        }
    }

    @WriteOnly
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = {Exception.class})
    @Override
    public CountryDTO update(Long id, CountryDTO dto) {
        try {
            Country existing = countryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("No Country Found", id));

            existing.setName(dto.name());
            existing.setCode(dto.code());
            existing.setActiveInd(Boolean.TRUE);
            existing.setUpdatedDate(DateTimeUtils.now());
            existing.setUpdatedBy(UserTypes.ADMIN.name());

            return toDto(countryRepository.save(existing));
        } catch (ResourceNotFoundException ex) {
            log.warn("Country not found with ID: {}", id);
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to update country with ID: {}", id, ex);
            throw new RuntimeException("Failed to update country with ID: " + id, ex);
        }
    }

    @WriteOnly
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = {Exception.class})
    @Override
    public void delete(Long id) {
        try {
            if (!countryRepository.existsById(id)) {
                throw new ResourceNotFoundException("Country", id);
            }
            countryRepository.deleteById(id);
            log.info("Country with ID {} deleted successfully", id);
        } catch (ResourceNotFoundException ex) {
            log.warn("Delete failed: Country not found with ID {}", id);
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to delete country with ID: {}", id, ex);
            throw new RuntimeException("Failed to delete country with ID: " + id, ex);
        }
    }

    public Country toEntity(CountryDTO dto) {
        return Country.builder()
                .name(dto.name())
                .code(dto.code())
                .createdDate(DateTimeUtils.now())
                .createdBy(UserTypes.ADMIN.name())
                .build();
    }

    public CountryDTO toDto(Country c) {
        return new CountryDTO(c.getName(), c.getCode(), c.getActiveInd());
    }
}
