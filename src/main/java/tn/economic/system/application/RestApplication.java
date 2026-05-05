package tn.economic.system.application;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.Set;
import java.util.HashSet;
import tn.economic.system.Controller.AchatController;
import tn.economic.system.Controller.AnnoncesPublicController;
import tn.economic.system.Controller.FournisseurController;
import tn.economic.system.Controller.FournisseurMetricsController;
import tn.economic.system.Controller.LoginController;
import tn.economic.system.Controller.ProduitController;
import tn.economic.system.Controller.UserController;
import tn.economic.system.Controller.CommandeController;

import org.glassfish.jersey.jackson.JacksonFeature;
@ApplicationPath("/api")
public class RestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {

        Set<Class<?>> classes = new HashSet<>();

        // Controller REST
        classes.add(UserController.class);
        classes.add(LoginController.class);
        classes.add(ProduitController.class);
        classes.add(AchatController.class);
         classes.add(FournisseurController.class);
               classes.add(CommandeController.class);
 classes.add(AnnoncesPublicController.class);
 classes.add(FournisseurMetricsController.class);

        // JSON support (Request/Response entity providers)
        classes.add(JacksonFeature.class);
        return classes;
    }
}
