package com.immortals.authapp.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event payload for user registration events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Instant registeredAt;
}
