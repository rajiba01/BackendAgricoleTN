package tn.economic.system.config;

import org.glassfish.jersey.server.ResourceConfig;

public class JerseyConfig extends ResourceConfig {
  public JerseyConfig() {
    // NOTE: your package is `tn.economic.system.Controller` (capital C)
    packages("tn.economic.system.Controller");

    // Multipart is optional at runtime; register it if present.
    try {
      // Some environments may have the feature class but miss its transitive deps,
      // which can still crash the app during class loading.
      Class.forName("org.glassfish.jersey.media.multipart.FormDataContentDisposition");
      Class<?> mp = Class.forName("org.glassfish.jersey.media.multipart.MultiPartFeature");
      register(mp);
    } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
      // jersey-media-multipart not on classpath; endpoints not requiring multipart still work.
    }
  }
}