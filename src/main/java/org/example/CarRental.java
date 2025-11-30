package org.example;

import java.sql.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CarRental {

    private static final Set<String> VALID_CAR_TYPES = Set.of("suv", "van", "sedan");
    private final DB db;

    public CarRental(DB db) {
        this.db = db;
    }

    public void handleCommand(String commandLine) {
        String[] args = commandLine.toLowerCase(Locale.ROOT).split(" ");
        String cmd = args[0];

        try {
            switch (cmd) {
                case "reserve" -> reserve(args);
                case "jump" -> jump(args);
                case "info" -> info(args);
                case "help" -> help();
                case "reset" -> resetDb();
                case "setdate" -> setDate(args);
                case "setcar" -> setCars(args);
                default -> System.out.println("Unknown command. Type 'help'.");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean reserve(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: reserve <car> <from-date> <to-date>");
            return false;
        }

        String car = args[1].toLowerCase();
        String from = args[2];
        String to = args[3];

        if (!VALID_CAR_TYPES.contains(car)) {
            System.out.println("Invalid car type. Must be one of: sedan, suv, van");
            return false;
        }

        Date fromDate;
        Date toDate;
        try {
            fromDate = Date.valueOf(from);
            toDate = Date.valueOf(to);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid date format. Use yyyy-mm-dd");
            return false;
        }

        // Validate date logic
        if (fromDate.after(toDate)) {
            System.out.println("Start date must not be after end date");
            return false;
        }

        String currDate = db.getCurrentDate();
        if(currDate == null){
            System.out.println("Start date must be set to reserve a car");
            return false;
        }

        Date currentDate = Date.valueOf(db.getCurrentDate());
        if (fromDate.before(currentDate)){
            System.out.println("Start date must not be before current date");
            return false;
        }

        // Check availability
        int available = db.getAvailableCars(car.toUpperCase(), fromDate, toDate);
        if (available <= 0) {
            System.out.println("No " + car + " available for the selected dates");
            return false;
        }

        db.addReservation(car.toUpperCase(), fromDate, toDate);
        System.out.println("Reservation saved! (" + (available - 1) + " " + car + "(s) still available)");
        return true;
    }

    public void jump(String[] args) {
        System.out.println("Jump executed.");
    }

    public void resetDb() {
        db.resetDatabase();
        System.out.println("Database reset.");
    }

    public void setDate(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: setdate <yyyy-mm-dd>");
            return;
        }

        try {
            Date.valueOf(args[1]); // Validate format
            db.setCurrentDate(args[1]);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid date format. Use yyyy-mm-dd");
        }
    }

    public void setCars(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: setcar <car_type> <amount>");
            return;
        }

        String car = args[1].toLowerCase();
        if (!VALID_CAR_TYPES.contains(car)) {
            System.out.println("Wrong car_type. Needs to be one of: \"suv\", \"van\", \"sedan\".");
            return;
        }

        try {
            int amount = Integer.parseInt(args[2]);
            if (amount < 0) {
                System.out.println("Amount must be non-negative.");
                return;
            }
            db.setCarAmounts(car.toUpperCase(), amount);
        } catch (NumberFormatException e) {
            System.out.println("Amount must be a number.");
        }
    }

    public void info(String[] args) {
        System.out.println("=== System Information ===");

        String date = db.getCurrentDate();
        System.out.println("Current date: " + (date != null ? date : "not set"));

        System.out.println("\nCar amounts:");
        Map<String, Integer> cars = db.getCarAmounts();
        for (Map.Entry<String, Integer> entry : cars.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        if(date != null){
            Date date1 = Date.valueOf(date);
            for (String s: VALID_CAR_TYPES){
                db.getAvailableCars(s, date1, date1);
            }
        } else {
            System.out.println("no cars booked");
        }

        if (cars.isEmpty())
            System.out.println(" No cars defined.");

        System.out.println("\nReservations:");
        List<String> reservations = db.listReservations();
        if (reservations.isEmpty()) {
            System.out.println(" No reservations.");
        } else {
            for (String res : reservations) {
                System.out.println(" " + res);
            }
        }
    }

    public void help() {
        System.out.println("Available commands:");
        System.out.println("  reserve <car> <from> <to>    - Create a reservation");
        System.out.println("  info                         - Show all reservations");
        System.out.println("  jump <something>             - Debug/test command");
        System.out.println("  reset                        - Reset the entire database");
        System.out.println("  setdate <yyyy-mm-dd>         - Set the simulation date");
        System.out.println("  setcar <car_type> <amount>   - Set available amount for all car types");
        System.out.println("  help                         - Show this help message");
        System.out.println("  stop                         - Quit the program");
    }
}