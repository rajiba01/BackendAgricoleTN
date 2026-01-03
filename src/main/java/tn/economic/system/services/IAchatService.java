package tn.economic.system.services;

import tn.economic.system.dto.AchatReceipt;

public interface IAchatService {
    void acheterProduit(Long idProduit, int quantite);

    // nouvelle méthode (pour facture / email)
    AchatReceipt acheterProduitEtEnvoyerFacture(Long idProduit, int quantite, String email);
}