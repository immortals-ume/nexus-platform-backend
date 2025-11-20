package com.immortals.usermanagementservice.controller;


import com.immortals.usermanagementservice.model.dto.CountryDTO;
import com.immortals.usermanagementservice.service.CountryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/countries")
@RequiredArgsConstructor
@Validated
public class CountryController {

    private final CountryService countryService;

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('READ')")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<CountryDTO>> getAll() {
        return ResponseEntity.ok(countryService.getAll());
    }

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('WRITE','CREATE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String create(@Valid @RequestBody Set<CountryDTO> dto) {
        return countryService.create(dto);
    }

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('UPDATE')")
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CountryDTO update(@PathVariable Long id, @Valid @RequestBody CountryDTO dto) {
        return countryService.update(id,dto);
    }

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('DELETE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        countryService.delete(id);
        return ResponseEntity.noContent()
                .build();
    }
}
