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
import edu.arizona.engagement.service.ReactionService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reactions")
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @PostMapping
    public ResponseEntity<ReactionResponse> createReaction(@Valid @RequestBody ReactionRequest request) {
        ReactionResponse response = reactionService.createReaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<ReactionResponse> getReactionsForGame(@RequestParam Long gameId) {
        return reactionService.getReactionsForGame(gameId);
    }

    @PutMapping("/{id}")
    public ReactionResponse updateReaction(@PathVariable Long id, @Valid @RequestBody ReactionRequest request) {
        return reactionService.updateReaction(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReaction(@PathVariable Long id) {
        reactionService.deleteReaction(id);
        return ResponseEntity.noContent().build();
    }
}
