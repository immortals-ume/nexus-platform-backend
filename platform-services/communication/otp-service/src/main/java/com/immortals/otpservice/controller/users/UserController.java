package com.immortals.otpservice.controller.users;


import com.immortals.authapp.model.dto.RegisterRequestDTO;
import com.immortals.authapp.model.dto.UserAddressDTO;
import com.immortals.authapp.model.dto.UserDto;
import com.immortals.authapp.model.entity.User;
import com.immortals.authapp.service.user.UserService;
import com.immortals.authapp.utils.JsonUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PreAuthorize(" hasRole('GUEST') ")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto register(@Valid @RequestBody RegisterRequestDTO dto) {
        return userService.register(dto);
    }


    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('READ')")
    @GetMapping("/{username}")
    @ResponseStatus(HttpStatus.CREATED)
    public User register( @PathVariable String username) {
        return userService.getUserByUsername(username);
    }


    @PreAuthorize(" hasRole('ADMIN') or hasRole('ROLE_USER') and hasAnyAuthority('UPDATE','CREATE')")
    @PatchMapping("/address")
    public ResponseEntity<String> updateAddress(@Valid @RequestBody UserAddressDTO dto) {
        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User not authenticated");
        }

        return ResponseEntity.ok(JsonUtils.toJson(userService.updateOrAddUserAddress(auth.getName(), dto)));
    }
}
