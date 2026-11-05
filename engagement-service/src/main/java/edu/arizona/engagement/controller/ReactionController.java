package edu.arizona.engagement.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.arizona.engagement.dto.ReactionRequest;
import edu.arizona.engagement.dto.ReactionResponse;
import edu.arizona.engagement.dto.ReactionUpdateRequest;
import edu.arizona.engagement.service.Paging;
import edu.arizona.engagement.service.ReactionService;
import edu.arizona.security.CurrentUser;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reactions")
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    /** The reacting user is the caller, taken from the token. */
    @PostMapping
    public ResponseEntity<ReactionResponse> createReaction(@Valid @RequestBody ReactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reactionService.createReaction(CurrentUser.id(), request));
    }

    /** Newest first, 50 per page by default (at most 100). Ask for further pages with {@code page=1,2,...}. */
    @GetMapping
    public List<ReactionResponse> getReactionsForGame(@RequestParam Long gameId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = Paging.DEFAULT_SIZE) int size) {
        return reactionService.getReactionsForGame(gameId, page, size);
    }

    @PutMapping("/{id}")
    public ReactionResponse updateReaction(@PathVariable Long id, @Valid @RequestBody ReactionUpdateRequest request) {
        return reactionService.updateReaction(id, request, CurrentUser.id());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReaction(@PathVariable Long id) {
        reactionService.deleteReaction(id, CurrentUser.id(), CurrentUser.isAdmin());
        return ResponseEntity.noContent().build();
    }
}
