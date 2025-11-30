package org.example;

import java.sql.Date;
import java.util.List;
import java.util.Map;

public interface Database {
    void addReservation(String car, Date from, Date to);
    int getAvailableCars(String carType, Date from, Date to);

    // util methods
    String getCurrentDate();
    void setCurrentDate(String date);
    Map<String, Integer> getCarAmounts();
    void setCarAmounts(String type, int amount);
    List<String> listReservations();
    void resetDatabase();
}