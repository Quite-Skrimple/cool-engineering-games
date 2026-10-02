package edu.arizona.catalog.dto;

import java.time.Instant;

import edu.arizona.catalog.model.enums.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GameResponse {

    private Long id;
    private String title;
    private String description;
    private String genre;
    private String playableUrl;
    private Long creatorId;
    private ApprovalStatus approvalStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
