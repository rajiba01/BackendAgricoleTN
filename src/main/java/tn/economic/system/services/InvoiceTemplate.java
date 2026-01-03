package tn.economic.system.services;

import tn.economic.system.dto.AchatReceipt;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class InvoiceTemplate {

    public static String buildHtml(AchatReceipt r, String customerEmail) {
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(r.getDateAchat());

        return """
            <html>
              <body style="font-family: Arial, sans-serif;">
                <h2>Facture - Achat #%d</h2>
                <p><b>Email client:</b> %s</p>
                <p><b>Date:</b> %s</p>
                <hr/>
                <table cellpadding="8" cellspacing="0" border="1">
                  <tr>
                    <th>Produit</th><th>Quantité</th><th>Prix Unitaire</th><th>Total</th>
                  </tr>
                  <tr>
                    <td>%s (ID=%d)</td>
                    <td>%d</td>
                    <td>%.2f</td>
                    <td>%.2f</td>
                  </tr>
                </table>
                <p style="margin-top:16px;">Merci pour votre achat.</p>
              </body>
            </html>
        """.formatted(
                r.getIdAchat(),
                customerEmail,
                date,
                r.getTypeProduit(),
                r.getIdProduit(),
                r.getQuantite(),
                r.getPrixUnitaire(),
                r.getTotal()
        );
    }
}