package tn.economic.system.services;

import tn.economic.system.models.Produits;

import java.util.List;

public interface IProduitService {
    void  AjouterProduit(Produits p);
    List<Produits> listerProduits();
}
