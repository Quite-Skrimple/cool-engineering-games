package edu.arizona.engagement.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import edu.arizona.engagement.dto.ReactionRequest;
import edu.arizona.engagement.dto.ReactionResponse;
import edu.arizona.engagement.exception.ConflictException;
import edu.arizona.engagement.model.entity.Reaction;
import edu.arizona.engagement.repository.ReactionRepository;

@Service
public class ReactionService {

    private final ReactionRepository reactionRepository;

    public ReactionService(ReactionRepository reactionRepository) {
        this.reactionRepository = reactionRepository;
    }

    public ReactionResponse createReaction(ReactionRequest request) {
        if (reactionRepository.existsByUserIdAndGameId(request.getUserId(), request.getGameId())) {
            throw new ConflictException(
                    "User " + request.getUserId() + " already reacted to game " + request.getGameId());
        }

        Reaction reaction = new Reaction();
        reaction.setUserId(request.getUserId());
        reaction.setGameId(request.getGameId());
        reaction.setType(request.getType());

        return toResponse(reactionRepository.save(reaction));
    }

    public List<ReactionResponse> getReactionsForGame(Long gameId) {
        return reactionRepository.findByGameId(gameId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ReactionResponse updateReaction(Long id, ReactionRequest request) {
        Reaction reaction = findReactionOrThrow(id);
        reaction.setType(request.getType());
        return toResponse(reactionRepository.save(reaction));
    }

    public void deleteReaction(Long id) {
        if (!reactionRepository.existsById(id)) {
            throw new NoSuchElementException("Reaction not found: " + id);
        }
        reactionRepository.deleteById(id);
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
