package com.example.qatra;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Calendar factors that lower donations (FR-8): Ramadan, holidays, school breaks, summer.
 * Dates are approximate (UAE, 2026-2027).
 */
public class SeasonCalendar {

    // {start, end} as yyyymmdd
    private static final int[][] RAMADAN = {{20260218, 20260319}, {20270208, 20270309}};
    private static final int[][] HOLIDAYS = {
        {20260320, 20260322},   // Eid al-Fitr
        {20260526, 20260529},   // Eid al-Adha
        {20261202, 20261203},   // National Day
        {20270101, 20270101},   // New Year
        {20270310, 20270312},   // Eid al-Fitr
        {20270515, 20270518}    // Eid al-Adha
    };

    private static int toDate(long day) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(day * 86400000L);
        return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH);
    }

    private static boolean inRanges(int date, int[][] ranges) {
        for (int[] r : ranges) {
            if (date >= r[0] && date <= r[1]) return true;
        }
        return false;
    }

    /** Name of the season on that day, or null for a normal day. */
    public static String seasonName(long day) {
        int date = toDate(day);
        int mmdd = date % 10000;
        if (inRanges(date, RAMADAN)) return "Ramadan";
        if (inRanges(date, HOLIDAYS)) return "Public holiday";
        if (mmdd >= 1214 || mmdd <= 104 || (mmdd >= 330 && mmdd <= 412)) return "School break";
        if (mmdd >= 601 && mmdd <= 831) return "Summer";
        return null;
    }

    /** How much donations drop: 1.0 = normal. */
    public static double donationFactor(long day) {
        String s = seasonName(day);
        if (s == null) return 1.0;
        switch (s) {
            case "Ramadan": return 0.6;
            case "Public holiday": return 0.7;
            case "Summer": return 0.75;
            default: return 0.85;   // school break
        }
    }
}
