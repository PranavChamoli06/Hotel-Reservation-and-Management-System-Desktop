package com.hotel.dao;

import com.hotel.model.Reservation;
import com.hotel.util.DBConnection;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDate;


/**
 * ReservationDAO – Optimized DAO layer for reservations.
 * - Centralized column lists
 * - Unified mapper
 * - Consistent logging
 * - Cleaner SQL handling
 * - Better structure for scaling
 */
public class ReservationDAO {

    private static final Logger LOGGER = Logger.getLogger(ReservationDAO.class.getName());

    /* ------------------------------------------------------------------------
       Shared reusable column list (ADD NEW COLUMNS HERE ONLY)
       ------------------------------------------------------------------------ */
    private static final String BASE_COLUMNS =
            "id, user_id, guest_name, room_number, room_type, check_in, check_out, " +
                    "phone_number, status, total_price, created_at";

    /* ------------------------------------------------------------------------
       MAPPER: Converts a DB row to a Reservation object
       ------------------------------------------------------------------------ */
    private Reservation map(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("guest_name"),
                rs.getInt("room_number"),
                rs.getString("room_type"),
                rs.getDate("check_in"),
                rs.getDate("check_out"),
                rs.getString("phone_number"),
                rs.getString("status"),
                rs.getDouble("total_price"),
                rs.getTimestamp("created_at")
        );
    }

    /* ========================================================================
       CRUD OPERATIONS
       ======================================================================== */

    /** Get all reservations */
    public List<Reservation> getAllReservations() {
        String sql = "SELECT " + BASE_COLUMNS + " FROM reservations ORDER BY id DESC";
        List<Reservation> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) list.add(map(rs));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in getAllReservations", e);
        }

        return list;
    }

    /** Single reservation by ID */
    public Optional<Reservation> getReservationById(int id) {
        String sql = "SELECT " + BASE_COLUMNS + " FROM reservations WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in getReservationById id=" + id, e);
        }

        return Optional.empty();
    }

    /** Insert reservation */
    public boolean createReservation(Reservation r) {
        String sql =
                "INSERT INTO reservations (" +
                        "user_id, guest_name, room_number, room_type, check_in, check_out, " +
                        "phone_number, status, total_price, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, r.getUserId());
            stmt.setString(2, r.getGuestName());
            stmt.setInt(3, r.getRoomNumber());
            stmt.setString(4, r.getRoomType());
            stmt.setDate(5, r.getCheckIn());
            stmt.setDate(6, r.getCheckOut());
            stmt.setString(7, r.getPhoneNumber());
            stmt.setString(8, r.getStatus());
            stmt.setDouble(9, r.getTotalPrice());
            stmt.setTimestamp(10, r.getCreatedAt());

            int affected = stmt.executeUpdate();
            if (affected == 0) return false;

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) LOGGER.info("Inserted reservation ID = " + keys.getInt(1));
            }

            return true;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in createReservation", e);
            return false;
        }
    }

    /** Update reservation */
    public boolean updateReservation(Reservation r) {
        String sql =
                "UPDATE reservations SET " +
                        "user_id=?, guest_name=?, room_number=?, room_type=?, check_in=?, " +
                        "check_out=?, phone_number=?, status=?, total_price=? " +
                        "WHERE id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, r.getUserId());
            stmt.setString(2, r.getGuestName());
            stmt.setInt(3, r.getRoomNumber());
            stmt.setString(4, r.getRoomType());
            stmt.setDate(5, r.getCheckIn());
            stmt.setDate(6, r.getCheckOut());
            stmt.setString(7, r.getPhoneNumber());
            stmt.setString(8, r.getStatus());
            stmt.setDouble(9, r.getTotalPrice());
            stmt.setInt(10, r.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in updateReservation", e);
            return false;
        }
    }

    /**
     * Update only the check_in and check_out columns for a reservation.
     * This intentionally avoids touching guest info, phone, room_type, price, etc.
     */
    public boolean updateCheckInOut(Reservation r) {
        String sql = "UPDATE reservations SET check_in = ?, check_out = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (r.getCheckIn() != null) stmt.setDate(1, r.getCheckIn());
            else stmt.setNull(1, java.sql.Types.DATE);

            if (r.getCheckOut() != null) stmt.setDate(2, r.getCheckOut());
            else stmt.setNull(2, java.sql.Types.DATE);

            stmt.setInt(3, r.getId());

            int updated = stmt.executeUpdate();
            return updated > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in updateCheckInOut id=" + r.getId(), e);
            return false;
        }
    }

    /* ========================================================================
       DELETE OPERATIONS
       ======================================================================== */

    /** Hard delete with result message */
    public static class DeleteResult {
        public final boolean success;
        public final String message;

        public DeleteResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public DeleteResult deleteReservationWithResult(int id) {
        String sql = "DELETE FROM reservations WHERE id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int affected = stmt.executeUpdate();

            if (affected > 0)
                return new DeleteResult(true, "Deleted successfully");

            return new DeleteResult(false, "No reservation found");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting reservation id=" + id, e);
            return new DeleteResult(false, "SQL error: " + e.getMessage());
        }
    }

    /** Boolean delete wrapper */
    public boolean deleteReservation(int id) {
        DeleteResult r = deleteReservationWithResult(id);
        if (!r.success) LOGGER.warning(r.message);
        return r.success;
    }

    /** Soft delete (for staff/managers) */
    public boolean softDeleteReservation(int id) {
        String sql = "UPDATE reservations SET status='Cancelled' WHERE id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in softDeleteReservation id=" + id, e);
            return false;
        }
    }

    /* ========================================================================
       ROOM AVAILABILITY & UTILITIES
       ======================================================================== */

    /** Check if a room is available for time range */
    public boolean isRoomAvailable(int roomNumber, Date checkIn, Date checkOut) {
        String sql =
                "SELECT COUNT(*) FROM reservations " +
                        "WHERE room_number=? " +
                        "AND status NOT IN ('Cancelled','No-Show') " +
                        "AND NOT (check_out <= ? OR check_in >= ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, checkIn);
            stmt.setDate(3, checkOut);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) == 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in isRoomAvailable", e);
        }

        return false;
    }

    /** Find available room by type */
    public Optional<Integer> findAvailableRoom(String type, Date checkIn, Date checkOut) {
        int start, end;

        switch (type) {
            case "Standard":
                start = 100; end = 150;
                break;
            case "Deluxe":
                start = 200; end = 250;
                break;
            case "Suite":
                start = 300; end = 350;
                break;
            default:
                return Optional.empty();
        }

        String sql = "SELECT room_number FROM reservations " +
                "WHERE room_type = ? " +
                "AND NOT (? >= check_out OR ? <= check_in)";

        Set<Integer> booked = new HashSet<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, type);
            stmt.setDate(2, checkIn);
            stmt.setDate(3, checkOut);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) booked.add(rs.getInt(1));

        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }

        // find first free room in the allowed range
        for (int room = start; room <= end; room++) {
            if (!booked.contains(room))
                return Optional.of(room);
        }

        return Optional.empty();
    }


    /* ========================================================================
       USER-SPECIFIC RESERVATIONS
       ======================================================================== */

    /** Reservations for a specific user */
    public List<Reservation> getReservationsByUserId(int userId) {
        String sql = "SELECT " + BASE_COLUMNS + " FROM reservations WHERE user_id=?";
        List<Reservation> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in getReservationsByUserId userId=" + userId, e);
        }

        return list;
    }

    /* ========================================================================
  COUNTING AVAILABLE ROOMS FOR EACH ROOM TYPE
  ======================================================================== */
    /**
     * Count available rooms for a given roomType and date range.
     * Uses the same number ranges as findAvailableRoom:
     *  Standard 100-199, Deluxe 200-299, Suite 300-399
     */
    public int countAvailableRooms(String type, Date checkIn, Date checkOut) {
        int start, end;

        switch (type) {
            case "Standard":
                start = 100; end = 150;
                break;
            case "Deluxe":
                start = 200; end = 250;
                break;
            case "Suite":
                start = 300; end = 350;
                break;
            default:
                return 0;
        }

        int totalRooms = (end - start + 1);

        String sql = "SELECT COUNT(*) FROM reservations " +
                "WHERE room_type = ? " +
                "AND NOT (? >= check_out OR ? <= check_in)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, type);
            stmt.setDate(2, checkIn);
            stmt.setDate(3, checkOut);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int booked = rs.getInt(1);
                return Math.max(0, totalRooms - booked);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }



    /* ========================================================================
   REPORTING FILTERS : DAILY / MONTHLY / YEARLY EXPORT
   ======================================================================== */

    public List<Reservation> getReservationsByDate(LocalDate date) {
        String sql = "SELECT " + BASE_COLUMNS + " FROM reservations WHERE DATE(check_in) = ?";
        List<Reservation> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in getReservationsByDate date=" + date, e);
        }

        return list;
    }

    public List<Reservation> getReservationsByMonth(int year, int month) {
        String sql = "SELECT " + BASE_COLUMNS +
                " FROM reservations WHERE YEAR(check_in) = ? AND MONTH(check_in) = ?";
        List<Reservation> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in getReservationsByMonth " + year + "-" + month, e);
        }

        return list;
    }

    public List<Reservation> getReservationsByYear(int year) {
        String sql = "SELECT " + BASE_COLUMNS + " FROM reservations WHERE YEAR(check_in) = ?";
        List<Reservation> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in getReservationsByYear year=" + year, e);
        }

        return list;
    }

}
