package com.immortals.usermanagementservice.repository;

import com.immortals.usermanagementservice.model.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
}
