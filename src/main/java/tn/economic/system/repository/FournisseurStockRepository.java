package tn.economic.system.repository;

import tn.economic.system.ModelException.DataAccessException;

public interface FournisseurStockRepository {
    void adjust(Long userId, Long produitId, int deltaQty) throws DataAccessException;
    Integer getQty(Long userId, Long produitId) throws DataAccessException;
}
