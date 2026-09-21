package com.kalaconnect.controller;

import com.kalaconnect.model.Artist;
import com.kalaconnect.model.Booking;
import com.kalaconnect.model.Booking.BookingStatus;
import com.kalaconnect.model.Event;
import com.kalaconnect.repository.ArtistRepository;
import com.kalaconnect.repository.BookingRepository;
import com.kalaconnect.repository.EventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final ArtistRepository artistRepository;


    // ============================================================
    // POST /api/bookings/request
    //
    // Customer sends a booking request
    // Creates Event + Booking in MySQL
    // ============================================================

    @PostMapping("/request")
    public ResponseEntity<?> requestBooking(
            @Valid @RequestBody BookingRequestDTO req) {

        log.info(
                "New booking request: artistId={} eventType={} customer={}",
                req.artistId(),
                req.eventType(),
                req.customerName()
        );


        // --------------------------------------------------------
        // 1. Check whether artist exists
        // --------------------------------------------------------

        Artist artist = artistRepository
                .findById(req.artistId())
                .orElse(null);

        if (artist == null) {
            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    "Artist not found"
                            )
                    );
        }


        // --------------------------------------------------------
        // 2. Parse event date
        // --------------------------------------------------------

        LocalDate eventDate;

        try {

            eventDate = parseDate(req.eventDate());

        } catch (Exception e) {

            log.error(
                    "Invalid event date received: {}",
                    req.eventDate()
            );

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    "Invalid date format. Please use a valid date such as 2026-09-25, 25-09-2026, 25/09/2026, 25 Sep 2026, or 25 September 2026"
                            )
                    );
        }


        // --------------------------------------------------------
        // 3. Create Event
        // --------------------------------------------------------

        Event event = new Event();

        event.setEventType(req.eventType());

        event.setEventLabel(req.eventLabel());

        event.setEventDate(eventDate);

        event.setLocationName(req.locationName());

        event.setBudgetMin(
                BigDecimal.valueOf(req.budgetMin())
        );

        event.setBudgetMax(
                BigDecimal.valueOf(req.budgetMax())
        );

        event.setCustomerName(
                req.customerName()
        );

        event.setCustomerPhone(
                req.customerPhone()
        );

        event.setNotes(
                req.customerNotes()
        );

        event.setStatus(
                Event.EventStatus.SEARCHING
        );

        eventRepository.save(event);


        // --------------------------------------------------------
        // 4. Create Booking
        // --------------------------------------------------------

        Booking booking = new Booking();

        booking.setEvent(event);

        booking.setArtist(artist);

        booking.setStatus(
                BookingStatus.PENDING
        );

        // Initial offer = customer's maximum budget
        booking.setQuotedPrice(
                BigDecimal.valueOf(req.budgetMax())
        );

        booking.setCustomerNotes(
                req.customerNotes()
        );

        bookingRepository.save(booking);


        log.info(
                "Booking created: id={} artist={} status=PENDING",
                booking.getId(),
                artist.getUser() != null
                        ? artist.getUser().getName()
                        : "?"
        );


        // --------------------------------------------------------
        // 5. Check button-phone artist
        // --------------------------------------------------------

        boolean ivrTriggered = false;

        if (
                artist.getUser() != null &&
                artist.getUser().getDeviceType()
                        == com.kalaconnect.model.User.DeviceType.BUTTON_PHONE
        ) {

            ivrTriggered = true;

            /*
             * TODO:
             * Connect Twilio IVR here.
             *
             * Example:
             *
             * String callSid =
             *     ivrService.triggerBookingCall(
             *         artist.getUser().getPhone(),
             *         booking
             *     );
             *
             * booking.setIvrCallSid(callSid);
             * bookingRepository.save(booking);
             */

            log.info(
                    "IVR would be triggered for button-phone artist: {}",
                    artist.getUser().getPhone()
            );
        }


        // --------------------------------------------------------
        // 6. Return response to mobile application
        // --------------------------------------------------------

        String artistName =
                artist.getUser() != null
                        ? artist.getUser().getName()
                        : "Artist";

        return ResponseEntity.ok(
                Map.of(
                        "bookingId",
                        booking.getId(),

                        "status",
                        "PENDING",

                        "artistName",
                        artistName,

                        "eventType",
                        req.eventType(),

                        "message",
                        "Booking request sent successfully to "
                                + artistName,

                        "ivrTriggered",
                        ivrTriggered
                )
        );
    }


    // ============================================================
    // PUT /api/bookings/{id}/respond
    //
    // Artist can:
    // ACCEPT
    // REJECT
    // NEGOTIATING
    // ============================================================

    @PutMapping("/{id}/respond")
    public ResponseEntity<?> respondToBooking(
            @PathVariable Long id,
            @Valid @RequestBody RespondDTO req) {

        log.info(
                "Artist responding to bookingId={} with status={}",
                id,
                req.status()
        );


        return bookingRepository
                .findById(id)

                .map(booking -> {

                    BookingStatus newStatus;

                    try {

                        newStatus =
                                BookingStatus.valueOf(
                                        req.status().toUpperCase()
                                );

                    } catch (IllegalArgumentException e) {

                        return ResponseEntity
                                .badRequest()
                                .<Object>body(
                                        Map.of(
                                                "message",
                                                "Invalid status. Use ACCEPTED, REJECTED, or NEGOTIATING"
                                        )
                                );
                    }


                    // Update booking status
                    booking.setStatus(newStatus);


                    // Save artist message/notes
                    booking.setArtistNotes(
                            req.artistNotes()
                    );


                    // Save negotiated price
                    if (
                            req.negotiatedPrice() != null &&
                            req.negotiatedPrice() > 0
                    ) {

                        booking.setNegotiatedPrice(
                                BigDecimal.valueOf(
                                        req.negotiatedPrice()
                                )
                        );
                    }


                    bookingRepository.save(booking);


                    log.info(
                            "Booking {} updated to {}",
                            id,
                            newStatus
                    );


                    return ResponseEntity.ok(
                            Map.of(
                                    "bookingId",
                                    id,

                                    "newStatus",
                                    newStatus.name(),

                                    "message",
                                    "Booking updated to "
                                            + newStatus.name()
                                            + " successfully"
                            )
                    );
                })

                .orElse(
                        ResponseEntity
                                .notFound()
                                .<Object>build()
                );
    }


    // ============================================================
    // GET /api/bookings/artist/{artistId}
    //
    // Returns bookings for artist dashboard
    // ============================================================

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<?> getArtistBookings(
            @PathVariable Long artistId) {

        List<Booking> bookings =
                bookingRepository
                        .findByArtistIdOrderByCreatedAtDesc(
                                artistId
                        );


        List<Map<String, Object>> response =
                bookings.stream()
                        .map(b -> {

                            Event e = b.getEvent();

                            Map<String, Object> m =
                                    new java.util.LinkedHashMap<>();


                            m.put(
                                    "id",
                                    b.getId()
                            );

                            m.put(
                                    "customerName",
                                    e.getCustomerName()
                            );

                            m.put(
                                    "customerPhone",
                                    e.getCustomerPhone()
                            );

                            m.put(
                                    "eventType",
                                    e.getEventType()
                            );

                            m.put(
                                    "eventLabel",
                                    e.getEventLabel()
                            );

                            m.put(
                                    "eventDate",
                                    e.getEventDate().toString()
                            );

                            m.put(
                                    "locationName",
                                    e.getLocationName()
                            );

                            m.put(
                                    "budgetMin",
                                    e.getBudgetMin()
                            );

                            m.put(
                                    "budgetMax",
                                    e.getBudgetMax()
                            );

                            m.put(
                                    "status",
                                    b.getStatus().name()
                            );

                            m.put(
                                    "negotiatedPrice",
                                    b.getNegotiatedPrice()
                            );

                            m.put(
                                    "customerNotes",
                                    e.getNotes()
                            );

                            m.put(
                                    "artistNotes",
                                    b.getArtistNotes()
                            );

                            m.put(
                                    "createdAt",
                                    b.getCreatedAt().toString()
                            );


                            return m;
                        })
                        .toList();


        return ResponseEntity.ok(
                Map.of(
                        "bookings",
                        response,

                        "count",
                        response.size()
                )
        );
    }


    // ============================================================
    // GET /api/bookings/customer/phone/{phone}
    //
    // Returns bookings for customer
    // ============================================================

    @GetMapping("/customer/phone/{phone}")
    public ResponseEntity<?> getCustomerBookingsByPhone(
            @PathVariable String phone) {

        List<Booking> bookings =
                bookingRepository
                        .findByCustomerPhoneOrderByCreatedAtDesc(
                                phone
                        );


        List<Map<String, Object>> response =
                bookings.stream()
                        .map(b -> {

                            Event e = b.getEvent();

                            Map<String, Object> m =
                                    new java.util.LinkedHashMap<>();


                            // ------------------------------------------------
                            // Booking ID
                            // ------------------------------------------------

                            m.put(
                                    "id",
                                    b.getId()
                            );


                            // ------------------------------------------------
                            // Customer information
                            // ------------------------------------------------

                            m.put(
                                    "customerName",
                                    e.getCustomerName()
                            );

                            m.put(
                                    "customerPhone",
                                    e.getCustomerPhone()
                            );


                            // ------------------------------------------------
                            // Artist information
                            // ------------------------------------------------

                            String artistName = "Artist";

                            if (
                                    b.getArtist() != null &&
                                    b.getArtist().getUser() != null
                            ) {

                                artistName =
                                        b.getArtist()
                                                .getUser()
                                                .getName();
                            }

                            m.put(
                                    "artistName",
                                    artistName
                            );


                            // ------------------------------------------------
                            // Event information
                            // ------------------------------------------------

                            m.put(
                                    "eventType",
                                    e.getEventType()
                            );

                            m.put(
                                    "eventLabel",
                                    e.getEventLabel()
                            );

                            m.put(
                                    "eventDate",
                                    e.getEventDate().toString()
                            );

                            m.put(
                                    "locationName",
                                    e.getLocationName()
                            );


                            // ------------------------------------------------
                            // Budget
                            // ------------------------------------------------

                            m.put(
                                    "budgetMin",
                                    e.getBudgetMin()
                            );

                            m.put(
                                    "budgetMax",
                                    e.getBudgetMax()
                            );


                            // ------------------------------------------------
                            // Booking status
                            // ------------------------------------------------

                            m.put(
                                    "status",
                                    b.getStatus().name()
                            );


                            // ------------------------------------------------
                            // Negotiation information
                            // ------------------------------------------------

                            m.put(
                                    "negotiatedPrice",
                                    b.getNegotiatedPrice()
                            );


                            // ------------------------------------------------
                            // Customer notes
                            // ------------------------------------------------

                            m.put(
                                    "customerNotes",
                                    e.getNotes()
                            );


                            // ------------------------------------------------
                            // Artist response/message
                            // ------------------------------------------------

                            m.put(
                                    "artistNotes",
                                    b.getArtistNotes()
                            );


                            // ------------------------------------------------
                            // Booking creation time
                            // ------------------------------------------------

                            m.put(
                                    "createdAt",
                                    b.getCreatedAt().toString()
                            );


                            return m;
                        })
                        .toList();


        return ResponseEntity.ok(
                Map.of(
                        "bookings",
                        response,

                        "count",
                        response.size()
                )
        );
    }


    // ============================================================
    // DATE PARSER
    //
    // Accepts multiple date formats from React Native
    // ============================================================

    private LocalDate parseDate(String dateStr) {

        if (dateStr == null || dateStr.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Date cannot be empty"
            );
        }


        String value = dateStr.trim();


        // --------------------------------------------------------
        // Format 1:
        // 2026-09-25
        // --------------------------------------------------------

        try {

            return LocalDate.parse(
                    value,
                    DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd"
                    )
            );

        } catch (DateTimeParseException ignored) {
        }


        // --------------------------------------------------------
        // Format 2:
        // 25 Sep 2026
        // --------------------------------------------------------

        try {

            return LocalDate.parse(
                    value,
                    DateTimeFormatter.ofPattern(
                            "d MMM yyyy"
                    )
            );

        } catch (DateTimeParseException ignored) {
        }


        // --------------------------------------------------------
        // Format 3:
        // 25 September 2026
        // --------------------------------------------------------

        try {

            return LocalDate.parse(
                    value,
                    DateTimeFormatter.ofPattern(
                            "d MMMM yyyy"
                    )
            );

        } catch (DateTimeParseException ignored) {
        }


        // --------------------------------------------------------
        // Format 4:
        // 25-09-2026
        // --------------------------------------------------------

        try {

            return LocalDate.parse(
                    value,
                    DateTimeFormatter.ofPattern(
                            "dd-MM-yyyy"
                    )
            );

        } catch (DateTimeParseException ignored) {
        }


        // --------------------------------------------------------
        // Format 5:
        // 25/09/2026
        // --------------------------------------------------------

        try {

            return LocalDate.parse(
                    value,
                    DateTimeFormatter.ofPattern(
                            "dd/MM/yyyy"
                    )
            );

        } catch (DateTimeParseException ignored) {
        }


        // --------------------------------------------------------
        // Format 6:
        // 2026/09/25
        // --------------------------------------------------------

        try {

            return LocalDate.parse(
                    value,
                    DateTimeFormatter.ofPattern(
                            "yyyy/MM/dd"
                    )
            );

        } catch (DateTimeParseException ignored) {
        }


        // --------------------------------------------------------
        // Nothing matched
        // --------------------------------------------------------

        throw new IllegalArgumentException(
                "Cannot parse date: " + dateStr
        );
    }


    // ============================================================
    // BOOKING REQUEST DTO
    // ============================================================

    public record BookingRequestDTO(

            @NotNull
            Long artistId,

            @NotBlank
            String eventType,

            String eventLabel,

            @NotBlank
            String eventDate,

            @NotBlank
            String locationName,

            @Positive
            double budgetMin,

            @Positive
            double budgetMax,

            @NotBlank
            String customerName,

            @NotBlank
            String customerPhone,

            String customerNotes

    ) {}


    // ============================================================
    // ARTIST RESPONSE DTO
    // ============================================================

    public record RespondDTO(

            @NotBlank
            String status,

            String artistNotes,

            Double negotiatedPrice

    ) {}
}