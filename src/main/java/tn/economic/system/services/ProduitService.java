package tn.economic.system.services;
import tn.economic.system.models.Produits;
import tn.economic.system.repository.ProduitRepository;

import java.util.List;

public class ProduitService implements IProduitService{
    private final ProduitRepository produitRepository = new ProduitRepository();
    public void  AjouterProduit(Produits p) {
        produitRepository.save(p);
    }
    public List<Produits> listerProduits() {
        return produitRepository.findAll();
    }
}
