package com.example.premiereversion;

public class User {
    private String nom;
    private String prenom;
    private String email;
    private String mot_de_passe;

    public User(String nom, String prenom, String email, String mot_de_passe) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.mot_de_passe = mot_de_passe;
    }

    public String getJson() {
        return "{ \"nom\": \"" + nom + "\", " +
                "\"prenom\": \"" + prenom + "\", " +
                "\"email\": \"" + email + "\", " +
                "\"mot_de_passe\": \"" + mot_de_passe + "\" }";
    }
}
