package tn.economic.system.models;

import java.util.Date;

public class FournisseurProfile {
    private Long id;        // ID
    private Long userId;    // USER_ID
    private String societe; // SOCIETE
    private String tel;     // TEL
    private String adresse; // ADRESSE
    private Date createdAt; // CREATED_AT

    public FournisseurProfile() {}

    public FournisseurProfile(Long id, Long userId, String societe, String tel, String adresse, Date createdAt) {
        this.id = id;
        this.userId = userId;
        this.societe = societe;
        this.tel = tel;
        this.adresse = adresse;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getSociete() { return societe; }
    public void setSociete(String societe) { this.societe = societe; }

    public String getTel() { return tel; }
    public void setTel(String tel) { this.tel = tel; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}