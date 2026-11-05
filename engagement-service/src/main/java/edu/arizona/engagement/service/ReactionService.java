package edu.arizona.engagement.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import edu.arizona.engagement.config.GameCatalogClient;
import edu.arizona.engagement.dto.ReactionRequest;
import edu.arizona.engagement.dto.ReactionResponse;
import edu.arizona.engagement.dto.ReactionUpdateRequest;
import edu.arizona.engagement.exception.ConflictException;
import edu.arizona.engagement.model.entity.Reaction;
import edu.arizona.engagement.repository.ReactionRepository;

@Service
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final GameCatalogClient catalogClient;

    public ReactionService(ReactionRepository reactionRepository, GameCatalogClient catalogClient) {
        this.reactionRepository = reactionRepository;
        this.catalogClient = catalogClient;
    }

    public ReactionResponse createReaction(Long userId, ReactionRequest request) {
        catalogClient.requireVisibleGame(request.getGameId());

        if (reactionRepository.existsByUserIdAndGameId(userId, request.getGameId())) {
            throw alreadyReacted(request.getGameId());
        }

        Reaction reaction = new Reaction();
        reaction.setUserId(userId);
        reaction.setGameId(request.getGameId());
        reaction.setType(request.getType());

        try {
            return toResponse(reactionRepository.save(reaction));
        } catch (DataIntegrityViolationException e) {
            throw alreadyReacted(request.getGameId());
        }
    }

    public List<ReactionResponse> getReactionsForGame(Long gameId, int page, int size) {
        return reactionRepository.findByGameId(gameId, Paging.newestFirst(page, size)).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Only the person who reacted can change their reaction. */
    public ReactionResponse updateReaction(Long id, ReactionUpdateRequest request, Long callerId) {
        Reaction reaction = findReactionOrThrow(id);
        if (!reaction.getUserId().equals(callerId)) {
            throw new AccessDeniedException("You can only change your own reaction");
        }
        reaction.setType(request.getType());
        return toResponse(reactionRepository.save(reaction));
    }

    public void deleteReaction(Long id, Long callerId, boolean admin) {
        Reaction reaction = findReactionOrThrow(id);
        if (!admin && !reaction.getUserId().equals(callerId)) {
            throw new AccessDeniedException("You can only remove your own reaction");
        }
        reactionRepository.delete(reaction);
    }

    private ConflictException alreadyReacted(Long gameId) {
        return new ConflictException("You already reacted to game " + gameId);
    }

    private Reaction findReactionOrThrow(Long id) {
        return reactionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reaction not found: " + id));
    }

    private ReactionResponse toResponse(Reaction reaction) {
        return new ReactionResponse(
                reaction.getId(),
                reaction.getUserId(),
                reaction.getGameId(),
                reaction.getType(),
                reaction.getCreatedAt(),
                reaction.getUpdatedAt());
    }
}
