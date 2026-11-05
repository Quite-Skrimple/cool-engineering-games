package edu.arizona.catalog.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.arizona.catalog.dto.GameRequest;
import edu.arizona.catalog.dto.GameResponse;
import edu.arizona.catalog.model.entity.Game;
import edu.arizona.catalog.model.enums.ApprovalStatus;
import edu.arizona.catalog.repository.GameRepository;

/**
 * Visibility rules: everyone sees APPROVED games; a game in any other state is visible only to its
 * creator and to administrators, and looks like it does not exist to everyone else.
 */
@Service
public class GameService {

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Transactional
    public GameResponse createGame(GameRequest request, Long creatorId) {
        Game game = new Game();
        apply(game, request);
        game.setCreatorId(creatorId);
        game.setApprovalStatus(ApprovalStatus.PENDING);
        return toResponse(gameRepository.save(game));
    }

    @Transactional(readOnly = true)
    public GameResponse getGame(Long id, Long callerId, boolean admin) {
        Game game = findGameOrThrow(id);
        if (!canView(game, callerId, admin)) {
            throw notFound(id);
        }
        return toResponse(game);
    }

    @Transactional(readOnly = true)
    public List<GameResponse> listGames(ApprovalStatus status, boolean mine, Long callerId, boolean admin) {
        List<Game> games;
        if (admin) {
            games = status != null ? gameRepository.findByApprovalStatus(status) : gameRepository.findAll();
        } else if (mine) {
            games = gameRepository.findByCreatorId(callerId).stream()
                    .filter(game -> status == null || game.getApprovalStatus() == status)
                    .toList();
        } else if (status == null || status == ApprovalStatus.APPROVED) {
            games = gameRepository.findByApprovalStatus(ApprovalStatus.APPROVED);
        } else {
            games = List.of();
        }
        return games.stream().map(this::toResponse).toList();
    }

    /** An edit by the creator sends an approved game back for review; an administrator's edit does not. */
    @Transactional
    public GameResponse updateGame(Long id, GameRequest request, Long callerId, boolean admin) {
        Game game = findManageableGame(id, callerId, admin);
        apply(game, request);
        if (!admin && game.getApprovalStatus() == ApprovalStatus.APPROVED) {
            game.setApprovalStatus(ApprovalStatus.PENDING);
        }
        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public GameResponse setApproval(Long id, ApprovalStatus status) {
        Game game = findGameOrThrow(id);
        game.setApprovalStatus(status);
        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public void deleteGame(Long id, Long callerId, boolean admin) {
        gameRepository.delete(findManageableGame(id, callerId, admin));
    }

    private Game findManageableGame(Long id, Long callerId, boolean admin) {
        Game game = findGameOrThrow(id);
        if (!canView(game, callerId, admin)) {
            throw notFound(id);
        }
        if (!admin && !game.getCreatorId().equals(callerId)) {
            throw new AccessDeniedException("Only the creator or an administrator can change this game");
        }
        return game;
    }

    private boolean canView(Game game, Long callerId, boolean admin) {
        return admin
                || game.getApprovalStatus() == ApprovalStatus.APPROVED
                || game.getCreatorId().equals(callerId);
    }

    private void apply(Game game, GameRequest request) {
        game.setTitle(request.getTitle());
        game.setDescription(request.getDescription());
        game.setGenre(request.getGenre());
        game.setPlayableUrl(request.getPlayableUrl());
    }

    private Game findGameOrThrow(Long id) {
        return gameRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    private NoSuchElementException notFound(Long id) {
        return new NoSuchElementException("Game not found: " + id);
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
