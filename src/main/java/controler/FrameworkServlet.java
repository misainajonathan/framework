package controler;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FrameworkServlet extends HttpServlet{
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException{
        try {
            String path = request.getPathInfo();
            response.getWriter().write("Hello from FrameworkServlet! You requested: " + path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}