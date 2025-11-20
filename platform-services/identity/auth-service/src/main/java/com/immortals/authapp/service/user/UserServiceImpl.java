package com.immortals.authapp.service.user;

import com.immortals.authapp.annotation.ReadOnly;
import com.immortals.authapp.annotation.WriteOnly;
import com.immortals.authapp.event.UserRegisteredEvent;
import com.immortals.platform.domain.dto.RegisterRequestDTO;
import com.immortals.platform.domain.dto.ResetCredentials;
import com.immortals.platform.domain.dto.UserAddressDTO;
import com.immortals.platform.domain.dto.UserDto;
import com.immortals.platform.domain.entity.User;
import com.immortals.platform.domain.entity.UserAddress;
import com.immortals.platform.domain.enums.AddressStatus;
import com.immortals.platform.domain.enums.UserTypes;
import com.immortals.authapp.repository.UserAddressRepository;
import com.immortals.authapp.repository.UserRepository;
import com.immortals.authapp.service.AuthEventPublisher;
import com.immortals.authapp.service.cache.CacheService;
import com.immortals.platform.common.exception.AuthenticationException;
import com.immortals.authapp.service.exception.ResourceNotFoundException;
import com.immortals.platform.common.exception.BusinessException;
import com.immortals.authapp.utils.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;

import static com.immortals.authapp.constants.CacheConstants.USER_HASH_KEY;


