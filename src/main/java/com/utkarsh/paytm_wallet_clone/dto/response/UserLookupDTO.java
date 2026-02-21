package com.utkarsh.paytm_wallet_clone.dto.response;

public class UserLookupDTO {

    private String name;
    private String phone;
    private boolean exists;

    public UserLookupDTO(String name, String phone, boolean exists) {
        this.name = name;
        this.phone = phone;
        this.exists = exists;
    }

    // Empty response for non-existent users
    public static UserLookupDTO notFound() {
        return new UserLookupDTO(null, null, false);
    }

    // Getters
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public boolean isExists() { return exists; }
}