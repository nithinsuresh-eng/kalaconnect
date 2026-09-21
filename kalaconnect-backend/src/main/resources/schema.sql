-- ============================================================
--  KalaConnect Database Schema
--  MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS kalaconnect_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE kalaconnect_db;

-- ─── 1. USERS ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100)  NOT NULL,
  email         VARCHAR(150)  NOT NULL UNIQUE,
  password_hash VARCHAR(255)  NOT NULL,
  phone         VARCHAR(15)   NOT NULL,
  role          ENUM('CUSTOMER', 'ARTIST') NOT NULL,
  device_type   ENUM('SMARTPHONE', 'BUTTON_PHONE') DEFAULT 'SMARTPHONE',
  preferred_lang VARCHAR(20)  DEFAULT 'ta',   -- ISO 639-1: ta=Tamil, en=English
  is_active     BOOLEAN       DEFAULT TRUE,
  created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_email (email),
  INDEX idx_role  (role)
);

-- ─── 2. ARTISTS ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS artists (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id           BIGINT        NOT NULL UNIQUE,
  art_form          VARCHAR(100)  NOT NULL,         -- e.g. Nadaswaram, Karagattam
  art_form_tamil    VARCHAR(100),                   -- e.g. நாதஸ்வரம்
  bio               TEXT,
  experience_years  INT           DEFAULT 0,
  base_price_min    DECIMAL(10,2) DEFAULT 0,
  base_price_max    DECIMAL(10,2) DEFAULT 0,
  latitude          DECIMAL(10,7),                  -- artist home location
  longitude         DECIMAL(10,7),
  city              VARCHAR(100),
  district          VARCHAR(100),
  state             VARCHAR(100)  DEFAULT 'Tamil Nadu',
  rating            DECIMAL(3,2)  DEFAULT 0.00,
  review_count      INT           DEFAULT 0,
  is_available      BOOLEAN       DEFAULT TRUE,
  instagram_url     VARCHAR(255),
  youtube_url       VARCHAR(255),
  celebrity_link    VARCHAR(255),
  teach_online      BOOLEAN       DEFAULT FALSE,
  teach_offline     BOOLEAN       DEFAULT FALSE,
  teach_price       DECIMAL(10,2),
  created_at        DATETIME      DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_art_form  (art_form),
  INDEX idx_location  (latitude, longitude),
  INDEX idx_rating    (rating)
);

-- ─── 3. ARTIST MEDIA (photos / videos) ───────────────────────
CREATE TABLE IF NOT EXISTS artist_media (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  artist_id   BIGINT        NOT NULL,
  media_type  ENUM('PHOTO','VIDEO') NOT NULL,
  url         VARCHAR(500)  NOT NULL,
  caption     VARCHAR(255),
  sort_order  INT           DEFAULT 0,
  uploaded_at DATETIME      DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (artist_id) REFERENCES artists(id) ON DELETE CASCADE
);

-- ─── 4. EVENTS (customer requests) ───────────────────────────
CREATE TABLE IF NOT EXISTS events (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id     BIGINT        NULL,           -- optional (for logged-in users)
  customer_name   VARCHAR(100)  NOT NULL,       -- direct capture (prototype mode)
  customer_phone  VARCHAR(15)   NOT NULL,
  event_type      VARCHAR(50)   NOT NULL,       -- wedding, temple_festival, etc.
  event_label     VARCHAR(120),                 -- display label e.g. "💍 Marriage / Wedding"
  event_date      DATE          NOT NULL,
  location_name   VARCHAR(255)  NOT NULL,
  latitude        DECIMAL(10,7),
  longitude       DECIMAL(10,7),
  budget_min      DECIMAL(10,2) NOT NULL,
  budget_max      DECIMAL(10,2) NOT NULL,
  status          ENUM('SEARCHING','BOOKED','CANCELLED','COMPLETED') DEFAULT 'SEARCHING',
  notes           TEXT,
  created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_event_type   (event_type),
  INDEX idx_event_date   (event_date),
  INDEX idx_status       (status),
  INDEX idx_customer_phone (customer_phone)
);

