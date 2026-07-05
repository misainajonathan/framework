package controler;

import annotation.Controller;
import annotation.UrlMapping;

@Controller
public class Test {
    
    @UrlMapping("/test")
    public String test() {
        return "test";
    }
}