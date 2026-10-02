package edu.arizona.engagement.dto;

import java.time.Instant;

import edu.arizona.engagement.model.enums.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReactionResponse {

    private Long id;
    private Long userId;
    private Long gameId;
    private ReactionType type;
    private Instant createdAt;
    private Instant updatedAt;
}
