package tn.economic.system.repository;

import tn.economic.system.models.Produits;
import tn.economic.system.enums.ProduitType;
import java.util.List;

public interface IProduitRepository {
    void save(Produits produits);
    List<Produits> findAll();
    Long findIdByType(ProduitType type);
}
