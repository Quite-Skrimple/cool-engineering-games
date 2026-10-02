package edu.arizona.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import edu.arizona.catalog.model.entity.Game;
import edu.arizona.catalog.model.enums.ApprovalStatus;
import edu.arizona.catalog.repository.GameRepository;

@DataJpaTest
class GameRepositoryTest {

    @Autowired
    private GameRepository gameRepository;

    private Game newGame(String title, Long creatorId) {
        Game game = new Game();
        game.setTitle(title);
        game.setDescription("A short engineering puzzle game.");
        game.setGenre("Puzzle");
        game.setPlayableUrl("https://example.com/" + title.toLowerCase().replace(" ", "-"));
        game.setCreatorId(creatorId);
        game.setApprovalStatus(ApprovalStatus.PENDING);
        return game;
    }

    @Test
    void savesAndFindsGameById() {
        Game saved = gameRepository.save(newGame("Circuit Breaker", 1L));

        assertThat(gameRepository.findById(saved.getId())).isPresent();
        assertThat(saved.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
    }

    @Test
    void findsGamesByApprovalStatus() {
        gameRepository.save(newGame("Circuit Breaker", 1L));

        assertThat(gameRepository.findByApprovalStatus(ApprovalStatus.PENDING)).hasSize(1);
        assertThat(gameRepository.findByApprovalStatus(ApprovalStatus.APPROVED)).isEmpty();
    }

    @Test
    void findsGamesByCreatorId() {
        gameRepository.save(newGame("Circuit Breaker", 1L));
        gameRepository.save(newGame("Truss Tower", 2L));

        assertThat(gameRepository.findByCreatorId(1L)).hasSize(1);
        assertThat(gameRepository.findByCreatorId(2L)).hasSize(1);
        assertThat(gameRepository.findByCreatorId(99L)).isEmpty();
    }
}
