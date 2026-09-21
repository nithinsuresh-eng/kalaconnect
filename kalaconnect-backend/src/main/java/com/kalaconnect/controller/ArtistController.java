package com.kalaconnect.controller;

import com.kalaconnect.model.Artist;
import com.kalaconnect.model.Event;
import com.kalaconnect.repository.ArtistRepository;
import com.kalaconnect.service.ArtistMatchingService;
import com.kalaconnect.service.ArtistMatchingService.RankedArtist;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ArtistController {

    private final ArtistRepository artistRepository;
    private final ArtistMatchingService matchingService;

    /**
     * GET /api/artists/{id}
     * Returns a single artist profile.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getArtist(@PathVariable Long id) {
        return artistRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/artists
     * Returns all artists (for browsing / admin).
     */
    @GetMapping
    public ResponseEntity<List<Artist>> getAllArtists() {
        return ResponseEntity.ok(artistRepository.findAll());
    }

    /**
     * POST /api/artists/search
     * Main AI-powered artist search endpoint.
     *
     * Request body:
     * {
     *   "eventType": "wedding",
     *   "latitude": 9.9252,
     *   "longitude": 78.1198,
     *   "budgetMin": 5000,
     *   "budgetMax": 15000
     * }
     *
     * Response: ranked list of matched artists with match score.
     */
    @PostMapping("/search")
    public ResponseEntity<?> searchArtists(@Valid @RequestBody ArtistSearchRequest req) {
        List<RankedArtist> results = matchingService.findMatchingArtists(
                req.eventType(),
                req.latitude(),
                req.longitude(),
                BigDecimal.valueOf(req.budgetMin()),
                BigDecimal.valueOf(req.budgetMax())
        );

        if (results.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "artists", List.of(),
                    "message", "No artists found. Try increasing budget range or expanding location."
            ));
        }

        List<ArtistSearchResponse> response = results.stream()
                .map(r -> new ArtistSearchResponse(r.artist(), r.matchScore()))
                .toList();

        return ResponseEntity.ok(Map.of(
                "artists", response,
                "count", response.size(),
                "message", "AI matched " + response.size() + " artists"
        ));
    }

    // ─── DTOs ────────────────────────────────────────────────

    public record ArtistSearchRequest(
            @NotBlank String eventType,
            @NotNull double latitude,
            @NotNull double longitude,
            @Positive double budgetMin,
            @Positive double budgetMax
    ) {}

    public record ArtistSearchResponse(Artist artist, int matchScore) {}
}
