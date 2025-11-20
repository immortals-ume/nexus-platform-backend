package com.immortals.usermanagementservice.service;

import com.immortals.usermanagementservice.model.dto.StateDTO;
import com.immortals.usermanagementservice.model.entity.States;


import java.util.List;

public interface StateService {
    List<StateDTO> getAll();

    StateDTO getById(Long id);


    StateDTO create(StateDTO dto);


    StateDTO update(Long id, StateDTO dto);


    void delete(Long id);

    States toEntity(StateDTO dto);
}
