package com.example.qatra;

import java.io.Serializable;

/** <<entity>> Deferral: a period when the donor may not donate. */
public class Deferral implements Serializable {
    private String reason;
    private long endDate;   // day number

    public Deferral(String reason, long endDate) {
        this.reason = reason;
        this.endDate = endDate;
    }

    public String getReason() { return reason; }
    public long getEndDate() { return endDate; }
}
