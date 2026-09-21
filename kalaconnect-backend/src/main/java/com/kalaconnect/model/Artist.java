package com.kalaconnect.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "artists")
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "art_form", nullable = false, length = 100)
    private String artForm;

    @Column(name = "art_form_tamil", length = 100)
    private String artFormTamil;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "experience_years")
    private Integer experienceYears = 0;

    @Column(name = "base_price_min", precision = 10, scale = 2)
    private BigDecimal basePriceMin = BigDecimal.ZERO;

    @Column(name = "base_price_max", precision = 10, scale = 2)
    private BigDecimal basePriceMax = BigDecimal.ZERO;

    /** Latitude of artist's home/base location (for geo-matching) */
    @Column
    private Double latitude;

    /** Longitude of artist's home/base location */
    @Column
    private Double longitude;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String state = "Tamil Nadu";

    @Column(precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(name = "instagram_url", length = 255)
    private String instagramUrl;

    @Column(name = "youtube_url", length = 255)
    private String youtubeUrl;

    @Column(name = "celebrity_link", length = 255)
    private String celebrityLink;

    @Column(name = "teach_online")
    private Boolean teachOnline = false;

    @Column(name = "teach_offline")
    private Boolean teachOffline = false;

    @Column(name = "teach_price", precision = 10, scale = 2)
    private BigDecimal teachPrice;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── No-arg constructor ───────────────────────────────────
    public Artist() {}

    // ─── Getters ──────────────────────────────────────────────
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getArtForm() { return artForm; }
    public String getArtFormTamil() { return artFormTamil; }
    public String getBio() { return bio; }
    public Integer getExperienceYears() { return experienceYears; }
    public BigDecimal getBasePriceMin() { return basePriceMin; }
    public BigDecimal getBasePriceMax() { return basePriceMax; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getCity() { return city; }
    public String getDistrict() { return district; }
    public String getState() { return state; }
    public BigDecimal getRating() { return rating; }
    public Integer getReviewCount() { return reviewCount; }
    public Boolean getIsAvailable() { return isAvailable; }
    public String getInstagramUrl() { return instagramUrl; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public String getCelebrityLink() { return celebrityLink; }
    public Boolean getTeachOnline() { return teachOnline; }
    public Boolean getTeachOffline() { return teachOffline; }
    public BigDecimal getTeachPrice() { return teachPrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ─── Setters ──────────────────────────────────────────────
    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setArtForm(String artForm) { this.artForm = artForm; }
    public void setArtFormTamil(String artFormTamil) { this.artFormTamil = artFormTamil; }
    public void setBio(String bio) { this.bio = bio; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }
    public void setBasePriceMin(BigDecimal basePriceMin) { this.basePriceMin = basePriceMin; }
    public void setBasePriceMax(BigDecimal basePriceMax) { this.basePriceMax = basePriceMax; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setCity(String city) { this.city = city; }
    public void setDistrict(String district) { this.district = district; }
    public void setState(String state) { this.state = state; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
    public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }
    public void setCelebrityLink(String celebrityLink) { this.celebrityLink = celebrityLink; }
    public void setTeachOnline(Boolean teachOnline) { this.teachOnline = teachOnline; }
    public void setTeachOffline(Boolean teachOffline) { this.teachOffline = teachOffline; }
    public void setTeachPrice(BigDecimal teachPrice) { this.teachPrice = teachPrice; }
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
}