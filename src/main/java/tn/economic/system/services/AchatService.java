package tn.economic.system.services;

import tn.economic.system.dto.AchatReceipt;
import tn.economic.system.repository.AchatRepository;

public class AchatService implements IAchatService {

    private final AchatRepository achatRepository = new AchatRepository();

    @Override
    public void acheterProduit(Long idProduit, int quantite) {
        // ancien
        achatRepository.acheterProduit(idProduit, quantite);
    }

    public AchatReceipt acheterProduitEtEnvoyerFacture(Long idProduit, int quantite, String email) {
        AchatReceipt receipt = achatRepository.acheterProduitEtRetournerReceipt(idProduit, quantite);

        String html = InvoiceTemplate.buildHtml(receipt, email);

        EmailService emailService = new EmailService();
        // async simple
        new Thread(() -> emailService.sendHtml(email, "Votre facture d'achat #" + receipt.getIdAchat(), html)).start();
        return receipt;
    }
}