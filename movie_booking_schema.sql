-- ============================================================
-- MOVIE TICKET BOOKING SYSTEM - DATABASE SCHEMA
-- ============================================================
-- Run this first in MySQL before running the Java program.
-- ============================================================

CREATE DATABASE IF NOT EXISTS movie_booking;
USE movie_booking;

-- ---------- Table 1: movies ----------
CREATE TABLE IF NOT EXISTS movies (
    movie_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    genre VARCHAR(50),
    duration_minutes INT,
    rating DOUBLE
);

-- ---------- Table 2: theaters ----------
CREATE TABLE IF NOT EXISTS theaters (
    theater_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(100),
    total_seats INT NOT NULL
);

-- ---------- Table 3: shows ----------
-- One "show" = a movie playing at a specific theater, date, and time
CREATE TABLE IF NOT EXISTS shows (
    show_id INT AUTO_INCREMENT PRIMARY KEY,
    movie_id INT NOT NULL,
    theater_id INT NOT NULL,
    show_date DATE NOT NULL,
    show_time TIME NOT NULL,
    ticket_price DOUBLE NOT NULL,
    total_seats INT NOT NULL,
    seats_booked INT DEFAULT 0,
    FOREIGN KEY (movie_id) REFERENCES movies(movie_id),
    FOREIGN KEY (theater_id) REFERENCES theaters(theater_id)
);

-- ---------- Table 4: bookings ----------
-- Each row = one seat booked by one customer for one show.
-- The UNIQUE constraint below is the database-level seat validation:
-- the same seat_number can never be booked twice for the same show.
CREATE TABLE IF NOT EXISTS bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    show_id INT NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    booking_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (show_id) REFERENCES shows(show_id),
    UNIQUE (show_id, seat_number)
);
