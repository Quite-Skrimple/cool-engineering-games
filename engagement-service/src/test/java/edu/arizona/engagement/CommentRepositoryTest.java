package edu.arizona.engagement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import edu.arizona.engagement.model.entity.Comment;
import edu.arizona.engagement.repository.CommentRepository;

@DataJpaTest
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    private Comment newComment(Long userId, Long gameId, String body) {
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setGameId(gameId);
        comment.setBody(body);
        return comment;
    }

    @Test
    void savesAndFindsCommentsByGameId() {
        commentRepository.save(newComment(1L, 1L, "Great logic puzzle!"));
        commentRepository.save(newComment(2L, 1L, "Took me a while to solve."));

        assertThat(commentRepository.findByGameId(1L)).hasSize(2);
        assertThat(commentRepository.findByGameId(2L)).isEmpty();
    }
}
