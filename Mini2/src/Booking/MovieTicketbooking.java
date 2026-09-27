package Booking;
import java.util.Scanner;
import java.sql.*;
import java.sql.Date;
import java.time.DateTimeException;
import java.time.LocalDate;

public class MovieTicketbooking {
	// ---------- Database Connection Details ----------
    static final String DB_URL = "jdbc:mysql://localhost:3306/movie_booking";
    static final String DB_USER = "root";
    static final String DB_PASSWORD = "your_password";
 
    static Scanner sc = new Scanner(System.in);
 
    public static void main(String[] args) {
 
        int choice;
 
        do {
            System.out.println("\n===== MOVIE TICKET BOOKING SYSTEM =====");
            System.out.println("1. Add Movie");
            System.out.println("2. Add Theater");
            System.out.println("3. Add Show");
            System.out.println("4. Movie Listing (View All Shows)");
            System.out.println("5. Book a Seat");
            System.out.println("6. Generate Ticket");
            System.out.println("7. Exit");
            System.out.print("Enter choice: ");
            choice = sc.nextInt();
 
            switch (choice) {
                case 1: addMovie(); break;
                case 2: addTheater(); break;
                case 3: addShow(); break;
                case 4: movieListing(); break;
                case 5: bookSeat(); break;
                case 6: generateTicket(); break;
                case 7: System.out.println("Exiting... Enjoy the movie!"); break;
                default: System.out.println("Invalid choice.");
            }
 
        } while (choice != 7);
 
        sc.close();
    }
 
    // ---------- Get a new DB connection ----------
    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
 
