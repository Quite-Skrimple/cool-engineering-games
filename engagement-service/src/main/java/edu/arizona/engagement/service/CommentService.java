package edu.arizona.engagement.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import edu.arizona.engagement.config.GameCatalogClient;
import edu.arizona.engagement.dto.CommentRequest;
import edu.arizona.engagement.dto.CommentResponse;
import edu.arizona.engagement.dto.CommentUpdateRequest;
import edu.arizona.engagement.model.entity.Comment;
import edu.arizona.engagement.repository.CommentRepository;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final GameCatalogClient catalogClient;

    public CommentService(CommentRepository commentRepository, GameCatalogClient catalogClient) {
        this.commentRepository = commentRepository;
        this.catalogClient = catalogClient;
    }

    public CommentResponse createComment(Long userId, CommentRequest request) {
        catalogClient.requireVisibleGame(request.getGameId());

        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setGameId(request.getGameId());
        comment.setBody(request.getBody());

        return toResponse(commentRepository.save(comment));
    }

    public List<CommentResponse> getCommentsForGame(Long gameId, int page, int size) {
        return commentRepository.findByGameId(gameId, Paging.newestFirst(page, size)).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Only the author can edit a comment. */
    public CommentResponse updateComment(Long id, CommentUpdateRequest request, Long callerId) {
        Comment comment = findCommentOrThrow(id);
        if (!comment.getUserId().equals(callerId)) {
            throw new AccessDeniedException("You can only edit your own comments");
        }
        comment.setBody(request.getBody());
        return toResponse(commentRepository.save(comment));
    }

    /** The author or an administrator (moderation) can delete a comment. */
    public void deleteComment(Long id, Long callerId, boolean admin) {
        Comment comment = findCommentOrThrow(id);
        if (!admin && !comment.getUserId().equals(callerId)) {
            throw new AccessDeniedException("You can only delete your own comments");
        }
        commentRepository.delete(comment);
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
