package edu.arizona.engagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LibraryEntryRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long gameId;
}
