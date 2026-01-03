package tn.economic.system.application;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import jakarta.servlet.MultipartConfigElement;
import java.nio.file.Files;
import java.nio.file.Path;

import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.ServletProperties;
import tn.economic.system.config.CorsFilter;

public class Main {

    public static void main(String[] args) throws Exception {

        // Serveur Jetty
        Server server = new Server(8080);

        HandlerList handlers = new HandlerList();

        // ---- Static /uploads/* handler ----
        Path uploadsDir = Path.of("uploads").toAbsolutePath().normalize();
        ResourceHandler uploadsResourceHandler = new ResourceHandler();
        uploadsResourceHandler.setDirectoriesListed(false);
        uploadsResourceHandler.setWelcomeFiles(new String[]{});
        uploadsResourceHandler.setResourceBase(uploadsDir.toString());

        ContextHandler uploadsContext = new ContextHandler("/uploads");
        uploadsContext.setHandler(uploadsResourceHandler);
        handlers.addHandler(uploadsContext);

        // ---- API /api/* handler (Jersey) ----
        ServletContextHandler apiContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        apiContext.setContextPath("/");
        // Enregistrer le filtre CORS pour toutes les requêtes
        FilterHolder cors = new FilterHolder(new CorsFilter());
        apiContext.addFilter(cors, "/*", null);

        // Jersey (REST)
        ServletHolder jerseyServlet =
                apiContext.addServlet(ServletContainer.class, "/api/*");

        // Enable multipart/form-data parsing for servlet requests (required for request.getPart()).
        // Without this, Jetty throws: "No multipart config for servlet".
        String tmpDir = Files.createTempDirectory("jetty-multipart").toAbsolutePath().toString();
        MultipartConfigElement multipartConfig = new MultipartConfigElement(tmpDir);
        jerseyServlet.getRegistration().setMultipartConfig(multipartConfig);

        jerseyServlet.setInitParameter(
                ServletProperties.JAXRS_APPLICATION_CLASS,
                "tn.economic.system.application.RestApplication"
        );

        handlers.addHandler(apiContext);
        server.setHandler(handlers);

        server.start();

        System.out.println("Server started at http://localhost:8080");
        System.out.println("POST User API -> http://localhost:8080/api/users");

        server.join();
    }
}
