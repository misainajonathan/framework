package controler;

import annotation.Controller;
import annotation.UrlKey;
import annotation.UrlMethode;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

public class FrameworkServlet extends HttpServlet {

    private Map<UrlKey, Method> mappingUrls = new HashMap<>();
    private Map<Class<?>, Object> controllerInstances = new HashMap<>();

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

    private void scanPackages(String packageName) throws Exception {
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
                            Object controllerInstance = createControllerInstance(cls);
                            controllerInstances.put(cls, controllerInstance);

                            Method[] methods = cls.getDeclaredMethods();
                            for (Method meth : methods) {
                                if (meth.isAnnotationPresent(UrlMethode.class)) {
                                    UrlMethode urlmeth = meth.getAnnotation(UrlMethode.class);
                                    String url = urlmeth.Value();
                                    String methode = urlmeth.method().name();

                                    UrlKey cle = new UrlKey(url, methode);
                                    mappingUrls.put(cle, meth);
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
        String methode = request.getMethod();

        UrlKey cle = new UrlKey(pathInfo, methode);
        Map<String, String[]> parameterMap = request.getParameterMap();
        
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head><title>Framework Mapping</title></head>");
            out.println("<body>");
            
            if (mappingUrls.containsKey(cle)) {
                Method meth = mappingUrls.get(cle);
                Object controller = controllerInstances.get(meth.getDeclaringClass());
                Object result = invokeMappedMethod(meth, controller, parameterMap);

                out.println("<h1>Méthode correspondante trouvée :</h1>");
                out.println("<p>Classe : " + meth.getDeclaringClass().getName() + "</p>");
                out.println("<p>Méthode : " + meth.getName() + " avec le type " + methode + "</p>");
                if (result != null) {
                    out.println("<p>Résultat : " + result + "</p>");
                }
            } else {
                out.println("<h1>Aucune méthode ne correspond à l'URL : " + pathInfo + "</h1>");
                out.println("<h2>Liste de toutes les méthodes disponibles :</h2>");
                if (mappingUrls.isEmpty()) {
                    out.println("<p>Aucun mapping trouvé dans le package.</p>");
                } else {
                    out.println("<table border='1'>");
                    out.println("<tr><th>URL</th><th>Classe</th><th>Méthode</th></tr>");
                    for (Map.Entry<UrlKey, Method> entry : mappingUrls.entrySet()) {
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

    private Object createControllerInstance(Class<?> controllerClass) throws ServletException {
        try {
            Constructor<?> constructor = controllerClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new ServletException("Impossible d'instancier le contrôleur : " + controllerClass.getName(), e);
        }
    }

    private Object invokeMappedMethod(Method method, Object controller, Map<String, String[]> requestParameters)
            throws ServletException {
        try {
            method.setAccessible(true);
            if (method.getParameterCount() == 0) {
                return method.invoke(controller);
            }

            if (method.getParameterCount() == 1 && Map.class.isAssignableFrom(method.getParameterTypes()[0])) {
                Map<String, String> flattenedParameters = new LinkedHashMap<>();
                for (Map.Entry<String, String[]> entry : requestParameters.entrySet()) {
                    String[] values = entry.getValue();
                    flattenedParameters.put(entry.getKey(), values != null && values.length > 0 ? values[0] : null);
                }
                return method.invoke(controller, flattenedParameters);
            }

            throw new ServletException("La méthode " + method.getName() + " doit accepter zéro paramètre ou un Map");
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ServletException("Erreur lors de l'invocation de la méthode " + method.getName(), e);
        }
    }
}