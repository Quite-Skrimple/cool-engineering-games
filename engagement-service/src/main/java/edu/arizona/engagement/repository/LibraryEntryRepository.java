package edu.arizona.engagement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.arizona.engagement.model.entity.LibraryEntry;

public interface LibraryEntryRepository extends JpaRepository<LibraryEntry, Long> {

    List<LibraryEntry> findByUserId(Long userId);

    boolean existsByUserIdAndGameId(Long userId, Long gameId);

    void deleteByUserIdAndGameId(Long userId, Long gameId);
}
