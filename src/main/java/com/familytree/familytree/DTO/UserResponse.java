package com.familytree.familytree.DTO;

public class UserResponse {

    private Long id;
    private String username;
    private boolean profileCompleted;

    public UserResponse() {
    }

    public UserResponse(
            Long id,
            String username,
            boolean profileCompleted) {

        this.id = id;
        this.username = username;
        this.profileCompleted = profileCompleted;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setProfileCompleted(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }
}