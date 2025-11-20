package com.immortals.authapp.service.user;

import com.immortals.platform.domain.dto.RegisterRequestDTO;
import com.immortals.platform.domain.dto.ResetCredentials;
import com.immortals.platform.domain.dto.UserAddressDTO;
import com.immortals.platform.domain.dto.UserDto;
import com.immortals.platform.domain.entity.User;
import com.immortals.platform.domain.entity.UserAddress;

public interface UserService {
    UserDto register(RegisterRequestDTO dto);

    String resetPassword(ResetCredentials resetCredentials);

    void updateLoginStatus(String username);

    void updateLogoutStatus(String username);

    UserAddress updateOrAddUserAddress(String userId, UserAddressDTO addressDTO);

    User getUserByUsername(String username);

    String sendChangePasswordEmail(String username);
}
