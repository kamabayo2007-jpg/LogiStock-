package com.logistock.adapter.web.dto;

public record LoginResponse(String token, UserDto user) {

    public record UserDto(Long id, String nom, String email, String role) {
    }
}
