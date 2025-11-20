package com.immortals.authapp.controller.users;

import com.immortals.platform.domain.dto.CountryDTO;
import com.immortals.authapp.service.user.CountryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/countries")
@RequiredArgsConstructor
@Validated
public class CountryController {

    private final CountryService service;

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('READ')")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<CountryDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('WRITE','CREATE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CountryDTO> create(@Valid @RequestBody CountryDTO dto) {
        return new ResponseEntity<>(service.create(dto), HttpStatus.CREATED);
    }

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('UPDATE')")
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<CountryDTO> update(@PathVariable Long id, @Valid @RequestBody CountryDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('DELETE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent()
                .build();
    }
}
