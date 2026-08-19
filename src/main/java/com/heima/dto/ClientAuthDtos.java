package com.heima.dto;

public final class ClientAuthDtos {

    private ClientAuthDtos() {
    }

    public record SendCodeRequest(String email) {
    }

    public record EmailLoginRequest(String email, String code) {
    }

    public record ClientLoginResponse(String token, String email, String name) {
    }
}
