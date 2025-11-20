package com.immortals.authapp.service.user;

import com.immortals.platform.domain.dto.CityDTO;
import com.immortals.platform.domain.entity.City;
import com.immortals.platform.domain.entity.States;
import com.immortals.platform.domain.enums.UserTypes;
import com.immortals.authapp.repository.CityRepository;
import com.immortals.authapp.repository.StateRepository;
import com.immortals.authapp.service.exception.ResourceNotFoundException;
import com.immortals.authapp.utils.DateTimeUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepo;
    private final StateRepository stateRepo;

    @Transactional(rollbackOn = Exception.class)
    @Override
    public CityDTO create(CityDTO dto) {
        try {
            States state = stateRepo.findByNameAndActiveIndTrue(dto.stateName())
                    .orElseThrow(() -> new ResourceNotFoundException("State", dto.stateName()));

            City city = toEntity(dto);
            city.setStates(state);

            City saved = cityRepo.save(city);
            log.info("Created city: {}", saved.getName());
            return toDto(saved);
        } catch (Exception ex) {
            log.error("Failed to create city: {}", dto.name(), ex);
            throw new RuntimeException("Failed to create city", ex);
        }
    }

    @Override
    public List<CityDTO> getAll() {
        return cityRepo.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CityDTO getById(Long id) {
        return cityRepo.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("City", id));
    }

    @Transactional(rollbackOn = Exception.class)
    @Override
    public CityDTO update(Long id, CityDTO dto) {
        try {
            City city = cityRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("City", id));

            States state = stateRepo.findByNameAndActiveIndTrue(dto.stateName())
                    .orElseThrow(() -> new ResourceNotFoundException("State", dto.stateName()));

            city.setName(dto.name());

            city.setActiveInd(dto.activeInd());
            city.setStates(state);

            City updated = cityRepo.save(city);
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
        if (!cityRepo.existsById(id)) {
            throw new ResourceNotFoundException("City", id);
        }
        cityRepo.deleteById(id);
        log.info("Deleted city with ID: {}", id);
    }

    // --- Mapper Methods ---
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
