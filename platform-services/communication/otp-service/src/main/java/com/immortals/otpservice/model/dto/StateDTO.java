package com.immortals.otpservice.model.dto;


public record StateDTO(


        String name,


        String code,


        Boolean activeInd,


        Long countryId) {
}
