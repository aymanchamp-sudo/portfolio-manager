package com.portfolio.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ActivityLog — tracks every user action for the audit trail.
 */
public class ActivityLog implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum ActionType { ADD, UPDATE, DELETE, SAVE, LOAD, EXPORT, IMPORT, CALCULATE }

    private int        id;
    private ActionType actionType;
    private String     description;
    private String     timestamp; // ISO datetime string

    public ActivityLog() {}

    public ActivityLog(int id, ActionType actionType, String description) {
        this.id          = id;
        this.actionType  = actionType;
        this.description = description;
        this.timestamp   = LocalDateTime.now()
                           .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    public String toJson() {
        return String.format(
            "{\"id\":%d,\"actionType\":\"%s\",\"description\":\"%s\",\"timestamp\":\"%s\"}",
            id,
            actionType != null ? actionType.name() : "UNKNOWN",
            description != null ? description.replace("\"", "\\\"") : "",
            timestamp != null ? timestamp : ""
        );
    }

    // Getters & Setters
    public int        getId()                        { return id; }
    public void       setId(int id)                  { this.id = id; }
    public ActionType getActionType()                { return actionType; }
    public void       setActionType(ActionType t)    { this.actionType = t; }
    public String     getDescription()               { return description; }
    public void       setDescription(String d)       { this.description = d; }
    public String     getTimestamp()                 { return timestamp; }
    public void       setTimestamp(String t)         { this.timestamp = t; }
}
