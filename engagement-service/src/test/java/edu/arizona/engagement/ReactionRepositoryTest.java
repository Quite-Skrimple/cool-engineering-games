package edu.arizona.engagement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import edu.arizona.engagement.model.entity.Reaction;
import edu.arizona.engagement.model.enums.ReactionType;
import edu.arizona.engagement.repository.ReactionRepository;

@DataJpaTest
class ReactionRepositoryTest {

    @Autowired
    private ReactionRepository reactionRepository;

    private Reaction newReaction(Long userId, Long gameId, ReactionType type) {
        Reaction reaction = new Reaction();
        reaction.setUserId(userId);
        reaction.setGameId(gameId);
        reaction.setType(type);
        return reaction;
    }

    @Test
    void savesAndFindsReactionByUserAndGame() {
        reactionRepository.save(newReaction(1L, 1L, ReactionType.LIKE));

        assertThat(reactionRepository.findByUserIdAndGameId(1L, 1L)).isPresent();
        assertThat(reactionRepository.findByUserIdAndGameId(1L, 2L)).isEmpty();
    }

    @Test
    void findsReactionsByGameId() {
        reactionRepository.save(newReaction(1L, 1L, ReactionType.LIKE));
        reactionRepository.save(newReaction(2L, 1L, ReactionType.DISLIKE));

        assertThat(reactionRepository.findByGameId(1L)).hasSize(2);
    }

    @Test
    void existsByUserIdAndGameIdReflectsSavedState() {
        reactionRepository.save(newReaction(1L, 1L, ReactionType.LIKE));

        assertThat(reactionRepository.existsByUserIdAndGameId(1L, 1L)).isTrue();
        assertThat(reactionRepository.existsByUserIdAndGameId(2L, 1L)).isFalse();
    }
}
