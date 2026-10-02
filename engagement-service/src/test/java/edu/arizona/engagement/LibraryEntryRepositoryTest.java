package edu.arizona.engagement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import edu.arizona.engagement.model.entity.LibraryEntry;
import edu.arizona.engagement.repository.LibraryEntryRepository;

@DataJpaTest
class LibraryEntryRepositoryTest {

    @Autowired
    private LibraryEntryRepository libraryEntryRepository;

    private LibraryEntry newEntry(Long userId, Long gameId) {
        LibraryEntry entry = new LibraryEntry();
        entry.setUserId(userId);
        entry.setGameId(gameId);
        return entry;
    }

    @Test
    void savesAndFindsEntriesByUserId() {
        libraryEntryRepository.save(newEntry(1L, 1L));
        libraryEntryRepository.save(newEntry(1L, 2L));

        assertThat(libraryEntryRepository.findByUserId(1L)).hasSize(2);
    }

    @Test
    void existsByUserIdAndGameIdReflectsSavedState() {
        libraryEntryRepository.save(newEntry(1L, 1L));

        assertThat(libraryEntryRepository.existsByUserIdAndGameId(1L, 1L)).isTrue();
        assertThat(libraryEntryRepository.existsByUserIdAndGameId(1L, 2L)).isFalse();
    }

    @Test
    void deletesByUserIdAndGameId() {
        libraryEntryRepository.save(newEntry(1L, 1L));

        libraryEntryRepository.deleteByUserIdAndGameId(1L, 1L);

        assertThat(libraryEntryRepository.existsByUserIdAndGameId(1L, 1L)).isFalse();
    }
}
