package com.immortals.authapp.service.user;

import com.immortals.authapp.annotation.ReadOnly;
import com.immortals.authapp.annotation.WriteOnly;
import com.immortals.platform.domain.dto.StateDTO;
import com.immortals.platform.domain.entity.Country;
import com.immortals.platform.domain.entity.States;
import com.immortals.platform.domain.enums.UserTypes;
import com.immortals.authapp.repository.CountryRepository;
import com.immortals.authapp.repository.StateRepository;
import com.immortals.authapp.service.exception.ResourceNotFoundException;
import com.immortals.authapp.utils.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StateServiceImpl implements StateService {

    private final StateRepository stateRepo;
    private final CountryRepository countryRepo;

    @ReadOnly
    @Override
    public List<StateDTO> getAll() {
        return stateRepo.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @ReadOnly
    @Override
    public StateDTO getById(Long id) {
        States state = stateRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("State", id));
        return toDto(state);
    }

    @WriteOnly
    @Transactional
    @Override
    public StateDTO create(StateDTO dto) {
        try {
            log.info("Creating state: {}", dto.name());

            Country country = countryRepo.findById(dto.countryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Country", dto.countryId()));

            States state = toEntity(dto);
            state.setCountry(country);

            return toDto(stateRepo.save(state));
        } catch (Exception ex) {
            log.error("Failed to create state: {}", dto.name(), ex);
            throw new RuntimeException("Failed to create state: " + dto.name(), ex);
        }
    }

    @WriteOnly
    @Transactional
    @Override
    public StateDTO update(Long id, StateDTO dto) {
        try {
            States state = stateRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("State", id));

            Country country = countryRepo.findById(dto.countryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Country", dto.countryId()));

            state.setName(dto.name());
            state.setCode(dto.code());
            state.setActiveInd(dto.activeInd());
            state.setCountry(country);

            return toDto(stateRepo.save(state));
        } catch (ResourceNotFoundException ex) {
            log.warn("Update failed: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to update state with ID: {}", id, ex);
            throw new RuntimeException("Failed to update state with ID: " + id, ex);
        }
    }

    @WriteOnly
    @Transactional
    @Override
    public void delete(Long id) {
        try {
            if (!stateRepo.existsById(id)) {
                throw new ResourceNotFoundException("State", id);
            }
            stateRepo.deleteById(id);
            log.info("Deleted state with ID: {}", id);
        } catch (ResourceNotFoundException ex) {
            log.warn("Delete failed: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to delete state with ID: {}", id, ex);
            throw new RuntimeException("Failed to delete state with ID: " + id, ex);
        }
    }

    // --- Mapper methods ---

    private StateDTO toDto(States s) {
        return new StateDTO(s.getName(), s.getCode(), s.getActiveInd(), s.getCountry().getId());
    }

    public States toEntity(StateDTO dto) {
        return States.builder()
                .name(dto.name())
                .code(dto.code())
                .activeInd(dto.activeInd())
                .createdDate(DateTimeUtils.now())
                .createdBy(UserTypes.ADMIN.name())
                .build();
    }
}
