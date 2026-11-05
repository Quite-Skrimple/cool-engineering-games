package edu.arizona.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What a creator submits. The creator is always the caller and the approval status is
 * controlled by the platform, so neither can be set from the request.
 */
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
}
