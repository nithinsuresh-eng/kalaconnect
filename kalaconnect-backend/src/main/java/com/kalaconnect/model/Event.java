package com.kalaconnect.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Customer details (stored directly for non-authenticated bookings) ──
    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 15)
    private String customerPhone;

    // ── Event details ──────────────────────────────────────────────────────
    /** Event type key: wedding, temple_festival, village_festival, etc. */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /** Human-readable label: "💍 Marriage / Wedding" */
    @Column(name = "event_label", length = 120)
    private String eventLabel;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "location_name", nullable = false, length = 255)
    private String locationName;

    /** Customer's event location latitude (for geo-matching) */
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "budget_min", nullable = false, precision = 10, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", nullable = false, precision = 10, scale = 2)
    private BigDecimal budgetMax;

    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.SEARCHING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ─── No-arg constructor ───────────────────────────────────
    public Event() {}

    // ─── Getters ──────────────────────────────────────────────
    public Long getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public String getEventType() { return eventType; }
    public String getEventLabel() { return eventLabel; }
    public LocalDate getEventDate() { return eventDate; }
    public String getLocationName() { return locationName; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public BigDecimal getBudgetMin() { return budgetMin; }
    public BigDecimal getBudgetMax() { return budgetMax; }
    public EventStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ─── Setters ──────────────────────────────────────────────
    public void setId(Long id) { this.id = id; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setEventLabel(String eventLabel) { this.eventLabel = eventLabel; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setBudgetMin(BigDecimal budgetMin) { this.budgetMin = budgetMin; }
    public void setBudgetMax(BigDecimal budgetMax) { this.budgetMax = budgetMax; }
    public void setStatus(EventStatus status) { this.status = status; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum EventStatus {
        SEARCHING, BOOKED, CANCELLED, COMPLETED
    }
}