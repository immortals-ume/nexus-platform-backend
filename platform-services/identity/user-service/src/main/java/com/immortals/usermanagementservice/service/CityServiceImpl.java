package com.immortals.usermanagementservice.service;


import com.immortals.usermanagementservice.model.dto.CityDTO;
import com.immortals.usermanagementservice.model.entity.City;
import com.immortals.usermanagementservice.model.entity.States;
import com.immortals.usermanagementservice.model.enums.UserTypes;
import com.immortals.usermanagementservice.repository.CityRepository;
import com.immortals.usermanagementservice.repository.StateRepository;
import com.immortals.usermanagementservice.service.exception.ResourceNotFoundException;
import com.immortals.usermanagementservice.utils.DateTimeUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;
    private final StateRepository stateRepo;

    @Transactional(isolation= Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED,rollbackFor = DataAccessException.class)
    @Override
    public CityDTO create(CityDTO dto) {
        try {
            States state = stateRepo.findByNameAndActiveIndTrue(dto.stateName())
                    .orElseThrow(() -> new ResourceNotFoundException("State", dto.stateName()));

            City city = toEntity(dto);
            city.setStates(state);

            City citySaved = cityRepository.saveAndFlush(city);
            log.info("Created city: {}", citySaved.getName());
            return toDto(citySaved);
        } catch (Exception ex) {
            log.error("Failed to create city: {}", dto.name(), ex);
            throw new RuntimeException("Failed to create city", ex);
        }
    }

    @Override
    public List<CityDTO> getAll() {
        return cityRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CityDTO getById(Long id) {
        return cityRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("City", id));
    }

    @Transactional(isolation= Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED,rollbackFor = DataAccessException.class)
    @Override
    public CityDTO update(Long id, CityDTO dto) {
        try {
            City city = cityRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("City", id));

            States state = stateRepo.findByNameAndActiveIndTrue(dto.stateName())
                    .orElseThrow(() -> new ResourceNotFoundException("State", dto.stateName()));

            city.setName(dto.name());

            city.setActiveInd(dto.activeInd());
            city.setStates(state);

            City updated = cityRepository.save(city);
            log.info("Updated city: {}", updated.getName());
            return toDto(updated);
        } catch (Exception ex) {
            log.error("Failed to update city with ID {}", id, ex);
            throw new RuntimeException("City update failed", ex);
        }
    }

    @Transactional
    @Override
    public void delete(Long id) {
        if (!cityRepository.existsById(id)) {
            throw new ResourceNotFoundException("City", id);
        }
        cityRepository.deleteById(id);
        log.info("Deleted city with ID: {}", id);
    }

    public CityDTO toDto(City city) {
        return new CityDTO(city.getName(), city.getActiveInd(), city.getStates()
                .getName());
    }

    public City toEntity(CityDTO dto) {
        return City.builder()
                .name(dto.name())
                .activeInd(dto.activeInd())
                .createdDate(DateTimeUtils.now())
                .createdBy(UserTypes.ADMIN.name())
                .build();
    }
}
