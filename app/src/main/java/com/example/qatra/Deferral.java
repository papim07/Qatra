package com.example.qatra;

import java.io.Serializable;

public class Deferral implements Serializable {
    private String reason;
    private long endDate;

    public Deferral(String reason, long endDate) {
        this.reason = reason;
        this.endDate = endDate;
    }

    public String getReason() { return reason; }
    public long getEndDate() { return endDate; }
}
