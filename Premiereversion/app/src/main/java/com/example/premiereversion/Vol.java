package com.example.premiereversion;

public class Vol {
    private int id;
    private String date;
    private String depart;
    private String destination;
    private double prix;

    public Vol(int id, String date, String depart, String destination, double prix) {
        this.id = id;
        this.date = date;
        this.depart = depart;
        this.destination = destination;
        this.prix = prix;
    }

    public int getId() { return id; }
    public String getDate() { return date; }
    public String getDepart() { return depart; }
    public String getDestination() { return destination; }
    public double getPrix() { return prix; }
}
