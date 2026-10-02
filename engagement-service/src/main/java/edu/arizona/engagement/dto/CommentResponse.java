package edu.arizona.engagement.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponse {

    private Long id;
    private Long userId;
    private Long gameId;
    private String body;
    private Instant createdAt;
    private Instant updatedAt;
}
