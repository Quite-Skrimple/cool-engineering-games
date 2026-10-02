package edu.arizona.engagement.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import edu.arizona.engagement.dto.CommentRequest;
import edu.arizona.engagement.dto.CommentResponse;
import edu.arizona.engagement.model.entity.Comment;
import edu.arizona.engagement.repository.CommentRepository;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public CommentResponse createComment(CommentRequest request) {
        Comment comment = new Comment();
        comment.setUserId(request.getUserId());
        comment.setGameId(request.getGameId());
        comment.setBody(request.getBody());

        return toResponse(commentRepository.save(comment));
    }

    public List<CommentResponse> getCommentsForGame(Long gameId) {
        return commentRepository.findByGameId(gameId).stream()
                .map(this::toResponse)
                .toList();
    }

    public CommentResponse updateComment(Long id, CommentRequest request) {
        Comment comment = findCommentOrThrow(id);
        comment.setBody(request.getBody());
        return toResponse(commentRepository.save(comment));
    }

    public void deleteComment(Long id) {
        if (!commentRepository.existsById(id)) {
            throw new NoSuchElementException("Comment not found: " + id);
        }
        commentRepository.deleteById(id);
    }

    private Comment findCommentOrThrow(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Comment not found: " + id));
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getUserId(),
                comment.getGameId(),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
