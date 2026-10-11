package controler;

import annotation.Controller;
import annotation.UrlMethode;
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

}