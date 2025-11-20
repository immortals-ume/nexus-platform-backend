package com.immortals.usermanagementservice.service;

import com.immortals.usermanagementservice.annotation.WriteOnly;
import com.immortals.usermanagementservice.model.dto.RegisterRequestDTO;
import com.immortals.usermanagementservice.model.dto.ResetCredentials;
import com.immortals.usermanagementservice.model.dto.UserAddressDTO;
import com.immortals.usermanagementservice.model.dto.UserDto;
import com.immortals.usermanagementservice.model.entity.User;
import com.immortals.usermanagementservice.model.entity.UserAddress;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {
    UserDto register(RegisterRequestDTO dto);

    String resetPassword(ResetCredentials resetCredentials);

    void updateLoginStatus(String username);

    @WriteOnly
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = {Exception.class}
    )
    UserDto updateUser(String email, UserDto dto);

    void updateLogoutStatus(String username);

    UserAddress updateOrAddUserAddress(String userId, UserAddressDTO addressDTO);

    User getUserByUsername(String username);

    String sendChangePasswordEmail(String username);
}
