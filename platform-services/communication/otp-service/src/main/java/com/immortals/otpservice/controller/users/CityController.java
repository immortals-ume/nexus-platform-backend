package com.immortals.otpservice.controller.users;

import com.immortals.authapp.model.dto.CityDTO;
import com.immortals.authapp.service.user.CityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('READ')")
    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<List<CityDTO>> getAllCities() {
        return ResponseEntity.ok(cityService.getAll());
    }

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('READ')")
    @GetMapping("/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<CityDTO> getCity(@PathVariable Long id) {
        return ResponseEntity.ok(cityService.getById(id));
    }

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('WRITE','CREATE')")
    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public ResponseEntity<CityDTO> createCity(@Valid @RequestBody CityDTO dto) {
        return ResponseEntity.ok(cityService.create(dto));
    }

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('UPDATE')")
    @PatchMapping("/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<CityDTO> updateCity(@PathVariable Long id, @Valid @RequestBody CityDTO dto) {
        return ResponseEntity.ok(cityService.update(id, dto));
    }

    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('DELETE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteCity(@PathVariable Long id) {
        cityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
