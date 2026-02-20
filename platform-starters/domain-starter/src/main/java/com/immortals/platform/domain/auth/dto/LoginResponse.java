package com.immortals.platform.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(@JsonProperty(access = JsonProperty.Access.READ_ONLY)
                             String username,

                            @JsonProperty(access = JsonProperty.Access.READ_ONLY)
                             String accessToken,


                            @JsonProperty(access = JsonProperty.Access.READ_ONLY) String refreshToken,


                            @JsonProperty(access = JsonProperty.Access.READ_ONLY)
                            String message,    long expiresIn) {
}
