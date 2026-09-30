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
    
}