@Slf4j
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepo;
    private final UserAddressRepository userAddressRepo;
    @Qualifier("passwordEncoder")
    private final PasswordEncoder passwordEncoder;
    private final CityService cityService;
    private final CountryService countryService;
    private final StateService stateService;
    private final UserRepository userRepository;
    private final CacheService<String, String, User> cacheService;
    private final AuthEventPublisher authEventPublisher;

    public UserServiceImpl(UserRepository userRepo, UserAddressRepository userAddressRepo, PasswordEncoder passwordEncoder, CityService cityService, CountryService countryService, StateService stateService, UserRepository userRepository, CacheService<String, String, User> cacheService, AuthEventPublisher authEventPublisher) {
        this.userRepo = userRepo;
        this.userAddressRepo = userAddressRepo;
        this.passwordEncoder = passwordEncoder;
        this.cityService = cityService;
        this.countryService = countryService;
        this.stateService = stateService;
        this.userRepository = userRepository;
        this.cacheService = cacheService;
        this.authEventPublisher = authEventPublisher;
    }

    @WriteOnly
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = {Exception.class})
    @Override
    public UserDto register(RegisterRequestDTO dto) {
        try {
            if (userRepo.existsByEmail(dto.email()) || userRepo.existsByUserName(dto.userName())) {
                throw new UserException("User registration failed: Email or username already exists.");
            }

            if (!dto.password()
                    .equals(dto.reTypePassword())) {
                throw new UserException("Password confirmation does not match the original password.");
            }

            User user = User.builder()
                    .firstName(dto.firstName())
                    .middleName(dto.middleName())
                    .lastName(dto.lastName())
                    .userName(dto.userName())
                    .password(passwordEncoder.encode(dto.password()))
                    .email(dto.email())
                    .phoneCode(dto.phoneCode())
                    .contactNumber(dto.contactNumber())
                    .emailVerified(Boolean.FALSE)
                    .phoneNumberVerified(Boolean.FALSE)
                    .accountNonExpired(Boolean.TRUE)
                    .accountNonLocked(Boolean.TRUE)
                    .accountLocked(Boolean.FALSE)
                    .credentialsNonExpired(Boolean.TRUE)
                    .createdBy(UserTypes.SYSTEM.name())
                    .createdDate(DateTimeUtils.now())
                    .activeInd(Boolean.TRUE)
                    .build();

            log.info("New user registered successfully: [username={}]", user.getUserName());
            User savedUser = userRepo.saveAndFlush(user);

            // Publish UserRegistered event
            UserRegisteredEvent event = UserRegisteredEvent.builder()
                    .userId(savedUser.getUserId().toString())
                    .username(savedUser.getUserName())
                    .email(savedUser.getEmail())
                    .firstName(savedUser.getFirstName())
                    .lastName(savedUser.getLastName())
                    .registeredAt(Instant.now())
                    .build();
            authEventPublisher.publishUserRegistered(event);

            return new UserDto(savedUser.getFirstName(), savedUser.getMiddleName(), savedUser.getLastName(), savedUser.getUserName(), savedUser.getEmail(), savedUser.getPhoneCode(), dto.contactNumber());

        } catch (IllegalArgumentException e) {
            log.warn("User registration failed due to invalid input: {}", e.getMessage());
            throw new UserException(e.getMessage(), e);
        } catch (Exception e) {
            log.error("System error occurred during user registration: {}", e.getMessage(), e);
            throw new UserException("An unexpected error occurred during registration. Please contact support.", e);
        }
    }

    @Override
    public String resetPassword(ResetCredentials resetCredentials) {
        return "";
    }

    @WriteOnly
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = {Exception.class})
    @Override
    public void updateLoginStatus(String username) {
        try {
            User user = getUserByUsername(username);

            user.setLogin(Instant.now());
            user.setUpdatedBy(UserTypes.SYSTEM.name());
            user.setUpdatedDate(DateTimeUtils.now());

            log.info("Login timestamp updated for user: [username={}]", username);
            userRepository.saveAndFlush(user);

        } catch (ResourceNotFoundException e) {
            log.warn("Login update failed: User not found with username={}", username);
            throw new UserException(e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error while updating login timestamp for user [username={}]: {}", username, e.getMessage(), e);
            throw new UserException("Failed to update login time. Please try again later.");
        }
    }

    @WriteOnly
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = {Exception.class})
    @Override
    public void updateLogoutStatus(String username) {
        try {
            User user = getUserByUsername(username);

            user.setLogout(Instant.now());
            user.setUpdatedBy(UserTypes.SYSTEM.name());
            user.setUpdatedDate(DateTimeUtils.now());

            log.info("Logout timestamp updated for user: [username={}]", username);
            userRepository.saveAndFlush(user);

        } catch (ResourceNotFoundException e) {
            log.warn("Logout update failed: User not found with username={}", username);
            throw new UserException(e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error while updating logout timestamp for user [username={}]: {}", username, e.getMessage(), e);
            throw new UserException("Failed to update logout time. Please try again later.");
        }
    }

    @ReadOnly
    @Transactional
    @Override
    public User getUserByUsername(String username) {
        try {
            User cachedUser = cacheService.get(USER_HASH_KEY+":"+username, username, username);
            if (cachedUser != null) {
                log.debug("User retrieved from cache: [username={}]", username);
                return cachedUser;
            }

            log.debug("Fetching user from database: [username={}]", username);
            return userRepo.findUser(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User", username));
        } catch (RuntimeException e) {
            log.error("Error retrieving user by username: {}", username, e);
            throw new AuthenticationException("Failed to retrieve user details.");
        }
    }

    @Override
    public String sendChangePasswordEmail(String username) {
        return "";
    }

    @WriteOnly
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = {Exception.class})
    @Override
    public UserAddress updateOrAddUserAddress(String username, UserAddressDTO dto) {
        try {

            User user = getUserByUsername(username);
            UserAddress address = UserAddress.builder()
                    .user(user)
                    .addressLine1(dto.addressLine1())
                    .addressLine2(dto.addressLine2())
                    .city(cityService.toEntity(cityService.getById(dto.city())))
                    .states(stateService.toEntity(stateService.getById(dto.state())))
                    .country(countryService.toEntity(countryService.getById(dto.country())))
                    .pincode(dto.zipCode())
                    .status(AddressStatus.ACTIVE)
                    .timezone(ZoneId.systemDefault()
                            .toString())
                    .createdDate(DateTimeUtils.now())
                    .createdBy(UserTypes.SYSTEM.name())
                    .build();

            if (user.getUserAddresses() != null) {

                user.getUserAddresses()
                        .add(address);

                return saveAndUpdateUserAddress(username, address);
            }

            return saveAndUpdateUserAddress(username, address);

        } catch (ResourceNotFoundException e) {
            log.warn("Address update failed: User not found [username={}]", username);
            throw new UserException(e.getMessage(), e);
        } catch (Exception e) {
            log.error("System error during address update for user [username={}]: {}", username, e.getMessage(), e);
            throw new UserException("Failed to update address. Please try again later.");
        }
    }

    @WriteOnly
    private UserAddress saveAndUpdateUserAddress(String username, UserAddress address) {
        log.info("Address successfully updated for user: [username={}]", username);
        return userAddressRepo.saveAndFlush(address);
    }
}