-- ─── 5. BOOKINGS ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bookings (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  event_id           BIGINT        NOT NULL,
  artist_id          BIGINT        NOT NULL,
  status             ENUM('PENDING','ACCEPTED','REJECTED','NEGOTIATING','COMPLETED','CANCELLED')
                     DEFAULT 'PENDING',
  quoted_price       DECIMAL(10,2),           -- customer's original offer
  negotiated_price   DECIMAL(10,2),           -- final agreed price
  ivr_call_sid       VARCHAR(100),            -- Twilio call SID (for button-phone artists)
  ivr_response       VARCHAR(20),             -- 'ACCEPT'|'REJECT'|'NEGOTIATE'
  customer_notes     TEXT,
  artist_notes       TEXT,
  created_at         DATETIME      DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (event_id)  REFERENCES events(id)  ON DELETE CASCADE,
  FOREIGN KEY (artist_id) REFERENCES artists(id) ON DELETE CASCADE,
  UNIQUE KEY uq_event_artist (event_id, artist_id),
  INDEX idx_booking_status (status)
);

-- ─── 6. RATINGS & REVIEWS ────────────────────────────────────
CREATE TABLE IF NOT EXISTS reviews (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  booking_id  BIGINT     NOT NULL UNIQUE,
  customer_id BIGINT     NOT NULL,
  artist_id   BIGINT     NOT NULL,
  rating      TINYINT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
  review_text TEXT,
  created_at  DATETIME   DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (booking_id)  REFERENCES bookings(id),
  FOREIGN KEY (customer_id) REFERENCES users(id),
  FOREIGN KEY (artist_id)   REFERENCES artists(id)
);

-- ─── 7. AI RECOMMENDATION LOG (ML Training Data) ─────────────
CREATE TABLE IF NOT EXISTS recommendation_logs (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id   BIGINT      NOT NULL,
  event_type    VARCHAR(50) NOT NULL,
  budget_min    DECIMAL(10,2),
  budget_max    DECIMAL(10,2),
  location      VARCHAR(100),
  recommended_artists  JSON,   -- array of artist IDs shown
  selected_artist_id   BIGINT, -- which one the customer chose
  created_at    DATETIME    DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (customer_id) REFERENCES users(id)
);

-- ─── 8. SAMPLE SEED DATA ─────────────────────────────────────
-- Insert sample artists (passwords are BCrypt hashed "password123")
INSERT IGNORE INTO users (id, name, email, password_hash, phone, role, device_type, preferred_lang) VALUES
(1, 'Murugan Pillai',   'murugan@kala.com',  '$2a$10$placeholder', '9876543210', 'ARTIST', 'SMARTPHONE', 'ta'),
(2, 'Kavitha Devi',     'kavitha@kala.com',  '$2a$10$placeholder', '9876543211', 'ARTIST', 'BUTTON_PHONE', 'ta'),
(3, 'Selvam',           'selvam@kala.com',   '$2a$10$placeholder', '9876543212', 'ARTIST', 'SMARTPHONE', 'ta'),
(4, 'Annamalai Balan',  'annamalai@kala.com','$2a$10$placeholder', '9876543213', 'ARTIST', 'BUTTON_PHONE', 'ta'),
(5, 'Meenakshi Ammal',  'meenakshi@kala.com','$2a$10$placeholder', '9876543214', 'ARTIST', 'SMARTPHONE', 'ta'),
(6, 'Rajesh Kumar',     'rajesh@kala.com',   '$2a$10$placeholder', '9876543215', 'CUSTOMER', 'SMARTPHONE', 'ta');

INSERT IGNORE INTO artists (id, user_id, art_form, art_form_tamil, experience_years, base_price_min, base_price_max, latitude, longitude, city, district, rating, review_count) VALUES
(1, 1, 'Nadaswaram', 'நாதஸ்வரம்', 22, 8000,  15000, 9.9252,  78.1198, 'Madurai',     'Madurai',     4.9, 128),
(2, 2, 'Karagattam', 'கரகாட்டம்',  15, 5000,  10000, 10.7867, 79.1378, 'Thanjavur',   'Thanjavur',   4.7, 95),
(3, 3, 'Therukoothu','தெருக்கூத்து',30, 12000, 25000, 11.9416, 79.1313, 'Villupuram',  'Villupuram',  4.8, 74),
(4, 4, 'Parai',      'பறை',        18, 4000,  8000,  11.0168, 76.9558, 'Coimbatore',  'Coimbatore',  4.6, 51),
(5, 5, 'Kolattam',   'கோலாட்டம்',  12, 3500,  7000,  8.7139,  77.7567, 'Tirunelveli', 'Tirunelveli', 4.5, 38);
