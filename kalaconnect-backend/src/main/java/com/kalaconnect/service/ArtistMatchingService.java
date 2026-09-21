package com.kalaconnect.service;

import com.kalaconnect.model.Artist;
import com.kalaconnect.repository.ArtistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * AI Artist Matching Service
 *
 * Algorithm:
 * 1. Rule-based recommendation: maps event type → preferred art forms
 * 2. Geo-radius filtering (Haversine): starts at initialRadiusKm, expands if no results
 * 3. Ranking: scores each artist on availability (40%), rating (30%), experience (20%), budget fit (10%)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArtistMatchingService {

    private final ArtistRepository artistRepository;

    @Value("${kalaconnect.geo.initial-radius-km:50}")
    private double initialRadiusKm;

    @Value("${kalaconnect.geo.max-radius-km:300}")
    private double maxRadiusKm;

    @Value("${kalaconnect.geo.radius-expansion-step-km:25}")
    private double radiusExpansionStep;

    // AI Recommendation Map: event type → preferred art forms (ordered by suitability)
    private static final Map<String, List<String>> RECOMMENDATION_MAP = Map.of(
        "wedding",          List.of("Nadaswaram", "Karagattam", "Kolattam", "Villu Paattu"),
        "temple_festival",  List.of("Therukoothu", "Parai", "Nadaswaram", "Kavadi Attam"),
        "village_festival", List.of("Karagattam", "Therukoothu", "Parai", "Oyilattam"),
        "corporate",        List.of("Bharatanatyam", "Silambam", "Villu Paattu"),
        "education",        List.of("Kolattam", "Silambam", "Puppet Show"),
        "cultural",         List.of("Therukoothu", "Karagattam", "Nadaswaram", "Parai"),
        "birthday",         List.of("Karagattam", "Puppet Show", "Kolattam"),
        "tourism",          List.of("Bharatanatyam", "Silambam", "Karagattam", "Nadaswaram")
    );

    /**
     * Main entry point: finds and ranks artists for a given event request.
     *
     * @param eventType   e.g. "wedding"
     * @param lat         event location latitude
     * @param lon         event location longitude
     * @param budgetMin   minimum customer budget
     * @param budgetMax   maximum customer budget
     * @return ranked list of matched artists with scores
     */
    public List<RankedArtist> findMatchingArtists(
            String eventType, double lat, double lon,
            BigDecimal budgetMin, BigDecimal budgetMax) {

        // Step 1: get AI-recommended art forms for this event type
        List<String> recommendedForms = RECOMMENDATION_MAP.getOrDefault(
                eventType, Collections.emptyList());
        log.info("AI recommends for '{}': {}", eventType, recommendedForms);

        // Step 2: geo-radius search with expansion
        List<Artist> candidates = new ArrayList<>();
        double currentRadius = initialRadiusKm;

        while (candidates.isEmpty() && currentRadius <= maxRadiusKm) {
            log.info("Searching artists within {}km radius...", currentRadius);
            candidates = artistRepository.findNearbyArtistsByBudget(
                    lat, lon, currentRadius, budgetMin, budgetMax);
            if (candidates.isEmpty()) {
                currentRadius += radiusExpansionStep;
                log.info("No artists found. Expanding radius to {}km", currentRadius);
            }
        }

        // Step 3: rank each candidate
        List<RankedArtist> ranked = new ArrayList<>();
        for (Artist artist : candidates) {
            double score = calculateMatchScore(artist, recommendedForms, lat, lon, budgetMin, budgetMax);
            ranked.add(new RankedArtist(artist, (int) Math.round(score)));
        }

        // Sort by score descending
        ranked.sort(Comparator.comparingInt(RankedArtist::matchScore).reversed());
        log.info("Returning {} ranked artists", ranked.size());
        return ranked;
    }

    /**
     * Composite match score (0–100):
     * - Art form relevance to event type  : 40 pts
     * - Artist rating (normalized to 5)   : 30 pts
     * - Experience (capped at 30 years)   : 20 pts
     * - Budget fit                        : 10 pts
     */
    private double calculateMatchScore(
            Artist artist, List<String> recommendedForms,
            double eventLat, double eventLon,
            BigDecimal budgetMin, BigDecimal budgetMax) {

        double score = 0;

        // Art form relevance (40 pts)
        int formIndex = findFormIndex(artist.getArtForm(), recommendedForms);
        if (formIndex == 0)      score += 40;
        else if (formIndex == 1) score += 30;
        else if (formIndex == 2) score += 20;
        else if (formIndex > 2)  score += 10;

        // Rating (30 pts): rating/5 * 30
        double ratingScore = (artist.getRating().doubleValue() / 5.0) * 30;
        score += ratingScore;

        // Experience (20 pts): min(exp, 30) / 30 * 20
        double expScore = (Math.min(artist.getExperienceYears(), 30) / 30.0) * 20;
        score += expScore;

        // Budget fit (10 pts): if artist's max <= customer's max → full pts; else proportional
        if (artist.getBasePriceMax().compareTo(budgetMax) <= 0) {
            score += 10;
        } else {
            double budgetRatio = budgetMax.doubleValue() / artist.getBasePriceMax().doubleValue();
            score += budgetRatio * 10;
        }

        return Math.min(score, 100);
    }

    private int findFormIndex(String artForm, List<String> forms) {
        for (int i = 0; i < forms.size(); i++) {
            if (forms.get(i).equalsIgnoreCase(artForm)) return i;
        }
        return -1; // not in recommended list
    }

    public record RankedArtist(Artist artist, int matchScore) {}
}
