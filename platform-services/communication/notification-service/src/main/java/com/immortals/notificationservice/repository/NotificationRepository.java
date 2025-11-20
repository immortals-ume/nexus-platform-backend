package com.immortals.notificationservice.repository;

import com.immortals.notificationservice.model.NotificationCapture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationCapture, Long> {
}