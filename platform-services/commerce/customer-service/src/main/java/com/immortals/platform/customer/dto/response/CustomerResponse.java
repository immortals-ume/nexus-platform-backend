package com.immortals.platform.customer.dto.response;

public record CustomerResponse(
    String id,
    String firstname,
    String lastname,
    String email,
    Address address
) {

}
