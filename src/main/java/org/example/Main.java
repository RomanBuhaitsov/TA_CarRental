package org.example;

import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        CarRental carRental = new CarRental(new DB());

        System.out.println("Type 'help' for commands.");

        while (true) {
            System.out.print("> ");
            String commandLine = scanner.nextLine().trim();

            if (commandLine.equalsIgnoreCase("stop")) break;
            if (commandLine.isEmpty()) continue;

            carRental.handleCommand(commandLine);
        }
    }
}
