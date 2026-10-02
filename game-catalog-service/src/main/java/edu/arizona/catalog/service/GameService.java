package edu.arizona.catalog.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import edu.arizona.catalog.dto.GameRequest;
import edu.arizona.catalog.dto.GameResponse;
import edu.arizona.catalog.model.entity.Game;
import edu.arizona.catalog.model.enums.ApprovalStatus;
import edu.arizona.catalog.repository.GameRepository;

@Service
public class GameService {

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public GameResponse createGame(GameRequest request) {
        Game game = new Game();
        game.setTitle(request.getTitle());
        game.setDescription(request.getDescription());
        game.setGenre(request.getGenre());
        game.setPlayableUrl(request.getPlayableUrl());
        game.setCreatorId(request.getCreatorId());
        game.setApprovalStatus(ApprovalStatus.PENDING);

        return toResponse(gameRepository.save(game));
    }

    public GameResponse getGame(Long id) {
        return toResponse(findGameOrThrow(id));
    }

    public List<GameResponse> getAllGames() {
        return gameRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<GameResponse> getGamesByStatus(ApprovalStatus status) {
        return gameRepository.findByApprovalStatus(status).stream()
                .map(this::toResponse)
                .toList();
    }

    public GameResponse updateGame(Long id, GameRequest request) {
        Game game = findGameOrThrow(id);

        game.setTitle(request.getTitle());
        game.setDescription(request.getDescription());
        game.setGenre(request.getGenre());
        game.setPlayableUrl(request.getPlayableUrl());
        if (request.getApprovalStatus() != null) {
            game.setApprovalStatus(request.getApprovalStatus());
        }

        return toResponse(gameRepository.save(game));
    }

    public void deleteGame(Long id) {
        if (!gameRepository.existsById(id)) {
            throw new NoSuchElementException("Game not found: " + id);
        }
        gameRepository.deleteById(id);
    }

    private Game findGameOrThrow(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Game not found: " + id));
    }

    private GameResponse toResponse(Game game) {
        return new GameResponse(
                game.getId(),
                game.getTitle(),
                game.getDescription(),
                game.getGenre(),
                game.getPlayableUrl(),
                game.getCreatorId(),
                game.getApprovalStatus(),
                game.getCreatedAt(),
                game.getUpdatedAt());
    }
}
