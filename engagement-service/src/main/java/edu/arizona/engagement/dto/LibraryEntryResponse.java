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
public class LibraryEntryResponse {

    private Long id;
    private Long userId;
    private Long gameId;
    private Instant addedAt;
}