    /* ================================================================
       MODULE 1a : ADD MOVIE   (part of Movie Listing)
       ================================================================ */
    static void addMovie() {
        sc.nextLine();
        System.out.print("Movie Title: ");
        String title = sc.nextLine();
        System.out.print("Genre: ");
        String genre = sc.nextLine();
        System.out.print("Duration (minutes): ");
        int duration = sc.nextInt();
        System.out.print("Rating (out of 5): ");
        double rating = sc.nextDouble();
 
        String insert = "INSERT INTO movies (title, genre, duration_minutes, rating) VALUES (?, ?, ?, ?)";
 
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(insert)) {
 
            ps.setString(1, title);
            ps.setString(2, genre);
            ps.setInt(3, duration);
            ps.setDouble(4, rating);
            ps.executeUpdate();
 
            System.out.println("Movie added successfully!");
 
        } catch (SQLException e) {
            System.out.println("Error adding movie: " + e.getMessage());
        }
    }
 
    /* ================================================================
       MODULE 2 : THEATER MANAGEMENT
       ================================================================ */
    static void addTheater() {
        sc.nextLine();
        System.out.print("Theater Name: ");
        String name = sc.nextLine();
        System.out.print("Location: ");
        String location = sc.nextLine();
        System.out.print("Total Seats: ");
        int totalSeats = sc.nextInt();
 
        String insert = "INSERT INTO theaters (name, location, total_seats) VALUES (?, ?, ?)";
 
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(insert)) {
 
            ps.setString(1, name);
            ps.setString(2, location);
            ps.setInt(3, totalSeats);
            ps.executeUpdate();
 
            System.out.println("Theater added successfully!");
 
        } catch (SQLException e) {
            System.out.println("Error adding theater: " + e.getMessage());
        }
    }
 
    /* ---------- Helper: Add a Show (links a movie to a theater + time slot) ---------- */
    static void addShow() {
        System.out.print("Enter Movie ID: ");
        int movieId = sc.nextInt();
        System.out.print("Enter Theater ID: ");
        int theaterId = sc.nextInt();
        sc.nextLine();
        System.out.print("Show Date (YYYY-MM-DD): ");
        LocalDate date = LocalDate.parse(sc.nextLine());
        System.out.print("Show Time (HH:MM, 24-hr): ");
        LocalTime time = LocalTime.parse(sc.nextLine());
        System.out.print("Ticket Price: ");
        double price = sc.nextDouble();
        System.out.print("Total Seats for this show: ");
        int totalSeats = sc.nextInt();
 
        String insert = "INSERT INTO shows (movie_id, theater_id, show_date, show_time, ticket_price, total_seats, seats_booked) " +
                "VALUES (?, ?, ?, ?, ?, ?, 0)";
 
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(insert)) {
 
            ps.setInt(1, movieId);
            ps.setInt(2, theaterId);
            ps.setDate(3, Date.valueOf(date));
            ps.setTime(4, Time.valueOf(time));
            ps.setDouble(5, price);
            ps.setInt(6, totalSeats);
            ps.executeUpdate();
 
            System.out.println("Show added successfully!");
 
        } catch (SQLException e) {
            System.out.println("Error adding show: " + e.getMessage());
        }
    }
 
    /* ================================================================
       MODULE 1b : MOVIE LISTING   (view all shows, joined with movies + theaters)
       ================================================================ */
    static void movieListing() {
        String query =
                "SELECT s.show_id, m.title, m.genre, t.name AS theater_name, " +
                "s.show_date, s.show_time, s.ticket_price, " +
                "(s.total_seats - s.seats_booked) AS seats_left " +
                "FROM shows s " +
                "JOIN movies m ON s.movie_id = m.movie_id " +
                "JOIN theaters t ON s.theater_id = t.theater_id " +
                "ORDER BY s.show_date, s.show_time";
 
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
 
            System.out.println("\n--- Now Showing ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println(
                        "Show ID: " + rs.getInt("show_id") +
                        " | " + rs.getString("title") + " (" + rs.getString("genre") + ")" +
                        " | Theater: " + rs.getString("theater_name") +
                        " | " + rs.getDate("show_date") + " " + rs.getTime("show_time") +
                        " | Price: Rs." + rs.getDouble("ticket_price") +
                        " | Seats Left: " + rs.getInt("seats_left")
                );
            }
            if (!found) System.out.println("No shows scheduled yet.");
 
        } catch (SQLException e) {
            System.out.println("Error fetching movie listing: " + e.getMessage());
        }
    }
 
    /* ================================================================
       MODULE 3 : SEAT BOOKING   (TRANSACTION + SEAT VALIDATION)
       ------------------------------------------------------------------
       Booking a seat touches 2 tables together:
         1) bookings -> insert the seat booking
         2) shows    -> increase seats_booked by 1
 
       SEAT VALIDATION happens in 2 layers:
         (a) In code: check if seats are still available AND check if
             the exact seat_number is already taken for this show
         (b) In the database: UNIQUE(show_id, seat_number) constraint
             blocks a duplicate seat even if 2 users click "book" at
             the exact same time (race condition safety net)
 
       Both table updates are wrapped in ONE transaction so a seat is
       never marked booked without the show's seat count also updating.
       ================================================================ */
    static void bookSeat() {
        System.out.print("Enter Show ID: ");
        int showId = sc.nextInt();
        sc.nextLine();
        System.out.print("Enter Customer Name: ");
        String customerName = sc.nextLine();
        System.out.print("Enter Seat Number (e.g. A1): ");
        String seatNumber = sc.nextLine().toUpperCase();
 
        Connection con = null;
 
        try {
            con = getConnection();
            con.setAutoCommit(false); // ---- START TRANSACTION ----
 
            // Step 1: lock the show row and check remaining seats
            String showQuery = "SELECT total_seats, seats_booked FROM shows WHERE show_id = ? FOR UPDATE";
            PreparedStatement showPs = con.prepareStatement(showQuery);
            showPs.setInt(1, showId);
            ResultSet showRs = showPs.executeQuery();
 
            if (!showRs.next()) {
                System.out.println("Show not found.");
                con.rollback();
                return;
            }
 
            int totalSeats = showRs.getInt("total_seats");
            int seatsBooked = showRs.getInt("seats_booked");
 
            if (seatsBooked >= totalSeats) {
                System.out.println("Sorry, this show is fully booked.");
                con.rollback();
                return;
            }
 
            // Step 2: SEAT VALIDATION - check if this exact seat is already taken
            String seatCheck = "SELECT seat_number FROM bookings WHERE show_id = ? AND seat_number = ?";
            PreparedStatement seatPs = con.prepareStatement(seatCheck);
            seatPs.setInt(1, showId);
            seatPs.setString(2, seatNumber);
            ResultSet seatRs = seatPs.executeQuery();
 
            if (seatRs.next()) {
                System.out.println("Seat " + seatNumber + " is already booked. Choose another seat.");
                con.rollback();
                return;
            }
 
            // Step 3: insert the booking
            String insertBooking = "INSERT INTO bookings (show_id, customer_name, seat_number) VALUES (?, ?, ?)";
            PreparedStatement insertPs = con.prepareStatement(insertBooking);
            insertPs.setInt(1, showId);
            insertPs.setString(2, customerName);
            insertPs.setString(3, seatNumber);
            insertPs.executeUpdate();
 
            // Step 4: update seats_booked count in shows
            String updateShow = "UPDATE shows SET seats_booked = seats_booked + 1 WHERE show_id = ?";
            PreparedStatement updatePs = con.prepareStatement(updateShow);
            updatePs.setInt(1, showId);
            updatePs.executeUpdate();
 
            con.commit(); // ---- COMMIT: both changes saved together ----
 
            System.out.println("Seat booked successfully!");
            System.out.println("Customer: " + customerName + " | Seat: " + seatNumber);
 
        } catch (SQLIntegrityConstraintViolationException dup) {
            // Extra safety net: triggers if the UNIQUE(show_id, seat_number)
            // constraint catches a duplicate seat (e.g. race condition)
            System.out.println("Booking failed: seat was just taken by someone else. Try a different seat.");
            try {
                if (con != null) con.rollback();
            } catch (SQLException ex) {
                System.out.println("Rollback failed: " + ex.getMessage());
            }
        } catch (SQLException e) {
            System.out.println("Transaction failed, rolling back. Error: " + e.getMessage());
            try {
                if (con != null) con.rollback();
            } catch (SQLException ex) {
                System.out.println("Rollback failed: " + ex.getMessage());
            }
        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
    }
 
    /* ================================================================
       MODULE 4 : TICKET GENERATION   (READ, uses JOIN across all 4 tables)
       ================================================================ */
    static void generateTicket() {
        System.out.print("Enter Booking ID: ");
        int bookingId = sc.nextInt();
 
        String query =
                "SELECT b.customer_name, b.seat_number, b.booking_time, " +
                "m.title, m.genre, t.name AS theater_name, t.location, " +
                "s.show_date, s.show_time, s.ticket_price " +
                "FROM bookings b " +
                "JOIN shows s ON b.show_id = s.show_id " +
                "JOIN movies m ON s.movie_id = m.movie_id " +
                "JOIN theaters t ON s.theater_id = t.theater_id " +
                "WHERE b.booking_id = ?";
 
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
 
            ps.setInt(1, bookingId);
            ResultSet rs = ps.executeQuery();
 
            if (rs.next()) {
                System.out.println("\n========= MOVIE TICKET =========");
                System.out.println("Movie      : " + rs.getString("title") + " (" + rs.getString("genre") + ")");
                System.out.println("Theater    : " + rs.getString("theater_name") + ", " + rs.getString("location"));
                System.out.println("Date/Time  : " + rs.getDate("show_date") + " " + rs.getTime("show_time"));
                System.out.println("Customer   : " + rs.getString("customer_name"));
                System.out.println("Seat No.   : " + rs.getString("seat_number"));
                System.out.println("Price      : Rs." + rs.getDouble("ticket_price"));
                System.out.println("Booked On  : " + rs.getTimestamp("booking_time"));
                System.out.println("=================================");
            } else {
                System.out.println("No booking found with that ID.");
            }
 
        } catch (SQLException e) {
            System.out.println("Error generating ticket: " + e.getMessage());
        }
    }
}
 

