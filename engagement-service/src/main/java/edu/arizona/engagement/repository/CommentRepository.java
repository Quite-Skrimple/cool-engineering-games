package edu.arizona.engagement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.arizona.engagement.model.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByGameId(Long gameId);
}
