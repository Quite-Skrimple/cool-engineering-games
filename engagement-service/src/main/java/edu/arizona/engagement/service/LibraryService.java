package edu.arizona.engagement.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.arizona.engagement.dto.LibraryEntryRequest;
import edu.arizona.engagement.dto.LibraryEntryResponse;
import edu.arizona.engagement.exception.ConflictException;
import edu.arizona.engagement.model.entity.LibraryEntry;
import edu.arizona.engagement.repository.LibraryEntryRepository;

@Service
public class LibraryService {

    private final LibraryEntryRepository libraryEntryRepository;

    public LibraryService(LibraryEntryRepository libraryEntryRepository) {
        this.libraryEntryRepository = libraryEntryRepository;
    }

    public LibraryEntryResponse addToLibrary(LibraryEntryRequest request) {
        if (libraryEntryRepository.existsByUserIdAndGameId(request.getUserId(), request.getGameId())) {
            throw new ConflictException(
                    "Game " + request.getGameId() + " is already in user " + request.getUserId() + "'s library");
        }

        LibraryEntry entry = new LibraryEntry();
        entry.setUserId(request.getUserId());
        entry.setGameId(request.getGameId());

        return toResponse(libraryEntryRepository.save(entry));
    }

    public List<LibraryEntryResponse> getLibraryForUser(Long userId) {
        return libraryEntryRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void removeFromLibrary(Long userId, Long gameId) {
        libraryEntryRepository.deleteByUserIdAndGameId(userId, gameId);
    }

    private LibraryEntryResponse toResponse(LibraryEntry entry) {
        return new LibraryEntryResponse(
                entry.getId(),
                entry.getUserId(),
                entry.getGameId(),
                entry.getAddedAt());
    }
}
