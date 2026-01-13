package me.minimings.backend.domain.user.model;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class User {
    
    private Integer id;
    private String userId;
    private String userPw;
    private String userName;
    private String email;

    // about social login
    public enum LoginType {
        LOCAL("local"), GOOGLE("google"), KAKAO("kakao");
        
        private final String value;
        
        LoginType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }
    private LoginType loginType;
    private String socialId;

    // infomation
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // role
    public enum Role {
        ROLE_USER("ROLE_USER"), ROLE_ADMIN("ROLE_ADMIN");

        private final String value;

        Role(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }
    private Role role;

}
