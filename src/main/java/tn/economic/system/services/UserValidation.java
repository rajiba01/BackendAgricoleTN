package tn.economic.system.services;

import java.util.Set;
import tn.economic.system.enums.Role;

public final class UserValidation {
  private static final Set<String> ALLOWED_REGIONS =
      Set.of("sfax", "sahel", "centre", "nord", "sud");

  private UserValidation() {}

  public static String normalizeRegion(String localisation) {
    if (localisation == null) return null;
    String v = localisation.trim().toLowerCase();
    return v.isEmpty() ? null : v;
  }

  public static void validateLocalisationForRole(Role role, String localisation) {
    String region = normalizeRegion(localisation);

    if (role == Role.FOURNISSEUR) {
      if (region == null) {
        throw new IllegalArgumentException("localisation is required for FOURNISSEUR (sfax|sahel|centre|nord|sud)");
      }
      if (!ALLOWED_REGIONS.contains(region)) {
        throw new IllegalArgumentException("localisation must be one of: sfax, sahel, centre, nord, sud");
      }
    }
  }
}