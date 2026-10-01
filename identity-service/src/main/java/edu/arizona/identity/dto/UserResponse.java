package edu.arizona.identity.dto;

import java.time.Instant;

import edu.arizona.identity.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private UserRole role;
    private Instant createdAt;
    private Instant updatedAt;
}