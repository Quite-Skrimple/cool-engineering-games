package edu.arizona.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String tokenType;
    /** Seconds until the token expires. */
    private long expiresIn;
    private UserResponse user;
}
