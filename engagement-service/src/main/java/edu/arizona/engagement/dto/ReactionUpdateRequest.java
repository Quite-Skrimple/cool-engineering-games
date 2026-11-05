package edu.arizona.engagement.dto;

import edu.arizona.engagement.model.enums.ReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReactionUpdateRequest {

    @NotNull
    private ReactionType type;
}
