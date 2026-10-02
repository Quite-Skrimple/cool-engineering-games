package edu.arizona.catalog.dto;

import edu.arizona.catalog.model.enums.ApprovalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GameRequest {

    @NotBlank
    @Size(min = 2, max = 120)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotBlank
    private String genre;

    @NotBlank
    private String playableUrl;

    @NotNull
    private Long creatorId;

    private ApprovalStatus approvalStatus;
}
