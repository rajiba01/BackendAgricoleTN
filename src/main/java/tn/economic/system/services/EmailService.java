
package tn.economic.system.services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class EmailService {

  private final Properties cfg = new Properties();

  public EmailService() {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("mail.properties")) {
      if (in != null) {
        cfg.load(in);
      } else {
        // Backward-compatible default: allow app to run even if mail.properties isn't provided
        // (email sending will fail with a clear error if used).
        cfg.put("mail.smtp.host", "localhost");
        cfg.put("mail.smtp.port", "25");
        cfg.put("mail.smtp.auth", "false");
        cfg.put("mail.smtp.starttls.enable", "false");
      }
    } catch (Exception e) {
      throw new IllegalStateException("Erreur chargement mail.properties", e);
    }
  }

  /**
   * Backward-compatible constructor for older callers.
   * <p>
   * The current implementation loads configuration from {@code mail.properties} on the classpath,
   * so these parameters are ignored.
   */
  public EmailService(String host, int port, String username, String password) {
    this();
  }

  public void sendHtml(String to, String subject, String html) {
    try {
      Properties props = new Properties();
      props.put("mail.smtp.host", cfg.getProperty("mail.smtp.host"));
      props.put("mail.smtp.port", cfg.getProperty("mail.smtp.port"));
      props.put("mail.smtp.auth", cfg.getProperty("mail.smtp.auth"));
      props.put("mail.smtp.starttls.enable", cfg.getProperty("mail.smtp.starttls.enable"));

      final String username = cfg.getProperty("mail.username");
      final String password = cfg.getProperty("mail.password");
      final String from = cfg.getProperty("mail.from", username);

      Session session = Session.getInstance(props, new Authenticator() {
        @Override protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(username, password);
        }
      });

      Message msg = new MimeMessage(session);
      msg.setFrom(new InternetAddress(from));
      msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
      msg.setSubject(subject);
      msg.setContent(html, "text/html; charset=" + StandardCharsets.UTF_8.name());

      Transport.send(msg);

    } catch (MessagingException e) {
      throw new RuntimeException("Erreur envoi mail: " + e.getMessage(), e);
    }
  }
}