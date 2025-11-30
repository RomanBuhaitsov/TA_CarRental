package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CarRentalTest {

    private DB db;
    private CarRental rental;

    @BeforeEach
    void setup() {
        db = mock(DB.class);
        rental = new CarRental(db);

        // Mock default database state
        when(db.getCurrentDate()).thenReturn("2025-01-01");
        when(db.getCarAmounts()).thenReturn(java.util.Map.of(
            "SEDAN", 5,
            "SUV", 5,
            "VAN", 5
        ));
        when(db.listReservations()).thenReturn(java.util.List.of());
    }

    // ===== Basic Command Tests =====

    @Test
    @DisplayName("Reserve command calls DB with correct parameters")
    void testReserveCommandCallsDb() {
        when(db.getAvailableCars(anyString(), any(Date.class), any(Date.class))).thenReturn(5);

        rental.handleCommand("reserve sedan 2025-01-01 2025-01-05");

        verify(db, times(1)).getAvailableCars("SEDAN", Date.valueOf("2025-01-01"), Date.valueOf("2025-01-05"));
        verify(db, times(1)).addReservation("SEDAN", Date.valueOf("2025-01-01"), Date.valueOf("2025-01-05"));
    }

    @Test
    @DisplayName("SetDate command updates database")
    void testSetDate() {
        rental.handleCommand("setdate 2025-02-01");
        verify(db).setCurrentDate("2025-02-01");
    }

    @Test
    @DisplayName("SetCar command updates car amounts")
    void testSetCars() {
        rental.handleCommand("setcar van 5");
        verify(db).setCarAmounts("VAN", 5);
    }

    @Test
    @DisplayName("Info command retrieves system information")
    void testInfo() {
        when(db.getCurrentDate()).thenReturn("2025-01-01");
        when(db.getCarAmounts()).thenReturn(java.util.Map.of("SEDAN", 5));
        when(db.listReservations()).thenReturn(java.util.List.of());

        rental.handleCommand("info");

        verify(db).getCurrentDate();
        verify(db).getCarAmounts();
        verify(db).listReservations();
    }

    @Test
    @DisplayName("SetDate validates date format")
    void testSetDateValidation() {
        rental.handleCommand("setdate 2025-01-01");
        verify(db).setCurrentDate("2025-01-01");

        // Invalid format should not call DB
        rental.handleCommand("setdate invalid-date");
        verify(db, times(1)).setCurrentDate(anyString()); // Only the valid one above
    }

    @Test
    @DisplayName("Reset command clears database")
    void testResetCallsDbReset() {
        rental.handleCommand("reset");
        verify(db).resetDatabase();
    }

    @Test
    @DisplayName("Unknown command doesn't crash or call DB")
    void testUnknownCommandHandled() {
        rental.handleCommand("foobar");
        verifyNoInteractions(db);
    }

    // ===== Booking Validation Tests =====

    @Test
    @DisplayName("Cannot book when no cars available")
    void testNoAvailableCars() {
        when(db.getAvailableCars("SEDAN", Date.valueOf("2025-01-01"), Date.valueOf("2025-01-05")))
                .thenReturn(0);

        boolean result = rental.reserve(new String[]{"reserve", "sedan", "2025-01-01", "2025-01-05"});

        assertFalse(result, "Reservation should fail when no cars available");
        verify(db, never()).addReservation(anyString(), any(Date.class), any(Date.class));
    }

    @Test
    @DisplayName("Can book when cars available")
    void testCarsAvailable() {
        when(db.getAvailableCars("SUV", Date.valueOf("2025-01-01"), Date.valueOf("2025-01-05")))
                .thenReturn(3);

        boolean result = rental.reserve(new String[]{"reserve", "suv", "2025-01-01", "2025-01-05"});

        assertTrue(result, "Reservation should succeed when cars available");
        verify(db).addReservation("SUV", Date.valueOf("2025-01-01"), Date.valueOf("2025-01-05"));
    }

    @Test
    @DisplayName("Invalid car type is rejected")
    void testInvalidCarType() {
        boolean result = rental.reserve(new String[]{"reserve", "truck", "2025-01-01", "2025-01-05"});

        assertFalse(result, "Should reject invalid car type");
        verify(db, never()).getAvailableCars(anyString(), any(Date.class), any(Date.class));
        verify(db, never()).addReservation(anyString(), any(Date.class), any(Date.class));
    }

    @Test
    @DisplayName("End date before start date is rejected")
    void testInvalidDateRange() {
        boolean result = rental.reserve(new String[]{"reserve", "sedan", "2025-01-10", "2025-01-05"});

        assertFalse(result, "Should reject booking where end date is before start date");
        verify(db, never()).getAvailableCars(anyString(), any(Date.class), any(Date.class));
    }

    @Test
    @DisplayName("Invalid date format is rejected")
    void testInvalidDateFormat() {
        boolean result = rental.reserve(new String[]{"reserve", "sedan", "2025/01/01", "2025/01/05"});

        assertFalse(result, "Should reject invalid date format");
        verify(db, never()).getAvailableCars(anyString(), any(Date.class), any(Date.class));
    }

    @Test
    @DisplayName("Missing arguments is rejected")
    void testMissingArguments() {
        boolean result = rental.reserve(new String[]{"reserve", "sedan"});

        assertFalse(result, "Should reject command with missing arguments");
        verify(db, never()).getAvailableCars(anyString(), any(Date.class), any(Date.class));
    }

    // ===== Case Sensitivity Tests =====

    @Test
    @DisplayName("Car types are case-insensitive")
    void testCaseInsensitiveCarTypes() {
        when(db.getAvailableCars("SEDAN", Date.valueOf("2025-01-01"), Date.valueOf("2025-01-05")))
                .thenReturn(5);

        rental.reserve(new String[]{"reserve", "SeDaN", "2025-01-01", "2025-01-05"});

        verify(db).getAvailableCars("SEDAN", Date.valueOf("2025-01-01"), Date.valueOf("2025-01-05"));
        verify(db).addReservation("SEDAN", Date.valueOf("2025-01-01"), Date.valueOf("2025-01-05"));
    }

    // ===== SetCar Validation Tests =====

    @Test
    @DisplayName("Cannot set negative car amount")
    void testNegativeCarAmount() {
        rental.handleCommand("setcar sedan -5");
        verify(db, never()).setCarAmounts(anyString(), anyInt());
    }

    @Test
    @DisplayName("Cannot set non-numeric car amount")
    void testNonNumericCarAmount() {
        rental.handleCommand("setcar sedan abc");
        verify(db, never()).setCarAmounts(anyString(), anyInt());
    }

    @Test
    @DisplayName("Can set zero car amount")
    void testZeroCarAmount() {
        rental.handleCommand("setcar suv 0");
        verify(db).setCarAmounts("SUV", 0);
    }

    @Test
    @DisplayName("Cannot set invalid car type in setcar")
    void testSetCarInvalidType() {
        rental.handleCommand("setcar bicycle 10");
        verify(db, never()).setCarAmounts(anyString(), anyInt());
    }
}