package edu.arizona.catalog.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.arizona.catalog.dto.ApprovalRequest;
import edu.arizona.catalog.dto.GameRequest;
import edu.arizona.catalog.dto.GameResponse;
import edu.arizona.catalog.model.enums.ApprovalStatus;
import edu.arizona.catalog.service.GameService;
import edu.arizona.security.CurrentUser;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /** Developers and administrators submit games; the creator is taken from the token. */
    @PostMapping
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<GameResponse> createGame(@Valid @RequestBody GameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gameService.createGame(request, CurrentUser.id()));
    }

    @GetMapping("/{id}")
    public GameResponse getGame(@PathVariable Long id) {
        return gameService.getGame(id, CurrentUser.id(), CurrentUser.isAdmin());
    }

    /**
     * Approved games for everyone. {@code mine=true} lists the caller's own games in any state;
     * administrators may filter all games by status.
     */
    @GetMapping
    public List<GameResponse> getGames(@RequestParam(required = false) ApprovalStatus status,
            @RequestParam(defaultValue = "false") boolean mine) {
        return gameService.listGames(status, mine, CurrentUser.id(), CurrentUser.isAdmin());
    }

    @PutMapping("/{id}")
    public GameResponse updateGame(@PathVariable Long id, @Valid @RequestBody GameRequest request) {
        return gameService.updateGame(id, request, CurrentUser.id(), CurrentUser.isAdmin());
    }

    @PutMapping("/{id}/approval")
    @PreAuthorize("hasRole('ADMIN')")
    public GameResponse setApproval(@PathVariable Long id, @Valid @RequestBody ApprovalRequest request) {
        return gameService.setApproval(id, request.getStatus());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable Long id) {
        gameService.deleteGame(id, CurrentUser.id(), CurrentUser.isAdmin());
        return ResponseEntity.noContent().build();
    }
}
