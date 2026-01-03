
package tn.economic.system.services;

import tn.economic.system.dto.AchatReceipt;

public class FactureTemplate {

  public static String buildClientMail(String otp, AchatReceipt r) {
    return """
      <div style="font-family: Arial, sans-serif; line-height: 1.6;">
        <h2 style="color:#2E7D32;">Votre facture</h2>
        <p>Merci pour votre commande.</p>

        <h3>Détails</h3>
        <ul>
          <li><b>Commande ID:</b> %d</li>
          <li><b>Produit:</b> %s</li>
          <li><b>Quantité:</b> %d</li>
          <li><b>Prix unitaire:</b> %.2f TND</li>
          <li><b>Total:</b> <b>%.2f TND</b></li>
        </ul>

        <h3>Code de livraison (OTP)</h3>
        <p style="font-size:22px; font-weight:bold; letter-spacing:2px;">%s</p>
        <p>Donnez ce code au fournisseur lors de la livraison.</p>

        <hr/>
        <small>Economic System</small>
      </div>
    """.formatted(
      r.getIdAchat(), r.getTypeProduit(), r.getQuantite(), r.getPrixUnitaire(), r.getTotal(), otp
    );
  }
}