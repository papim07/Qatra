package com.example.qatra;

import java.util.LinkedHashMap;

/**
 * <<boundary>> MapsService (external system, simulated).
 * Gives the distance in km from a donor's area to the blood bank.
 */
public class MapsService {

    // area -> distance from the Abu Dhabi blood bank (km)
    private static final LinkedHashMap<String, Integer> DISTANCES = new LinkedHashMap<>();
    static {
        DISTANCES.put("Abu Dhabi - Al Reem", 5);
        DISTANCES.put("Abu Dhabi - Al Khalidiya", 8);
        DISTANCES.put("Abu Dhabi - Khalifa City", 22);
        DISTANCES.put("Abu Dhabi - Mussafah", 28);
        DISTANCES.put("Abu Dhabi - Al Shamkha", 45);
        DISTANCES.put("Dubai", 140);
        DISTANCES.put("Al Ain", 160);
        DISTANCES.put("Sharjah", 165);
    }

    public static String[] locations() {
        return DISTANCES.keySet().toArray(new String[0]);
    }

    public int getDistance(String donorLocation, String bankLocation) {
        Integer donorKm = DISTANCES.get(donorLocation);
        Integer bankKm = DISTANCES.get(bankLocation);
        if (donorKm == null) return 999;
        if (bankKm == null) bankKm = 0;
        return Math.abs(donorKm - bankKm);
    }
}
