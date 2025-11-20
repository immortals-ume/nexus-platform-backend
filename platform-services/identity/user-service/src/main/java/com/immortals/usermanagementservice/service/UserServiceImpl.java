package com.immortals.usermanagementservice.service;


import com.immortals.usermanagementservice.annotation.ReadOnly;
import com.immortals.usermanagementservice.annotation.WriteOnly;
import com.immortals.usermanagementservice.manager.TokenLockManager;
import com.immortals.usermanagementservice.model.dto.RegisterRequestDTO;
import com.immortals.usermanagementservice.model.dto.ResetCredentials;
import com.immortals.usermanagementservice.model.dto.UserAddressDTO;
import com.immortals.usermanagementservice.model.dto.UserDto;
import com.immortals.usermanagementservice.model.entity.User;
import com.immortals.usermanagementservice.model.entity.UserAddress;
import com.immortals.usermanagementservice.model.enums.AddressStatus;
import com.immortals.usermanagementservice.model.enums.UserTypes;
import com.immortals.usermanagementservice.repository.UserAddressRepository;
import com.immortals.usermanagementservice.repository.UserRepository;
import com.immortals.usermanagementservice.service.cache.CacheService;
import com.immortals.usermanagementservice.service.exception.AuthException;
import com.immortals.usermanagementservice.service.exception.ResourceNotFoundException;
import com.immortals.usermanagementservice.service.exception.UserException;
import com.immortals.usermanagementservice.utils.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static com.immortals.usermanagementservice.constants.CacheConstants.USER_HASH_KEY;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepo;
    @Qualifier("passwordEncoder")
    private final PasswordEncoder passwordEncoder;
    private final CityService cityService;
    private final CountryService countryService;
    private final StateService stateService;
    private final CacheService<String, String, User> cacheService;
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private final TokenLockManager tokenLockManager;

    public UserServiceImpl(UserAddressRepository userAddressRepo, PasswordEncoder passwordEncoder, CityService cityService, CountryService countryService, StateService stateService, UserRepository userRepository, CacheService<String, String, User> cacheService, TokenLockManager tokenLockManager) {
        this.userAddressRepo = userAddressRepo;
        this.passwordEncoder = passwordEncoder;
        this.cityService = cityService;
        this.countryService = countryService;
        this.stateService = stateService;
        this.userRepository = userRepository;
        this.cacheService = cacheService;
        this.tokenLockManager = tokenLockManager;
    }

    @WriteOnly
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = {Exception.class}
    )
    @Override
    public UserDto register(RegisterRequestDTO dto) {
        String lockKey = "lock:user:register:" + dto.email();
        String userHashKey = USER_HASH_KEY;
        String fieldKey = dto.email();
        ReentrantLock lock = locks.computeIfAbsent(dto.email(), k -> new ReentrantLock());
        lock.lock();
        try {
            User cachedUser = cacheService.get(userHashKey, fieldKey, lockKey);
            if (cachedUser != null) {
                log.info("User found in cache: [email={}]", dto.email());
                throw new UserException("User already exists with this email.");
            }

            if (userRepository.existsByEmail(dto.email()) || userRepository.existsByUserName(dto.userName())) {
                throw new UserException("User registration failed: Email or username already exists.");
            }

            if (!dto.password().equals(dto.reTypePassword())) {
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

            User savedUser = userRepository.saveAndFlush(user);
            cacheService.put(userHashKey, fieldKey, savedUser, Duration.ofMinutes(30), lockKey);

            log.info("New user registered successfully and cached: [username={}]", savedUser.getUserName());

            return new UserDto(
                    savedUser.getFirstName(),
                    savedUser.getMiddleName(),
                    savedUser.getLastName(),
                    savedUser.getUserName(),
                    savedUser.getEmail(),
                    savedUser.getPhoneCode(),
                    dto.contactNumber()
            );

        } catch (IllegalArgumentException e) {
            log.warn("User registration failed due to invalid input: {}", e.getMessage());
            throw new UserException(e.getMessage(), e);
        } catch (Exception e) {
            log.error("System error occurred during user registration: {}", e.getMessage(), e);
            throw new UserException("An unexpected error occurred during registration. Please contact support.", e);
        }finally {
            lock.unlock();
            locks.remove(dto.email());
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
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = {Exception.class}
    )
    @Override
    public UserDto updateUser(String email, UserDto dto) {
        String lockKey = "lock:user:update:" + email;
        String userHashKey = USER_HASH_KEY;

        try {

            tokenLockManager.acquireWrite(lockKey);
            User user = cacheService.get(userHashKey, email, lockKey);

            if (user == null) {
                user = userRepository.findUser(email)
                        .orElseThrow(() -> new UserException("User not found."));
            }

            if (dto.firstName() != null) user.setFirstName(dto.firstName());
            if (dto.middleName() != null) user.setMiddleName(dto.middleName());
            if (dto.lastName() != null) user.setLastName(dto.lastName());
            if (dto.phoneCode() != null) user.setPhoneCode(dto.phoneCode());
            if (dto.contactNumber() != null) user.setContactNumber(dto.contactNumber());

            user.setUpdatedDate(DateTimeUtils.now());
            user.setUpdatedBy(UserTypes.SYSTEM.name());

            User savedUser = userRepository.saveAndFlush(user);
            
            cacheService.put(userHashKey, email, savedUser, Duration.ofHours(1), lockKey);

            return new UserDto(
                    savedUser.getFirstName(),
                    savedUser.getMiddleName(),
                    savedUser.getLastName(),
                    savedUser.getUserName(),
                    savedUser.getEmail(),
                    savedUser.getPhoneCode(),
                    savedUser.getContactNumber()
            );

        } catch (Exception e) {
            log.error("Failed to update user [{}]: {}", email, e.getMessage(), e);
            throw new UserException("Unable to update user. Please try again.", e);
        } finally {
            tokenLockManager.releaseWrite(lockKey);
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
            throw new AuthException("Failed to retrieve user details.");
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
