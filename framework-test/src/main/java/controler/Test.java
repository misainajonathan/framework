package controler;

import annotation.Controller;
import annotation.UrlMethode;
import jakarta.servlet.http.Part;
import annotation.RestApi;
import annotation.HttpMethod;

@Controller
public class Test {
    
    @UrlMethode(Value = "/api/test", method = HttpMethod.GET)
    @RestApi
    public String testJson(){
        return "Hello JSON";
    }

    @UrlMethode(Value = "/essai", method = HttpMethod.GET)
    public String essai(String param1, String param2){
        return "Param1: " + param1 + ", Param2: " + param2;
    }

    @UrlMethode(Value = "/testobjet", method = HttpMethod.GET)
    public String testerBindingEmploye(Employe employe) {
        if (employe != null) {
            return "Objet reçu avec succès : " + employe.toString();
        }
        return "L'objet est null";
    }

    @UrlMethode(Value = "/file", method = HttpMethod.POST)
    public String upload(Part file) {
        // Logique de traitement du fichier
        if (file==null) {
            throw new IllegalArgumentException("Le fichier ne peut pas être null");
        }

        return "Fichier reçu : " + file;
    }

    @UrlMethode(Value = "/session/set", method = HttpMethod.GET)
    public String setSession(MySession session, String nom) {
        session.put("nom", nom);
        return "Nom enregistré : " + nom;
    }

    @UrlMethode(Value = "/session/get", method = HttpMethod.GET)
    public String getSession(MySession session) {
        return "Nom en session : " + session.get("nom") + " (id = " + session.getSessionId() + ")";
    }

}