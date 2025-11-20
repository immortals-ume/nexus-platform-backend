package com.immortals.authapp.service.user;

import com.immortals.platform.domain.dto.StateDTO;
import com.immortals.platform.domain.entity.States;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface StateService {
    List<StateDTO> getAll();

    StateDTO getById(Long id);

    @Transactional
    StateDTO create(StateDTO dto);

    @Transactional
    StateDTO update(Long id, StateDTO dto);

    @Transactional
    void delete(Long id);

    States toEntity(StateDTO dto);
}
