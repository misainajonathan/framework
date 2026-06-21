package controler;

import annotation.Controller;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FrameworkServlet extends HttpServlet {

    private List<String> annotatedClasses = new ArrayList<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        // 1. Récupérer le nom du package défini dans le web.xml
        String packageToScan = config.getInitParameter("scan-package");
        
        if (packageToScan != null && !packageToScan.trim().isEmpty()) {
            try {
                // 2. Scanner le package pour trouver les classes
                scanPackages(packageToScan);
            } catch (Exception e) {
                throw new ServletException("Erreur lors du scan du package : " + packageToScan, e);
            }
        }
    }

    private void scanPackages(String packageName) throws ClassNotFoundException {
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);

        if (resource == null) {
            return;
        }

        File directory = new File(resource.getFile());
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".class")) {
                        String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                        Class<?> cls = Class.forName(className);
                        
                        // 3. Vérifier si la classe possède l'annotation @Controller
                        if (cls.isAnnotationPresent(Controller.class)) {
                            annotatedClasses.add(cls.getName());
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head><title>Framework Scan</title></head>");
            out.println("<body>");
            out.println("<h1>Liste des classes annotées avec @Controller :</h1>");
            
            if (annotatedClasses.isEmpty()) {
                out.println("<p>Aucune classe trouvée ou annotée.</p>");
            } else {
                out.println("<ul>");
                for (String className : annotatedClasses) {
                    out.println("<li>" + className + "</li>");
                }
                out.println("</ul>");
            }
            
            out.println("</body>");
            out.println("</html>");
        }
    }
}