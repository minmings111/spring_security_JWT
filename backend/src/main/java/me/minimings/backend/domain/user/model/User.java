package me.minimings.backend.domain.user.model;

import java.time.LocalDateTime;

public class User {
    private Integer id;

    private String userId;
    private String userPw;

    private String userName;
    private String userEmail;

    private LoginType loginType;
    private String socialId;

    private Boolean isActive;

    private UserRole role;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
