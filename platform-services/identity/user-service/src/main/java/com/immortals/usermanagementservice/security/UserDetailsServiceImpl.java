package com.immortals.usermanagementservice.security;

import com.immortals.authapp.model.entity.Permissions;
import com.immortals.authapp.model.entity.Roles;
import com.immortals.authapp.model.entity.User;
import com.immortals.authapp.model.helper.UserPrincipal;
import com.immortals.authapp.repository.RoleRepository;
import com.immortals.authapp.repository.UserRepository;
import com.immortals.authapp.service.cache.CacheService;
import com.immortals.authapp.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.immortals.authapp.constants.CacheConstants.USER_HASH_KEY;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final CacheService<String, String, String> hashCacheService;
    private final RoleRepository roleRepository;

    @Value("${auth.access-token-expiry-ms}")
    private int jwtExpirationMs;

    @Transactional
    @Override
    public UserDetails loadUserByUsername(String userNameOrEmailOrPhone) throws UsernameNotFoundException {
        User user = userRepository.findUser(userNameOrEmailOrPhone)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username or email: " + userNameOrEmailOrPhone));

        Set<Roles> roles = userRepository.findByIdWithRoles(user.getUserId())
                .get()
                .getRoles();

        AtomicReference<Collection<Permissions>> permissions = new AtomicReference<>();

        roles.forEach(role -> permissions.set(roleRepository.findByIdWithPermissions(role.getRoleId())
                .get()
                .getPermissions()));


        String userJson = JsonUtils.toJson(user);
        hashCacheService.put(
                USER_HASH_KEY+":"+user.getUserName(),
                user.getUserName(),
                userJson,
                Duration.ofMillis(jwtExpirationMs),
                user.getUserName()
        );

        return UserPrincipal.create(
                user,
                roles,
                permissions.get()
                        .stream()
                        .map(Permissions::getPermissionName)
                        .collect(Collectors.toSet())
        );
    }
}
