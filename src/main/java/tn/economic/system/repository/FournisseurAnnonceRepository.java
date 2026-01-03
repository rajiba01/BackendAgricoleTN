package tn.economic.system.repository;

import tn.economic.system.ModelException.DataAccessException;
import tn.economic.system.models.FournisseurAnnonce;

import java.util.List;

public interface FournisseurAnnonceRepository {
    Long create(FournisseurAnnonce annonce) throws DataAccessException;
    List<FournisseurAnnonce> listByUserId(Long userId) throws DataAccessException;
}