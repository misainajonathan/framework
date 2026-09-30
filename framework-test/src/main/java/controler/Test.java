package controler;

import annotation.Controller;
import annotation.UrlMethode;
import annotation.RestApi;

@Controller
public class Test {
    
    @UrlMethode(Value = "/api/test", method = HttpMethod.GET)
    @RestApi
    public String testJson() {
        return "Hello JSON";
    }
}