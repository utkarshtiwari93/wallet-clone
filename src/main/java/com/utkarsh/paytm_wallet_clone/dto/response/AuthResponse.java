package com.utkarsh.paytm_wallet_clone.dto.response;

public class AuthResponse {

    private String token;
    private String email;
    private Long userId;
    private String name;

    public AuthResponse(String token, String email, Long userId, String name) {
        this.token = token;
        this.email = email;
        this.userId = userId;
        this.name = name;
    }

    // Getters
    public String getToken() { return token; }
    public String getEmail() { return email; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
}
