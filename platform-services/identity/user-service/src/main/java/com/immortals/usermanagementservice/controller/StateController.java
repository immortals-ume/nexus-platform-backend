package com.immortals.usermanagementservice.controller;

import com.immortals.usermanagementservice.model.dto.StateDTO;
import com.immortals.usermanagementservice.service.StateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/states")
@RequiredArgsConstructor
@Validated
public class StateController {

    private final StateService stateService;

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('READ')")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<StateDTO>> getAll() {
        return ResponseEntity.ok(stateService.getAll());
    }

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('READ')")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<StateDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stateService.getById(id));
    }

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('WRITE','CREATE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<StateDTO> create(@Valid @RequestBody StateDTO dto) {
        return new ResponseEntity<>(stateService.create(dto), HttpStatus.CREATED);
    }

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('UPDATE')")
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<StateDTO> update(@PathVariable Long id, @Valid @RequestBody StateDTO dto) {
        return ResponseEntity.ok(stateService.update(id, dto));
    }

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('DELETE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stateService.delete(id);
        return ResponseEntity.noContent()
                .build();
    }
}
