package com.kalaconnect.repository;

import com.kalaconnect.model.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {

    /**
     * Haversine formula — finds artists within a given radius (km) of a lat/long point.
     * Filters by budget overlap and availability.
     *
     * Formula: 6371 * acos(cos(radians(lat1)) * cos(radians(lat2))
     *              * cos(radians(lon2) - radians(lon1))
     *              + sin(radians(lat1)) * sin(radians(lat2))) <= radius
     */
    @Query(value = """
        SELECT a.* FROM artists a
        JOIN users u ON a.user_id = u.id
        WHERE a.is_available = TRUE
          AND u.is_active = TRUE
          AND a.base_price_min <= :budgetMax
          AND a.base_price_max >= :budgetMin
          AND (
            6371 * acos(
              GREATEST(-1, LEAST(1,
                cos(radians(:lat)) * cos(radians(a.latitude))
                * cos(radians(a.longitude) - radians(:lon))
                + sin(radians(:lat)) * sin(radians(a.latitude))
              ))
            )
          ) <= :radiusKm
        ORDER BY a.rating DESC, a.experience_years DESC
        """,
        nativeQuery = true)
    List<Artist> findNearbyArtistsByBudget(
            @Param("lat")        double lat,
            @Param("lon")        double lon,
            @Param("radiusKm")   double radiusKm,
            @Param("budgetMin")  BigDecimal budgetMin,
            @Param("budgetMax")  BigDecimal budgetMax
    );

    /**
     * Find artists by art form (for recommendation engine output).
     */
    @Query("SELECT a FROM Artist a WHERE LOWER(a.artForm) LIKE LOWER(CONCAT('%', :artForm, '%')) AND a.isAvailable = true")
    List<Artist> findByArtFormContaining(@Param("artForm") String artForm);

    /** Artists registered as button-phone users — used by IVR trigger logic. */
    @Query("SELECT a FROM Artist a JOIN a.user u WHERE u.deviceType = 'BUTTON_PHONE' AND a.id = :id")
    java.util.Optional<Artist> findButtonPhoneArtistById(@Param("id") Long id);
}
