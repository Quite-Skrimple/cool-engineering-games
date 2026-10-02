package edu.arizona.engagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.arizona.engagement.model.entity.Reaction;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    List<Reaction> findByGameId(Long gameId);

    Optional<Reaction> findByUserIdAndGameId(Long userId, Long gameId);

    boolean existsByUserIdAndGameId(Long userId, Long gameId);
}
