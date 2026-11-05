package edu.arizona.engagement.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.arizona.engagement.dto.CommentRequest;
import edu.arizona.engagement.dto.CommentResponse;
import edu.arizona.engagement.dto.CommentUpdateRequest;
import edu.arizona.engagement.service.CommentService;
import edu.arizona.engagement.service.Paging;
import edu.arizona.security.CurrentUser;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /** The author is the caller, taken from the token. */
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(CurrentUser.id(), request));
    }

    /** Newest first, 50 per page by default (at most 100). Ask for further pages with {@code page=1,2,...}. */
    @GetMapping
    public List<CommentResponse> getCommentsForGame(@RequestParam Long gameId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = Paging.DEFAULT_SIZE) int size) {
        return commentService.getCommentsForGame(gameId, page, size);
    }

    @PutMapping("/{id}")
    public CommentResponse updateComment(@PathVariable Long id, @Valid @RequestBody CommentUpdateRequest request) {
        return commentService.updateComment(id, request, CurrentUser.id());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id, CurrentUser.id(), CurrentUser.isAdmin());
        return ResponseEntity.noContent().build();
    }
}
