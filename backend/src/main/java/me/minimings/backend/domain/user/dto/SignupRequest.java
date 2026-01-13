package me.minimings.backend.domain.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SignupRequest {
    
    private String userId;
    private String userPw;
    private String userName;
    private String email;

}
