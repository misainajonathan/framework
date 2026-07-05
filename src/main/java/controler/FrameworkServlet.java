package controler;

import annotation.Controller;
import annotation.UrlMapping;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class FrameworkServlet extends HttpServlet {

    private Map<String, Method> mappingUrls = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        String packageToScan = config.getInitParameter("scan-package");
        
        if (packageToScan != null && !packageToScan.trim().isEmpty()) {
            try {
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
                        
                        if (cls.isAnnotationPresent(Controller.class)) {
                            Method[] methods = cls.getDeclaredMethods();
                            for (Method meth : methods) {
                                if (meth.isAnnotationPresent(UrlMapping.class)) {
                                    UrlMapping urlMapping = meth.getAnnotation(UrlMapping.class);
                                    mappingUrls.put(urlMapping.value(), meth);
                                }
                            }
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
        String pathInfo = request.getPathInfo();
        
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head><title>Framework Mapping</title></head>");
            out.println("<body>");
            
            if (mappingUrls.containsKey(pathInfo)) {
                Method meth = mappingUrls.get(pathInfo);
                out.println("<h1>Méthode correspondante trouvée :</h1>");
                out.println("<p>Classe : " + meth.getDeclaringClass().getName() + "</p>");
                out.println("<p>Méthode : " + meth.getName() + "</p>");
            } else {
                out.println("<h1>Aucune méthode ne correspond à l'URL : " + pathInfo + "</h1>");
                out.println("<h2>Liste de toutes les méthodes disponibles :</h2>");
                if (mappingUrls.isEmpty()) {
                    out.println("<p>Aucun mapping trouvé dans le package.</p>");
                } else {
                    out.println("<table border='1'>");
                    out.println("<tr><th>URL</th><th>Classe</th><th>Méthode</th></tr>");
                    for (Map.Entry<String, Method> entry : mappingUrls.entrySet()) {
                        out.println("<tr>");
                        out.println("<td>" + entry.getKey() + "</td>");
                        out.println("<td>" + entry.getValue().getDeclaringClass().getName() + "</td>");
                        out.println("<td>" + entry.getValue().getName() + "</td>");
                        out.println("</tr>");
                    }
                    out.println("</table>");
                }
            }
            
            out.println("</body>");
            out.println("</html>");
        }
    }
}