package edu.arizona.engagement.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.arizona.engagement.config.GameCatalogClient;
import edu.arizona.engagement.dto.LibraryEntryRequest;
import edu.arizona.engagement.dto.LibraryEntryResponse;
import edu.arizona.engagement.exception.ConflictException;
import edu.arizona.engagement.model.entity.LibraryEntry;
import edu.arizona.engagement.repository.LibraryEntryRepository;

@Service
public class LibraryService {

    private final LibraryEntryRepository libraryEntryRepository;
    private final GameCatalogClient catalogClient;

    public LibraryService(LibraryEntryRepository libraryEntryRepository, GameCatalogClient catalogClient) {
        this.libraryEntryRepository = libraryEntryRepository;
        this.catalogClient = catalogClient;
    }

    public LibraryEntryResponse addToLibrary(Long userId, LibraryEntryRequest request) {
        catalogClient.requireVisibleGame(request.getGameId());

        if (libraryEntryRepository.existsByUserIdAndGameId(userId, request.getGameId())) {
            throw alreadyInLibrary(request.getGameId());
        }

        LibraryEntry entry = new LibraryEntry();
        entry.setUserId(userId);
        entry.setGameId(request.getGameId());

        try {
            return toResponse(libraryEntryRepository.save(entry));
        } catch (DataIntegrityViolationException e) {
            throw alreadyInLibrary(request.getGameId());
        }
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

    private ConflictException alreadyInLibrary(Long gameId) {
        return new ConflictException("Game " + gameId + " is already in your library");
    }

    private LibraryEntryResponse toResponse(LibraryEntry entry) {
        return new LibraryEntryResponse(
                entry.getId(),
                entry.getUserId(),
                entry.getGameId(),
                entry.getAddedAt());
    }
}
