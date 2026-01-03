
package tn.economic.system.repository;

import tn.economic.system.ModelException.DataAccessException;
import tn.economic.system.models.FournisseurProfile;

public interface FournisseurProfileRepository {
    void upsert(FournisseurProfile profile) throws DataAccessException;
    FournisseurProfile findByUserId(Long userId) throws DataAccessException;
}