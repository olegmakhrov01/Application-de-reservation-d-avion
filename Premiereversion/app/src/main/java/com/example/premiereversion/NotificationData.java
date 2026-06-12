package com.example.premiereversion;

public class NotificationData {
    private String titre;
    private String message;

    public NotificationData(String titre, String message) {
        this.titre = titre;
        this.message = message;
    }

    public String getTitre() {
        return titre;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return titre + ": " + message;
    }
}