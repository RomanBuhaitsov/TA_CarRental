package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DB implements Database {

    private static final String URL = "jdbc:h2:./testdb";
    private static final String USER = "sa";
    private static final String PASS = "";

    public DB() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS reservations (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            car VARCHAR(255),
                            from_date DATE,
                            to_date DATE
                        );
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS cars (
                            car_type VARCHAR(255) PRIMARY KEY,
                            amount INT
                        );
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS settings (
                            "setting_key" VARCHAR(50) PRIMARY KEY,
                            "setting_value" VARCHAR(255)
                        );
                    """);

            initializeDefaultData();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initializeDefaultData() {
        try (Connection conn = getConnection()) {

            // Check if current_date is set, if not set default
            PreparedStatement checkDate = conn.prepareStatement(
                    "SELECT COUNT(*) as count FROM settings WHERE setting_key = 'current_date'");
            ResultSet rs = checkDate.executeQuery();
            if (rs.next() && rs.getInt("count") == 0) {
                setCurrentDate("2025-01-01");
            }
            rs.close();
            checkDate.close();

            // Check if cars table is empty, if so add defaults
            PreparedStatement checkCars = conn.prepareStatement(
                    "SELECT COUNT(*) as count FROM cars");
            ResultSet rsCars = checkCars.executeQuery();
            if (rsCars.next() && rsCars.getInt("count") == 0) {
                setCarAmounts("SEDAN", 5);
                setCarAmounts("SUV", 5);
                setCarAmounts("VAN", 5);
            }
            rsCars.close();
            checkCars.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public void addReservation(String car, Date from, Date to) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO reservations (car, from_date, to_date) VALUES (?, ?, ?)")) {

            ps.setString(1, car);
            ps.setDate(2, from);
            ps.setDate(3, to);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getAvailableCars(String carType, Date from, Date to) {
        int totalCars = 0;
        int bookedCars = 0;

        // Get total cars of this type
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT amount FROM cars WHERE car_type = ?")) {
            ps.setString(1, carType.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                totalCars = rs.getInt("amount");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }

        // Count overlapping reservations
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) as count FROM reservations WHERE car = ? AND " +
                     "((from_date <= ? AND to_date > ?) OR " +
                     "(from_date < ? AND to_date >= ?) OR " +
                     "(from_date >= ? AND to_date <= ?))")) {

            ps.setString(1, carType);
            ps.setDate(2, to);
            ps.setDate(3, from);
            ps.setDate(4, to);
            ps.setDate(5, from);
            ps.setDate(6, from);
            ps.setDate(7, to);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                bookedCars = rs.getInt("count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }

        return totalCars - bookedCars;
    }

    public void setCurrentDate(String date) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "MERGE INTO settings (setting_key, setting_value) KEY(setting_key) VALUES ('current_date', ?)"
             )) {

            ps.setString(1, date);
            ps.executeUpdate();

            System.out.println("Date set to " + date);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getCurrentDate() {
        String date = null;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM settings WHERE setting_key = 'current_date'"
             );
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                date = rs.getString("setting_value");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return date;
    }

    public void setCarAmounts(String type, int amount) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "MERGE INTO cars (car_type, amount) KEY(car_type) VALUES (?, ?)")) {
            ps.setString(1, type.toUpperCase());
            ps.setInt(2, amount);
            ps.executeUpdate();
            System.out.println("Set " + type + " amount to " + amount);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Integer> getCarAmounts() {
        Map<String, Integer> cars = new HashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT car_type, amount FROM cars");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String carType = rs.getString("car_type");
                int amount = rs.getInt("amount");
                cars.put(carType, amount);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cars;
    }

    public List<String> listReservations() {
        List<String> reservations = new ArrayList<>();
        String currentDate = getCurrentDate();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM reservations WHERE to_date >= ? ORDER BY from_date")) {

            // Use current date if set, otherwise show all
            if (currentDate != null) {
                ps.setDate(1, Date.valueOf(currentDate));
            } else {
                ps.setDate(1, Date.valueOf("1900-01-01")); // Show all if no current date
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                reservations.add(
                        rs.getInt("id") + ": " +
                                rs.getString("car") + " " +
                                rs.getDate("from_date") + " → " +
                                rs.getDate("to_date"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return reservations;
    }

    public void resetDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE IF EXISTS reservations;");
            stmt.execute("DROP TABLE IF EXISTS cars;");
            stmt.execute("DROP TABLE IF EXISTS settings;");

            stmt.execute("""
                        CREATE TABLE settings (
                            setting_key VARCHAR(50) PRIMARY KEY,
                            setting_value VARCHAR(255)
                        );
                    """);

            stmt.execute("""
                        CREATE TABLE cars (
                            car_type VARCHAR(50) PRIMARY KEY,
                            amount INT
                        );
                    """);

            stmt.execute("""
                        CREATE TABLE reservations (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            car VARCHAR(50),
                            from_date DATE,
                            to_date DATE
                        );
                    """);

            setCurrentDate("2025-01-01");
            setCarAmounts("SEDAN", 5);
            setCarAmounts("SUV", 5);
            setCarAmounts("VAN", 5);

            System.out.println("Database reset and initialized.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}