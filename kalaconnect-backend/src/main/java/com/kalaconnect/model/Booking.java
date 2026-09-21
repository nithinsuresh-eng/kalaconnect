package com.kalaconnect.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "bookings",
    uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "artist_id"})
)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "quoted_price", precision = 10, scale = 2)
    private BigDecimal quotedPrice;

    @Column(name = "negotiated_price", precision = 10, scale = 2)
    private BigDecimal negotiatedPrice;

    /** Twilio call SID — populated when IVR call is placed to button-phone artist */
    @Column(name = "ivr_call_sid", length = 100)
    private String ivrCallSid;

    /** Artist's voice response via IVR: ACCEPT | REJECT | NEGOTIATE */
    @Column(name = "ivr_response", length = 20)
    private String ivrResponse;

    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    @Column(name = "artist_notes", columnDefinition = "TEXT")
    private String artistNotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── No-arg constructor ───────────────────────────────────
    public Booking() {}

    // ─── Getters ──────────────────────────────────────────────
    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public Artist getArtist() { return artist; }
    public BookingStatus getStatus() { return status; }
    public BigDecimal getQuotedPrice() { return quotedPrice; }
    public BigDecimal getNegotiatedPrice() { return negotiatedPrice; }
    public String getIvrCallSid() { return ivrCallSid; }
    public String getIvrResponse() { return ivrResponse; }
    public String getCustomerNotes() { return customerNotes; }
    public String getArtistNotes() { return artistNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ─── Setters ──────────────────────────────────────────────
    public void setId(Long id) { this.id = id; }
    public void setEvent(Event event) { this.event = event; }
    public void setArtist(Artist artist) { this.artist = artist; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public void setQuotedPrice(BigDecimal quotedPrice) { this.quotedPrice = quotedPrice; }
    public void setNegotiatedPrice(BigDecimal negotiatedPrice) { this.negotiatedPrice = negotiatedPrice; }
    public void setIvrCallSid(String ivrCallSid) { this.ivrCallSid = ivrCallSid; }
    public void setIvrResponse(String ivrResponse) { this.ivrResponse = ivrResponse; }
    public void setCustomerNotes(String customerNotes) { this.customerNotes = customerNotes; }
    public void setArtistNotes(String artistNotes) { this.artistNotes = artistNotes; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum BookingStatus {
        PENDING, ACCEPTED, REJECTED, NEGOTIATING, COMPLETED, CANCELLED
    }
}
