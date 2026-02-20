package com.immortals.authapp.security;

import com.immortals.platform.domain.auth.entity.Permissions;
import com.immortals.platform.domain.auth.entity.Roles;
import com.immortals.platform.domain.auth.entity.User;
import com.immortals.authapp.repository.RoleRepository;
import com.immortals.authapp.repository.AuthRepository;
import com.immortals.platform.cache.providers.redis.RedisHashCacheService;
import com.immortals.platform.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
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

import static com.immortals.platform.domain.auth.constants.CacheConstants.USER_HASH_KEY;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AuthRepository authRepository;
    private final RedisHashCacheService<String, String, Object> hashCacheService;
    private final RoleRepository roleRepository;

    @Value("${auth.access-token-expiry-ms}")
    private int jwtExpirationMs;

    @Transactional
    @Override
    @Cacheable(value = "user-details", key = "#userNameOrEmailOrPhone", condition = "#userNameOrEmailOrPhone != null")
    public UserDetails loadUserByUsername(String userNameOrEmailOrPhone) throws UsernameNotFoundException {
        User user = authRepository.findUser(userNameOrEmailOrPhone)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username or email: " + userNameOrEmailOrPhone));

        Set<Roles> roles = authRepository.findByIdWithRoles(user.getId())
                .get()
                .getRoles();

        AtomicReference<Collection<Permissions>> permissions = new AtomicReference<>();

        roles.forEach(role -> permissions.set(roleRepository.findByIdWithPermissions(role.getRoleId())
                .get()
                .getPermissions()));

        String userJson = JsonUtils.toJson(user);
        hashCacheService.put(
                USER_HASH_KEY + ":" + user.getUserName(),
                user.getUserName(),
                userJson,
                Duration.ofMillis(jwtExpirationMs)
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
