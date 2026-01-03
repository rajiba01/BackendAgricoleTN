package tn.economic.system.models;


import tn.economic.system.enums.Role;


public class User {

    private Long id;

    private String email;
    private String nom;
    private String prenom;
    private Role role;
    private int tel;
    private String localisation;
    private String mdp;


    public User() {
    }

    // 🔹 Constructeur avec paramètres (optionnel)
    public User(String email, String nom, String prenom,
                Role role, int tel, String localisation, String mdp) {

        this.email = email;
        this.nom = nom;
        this.prenom = prenom;
        this.role = role;
        this.tel = tel;
        this.localisation = localisation;
        this.mdp = mdp;
    }

    // 🔹 Getters & Setters
    public Long getId() { return id; }
public void setId(Long id){
        this.id=id;}
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public int getTel() { return tel; }
    public void setTel(int tel) { this.tel = tel; }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    public String getMdp() { return mdp; }
    public void setMdp(String mdp) { this.mdp = mdp; }
}

