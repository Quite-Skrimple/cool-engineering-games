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
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @PostMapping
    public ResponseEntity<LibraryEntryResponse> addToLibrary(@Valid @RequestBody LibraryEntryRequest request) {
        LibraryEntryResponse response = libraryService.addToLibrary(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}")
    public List<LibraryEntryResponse> getLibraryForUser(@PathVariable Long userId) {
        return libraryService.getLibraryForUser(userId);
    }

    @DeleteMapping("/{userId}/{gameId}")
    public ResponseEntity<Void> removeFromLibrary(@PathVariable Long userId, @PathVariable Long gameId) {
        libraryService.removeFromLibrary(userId, gameId);
        return ResponseEntity.noContent().build();
    }
}
