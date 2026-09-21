package com.kalaconnect.repository;

import com.kalaconnect.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * All bookings for a given artist, newest first.
     * Used by Artist Dashboard to show incoming requests.
     */
    List<Booking> findByArtistIdOrderByCreatedAtDesc(Long artistId);

    /**
     * All bookings placed by a customer (matched by phone number in Event).
     * Used by Customer "My Bookings" screen.
     */
    @Query("SELECT b FROM Booking b WHERE b.event.customerPhone = :phone ORDER BY b.createdAt DESC")
    List<Booking> findByCustomerPhoneOrderByCreatedAtDesc(@Param("phone") String phone);

    /**
     * Count pending bookings for an artist (for dashboard badge).
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.artist.id = :artistId AND b.status = 'PENDING'")
    long countPendingByArtistId(@Param("artistId") Long artistId);
}
