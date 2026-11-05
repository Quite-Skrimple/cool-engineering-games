package edu.arizona.engagement.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.arizona.engagement.dto.LibraryEntryRequest;
import edu.arizona.engagement.dto.LibraryEntryResponse;
import edu.arizona.engagement.service.LibraryService;
import edu.arizona.security.CurrentUser;
import jakarta.validation.Valid;

/** A user's saved games. Always the caller's own library: there is no way to name another user. */
@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @PostMapping
    public ResponseEntity<LibraryEntryResponse> addToLibrary(@Valid @RequestBody LibraryEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(libraryService.addToLibrary(CurrentUser.id(), request));
    }

    @GetMapping
    public List<LibraryEntryResponse> getMyLibrary() {
        return libraryService.getLibraryForUser(CurrentUser.id());
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> removeFromLibrary(@PathVariable Long gameId) {
        libraryService.removeFromLibrary(CurrentUser.id(), gameId);
        return ResponseEntity.noContent().build();
    }
}
