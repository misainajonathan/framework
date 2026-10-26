package controler;

import annotation.Controller;
import annotation.RestApi;
import annotation.UrlKey;
import annotation.UrlMethode;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import util.*;

@MultipartConfig 
public class FrameworkServlet extends HttpServlet {

    private Map<UrlKey, Method> mappingUrls = new HashMap<>();
    private Map<Class<?>, Object> controllerInstances = new HashMap<>();
    private String prefix = "";
    private String suffix = "";
    private String htmlupload = Upload.getHtmlUpload();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String packageToScan = config.getInitParameter("scan-package");
        String initPrefix = config.getInitParameter("prefix");
        String initSuffix = config.getInitParameter("suffix");

        if (initPrefix != null) {
            prefix = initPrefix;
        }

        if (initSuffix != null) {
            suffix = initSuffix;
        }

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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String pathInfo = request.getPathInfo();
    String methode = request.getMethod();

    UrlKey cle = new UrlKey(pathInfo, methode);
    Map<String, String[]> parameterMap = request.getParameterMap();
    Object[] args = null;
    
    if (mappingUrls.containsKey(cle)) {
        Method meth = mappingUrls.get(cle);
        try {
            Parameter[] parameters = meth.getParameters();
            args = new Object[parameters.length]; // Initialisé avec des null partout

            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                Class<?> paramType = param.getType();
                String parametreName = param.getName(); // Nom de la variable (ou argument)

                if (isTypeSimple(paramType)) {
                    if (parameterMap.containsKey(parametreName)) {
                        String[] valeurs = parameterMap.get(parametreName);
                        if (valeurs != null && valeurs.length > 0) {
                            args[i] = convertirParametre(valeurs[0], paramType);
                        } else {
                            args[i] = null;
                        }
                    } else {
                        args[i] = null;
                    }
                } 
                else {
                    Object objetComplexe = instancierEtRemplirObjet(paramType, parameterMap);
                    args[i] = objetComplexe;
                }
            }

        } catch (Exception e) {
            throw new ServletException("Erreur lors du binding des paramètres pour la méthode " + meth.getName(), e);
        }
        
        Object controller = controllerInstances.get(meth.getDeclaringClass());
        if (cle.getUrl().contains("file")) {
            Map<String, Parameter> params = null;
            try {
                params = getMethodParameters(meth);
                if (params==null) {
                    throw new ServletException("Aucun paramètre trouvé pour la méthode " + meth.getName());
                }

                Part filePart = request.getPart("file");
                if (filePart == null) {
                    throw new ServletException("Aucun fichier uploadé pour la méthode " + meth.getName());
                }

                String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                String destination = getProperties("src/main/resources/application.properties").getProperty("upload.directory");
                if (destination == null || destination.trim().isEmpty()) {
                    throw new ServletException("Le répertoire de destination pour l'upload n'est pas défini dans application.properties");
                }
                File uploadDir = new File(destination);
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                String filePath = destination + File.separator + fileName;
                filePart.write(filePath);

            } catch (Exception e) {
                throw new ServletException("Erreur lors de l'upload du fichier pour la méthode " + meth.getName(), e);
            }

        }

        Object result = invokeMappedMethodDynamique(meth, controller, args);

        if (meth.isAnnotationPresent(RestApi.class)) {
            response.setContentType("application/json;charset=UTF-8");
            String json = convertirEnJson(result);
            PrintWriter out = response.getWriter();
            out.write(json);
            out.flush();
            return;   
        }


        if (result instanceof ModelAndView) {
            traiterModelAndView((ModelAndView) result, request, response);
            return;   
        }

        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html><head><title>Framework Mapping</title></head><body>");
            out.println("<h1>Méthode correspondante trouvée :</h1>");
            out.println("<p>Classe : " + meth.getDeclaringClass().getName() + "</p>");
            out.println("<p>Méthode : " + meth.getName() + " avec le type " + methode + "</p>");
            if (result != null) {
                out.println("<p>Résultat : " + result + "</p>");
            }
            out.println("</body></html>");
        }

        } else {
            response.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.println("<!DOCTYPE html>");
                out.println("<html><head><title>Framework Mapping</title></head><body>");
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
                    out.println(htmlupload);
                }
                out.println("</body></html>");
            }
        }
    }

    private Object createControllerInstance(Class<?> controllerClass) throws ServletException {
        try {
            Constructor<?> constructor = controllerClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
                | InvocationTargetException e) {
            throw new ServletException("Impossible d'instancier le contrôleur : " + controllerClass.getName(), e);
        }
    }

    private Object invokeMappedMethodDynamique(Method method, Object controller, Object[] args)
            throws ServletException {
        try {
            method.setAccessible(true);
            return method.invoke(controller, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ServletException("Erreur lors de l'invocation de la méthode " + method.getName(), e);
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

    private String convertirEnJson(Object result) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        return gson.toJson(result);
    }

    public void traiterModelAndView(ModelAndView mv, HttpServletRequest req, HttpServletResponse res)
            throws ServletException {
        if (mv == null) {
            return;
        }

        String viewName = mv.getViewName();
        Map<String, Object> model = mv.getModel();

        for (Map.Entry<String, Object> entry : model.entrySet()) {
            req.setAttribute(entry.getKey(), entry.getValue());
        }

        try {
            RequestDispatcher dispatcher = req.getRequestDispatcher(prefix + viewName + suffix);
            dispatcher.forward(req, res);
        } catch (Exception e) {
            throw new ServletException("Erreur lors du forwarding vers la vue", e);
        }
    }


    public Map<String, Parameter> getMethodParameters(Method method) throws Exception {
        Map<String, Parameter> methodParameters = new HashMap<>();
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters){
            String paramName = parameter.getName();
            Class<?> paramType = parameter.getType();
            System.out.println("Paramètre : " + paramName + ", Type : " + paramType.getName());
            methodParameters.put(paramName, parameter);
        }
        return methodParameters;
    }

    public Object convertirParametre(String paramValue, Class<?> paramType) throws Exception {
        if (paramType == String.class) {
            return paramValue;
        } else if (paramType == int.class || paramType == Integer.class) {
            return Integer.parseInt(paramValue);
        } else if (paramType == long.class || paramType == Long.class) {
            return Long.parseLong(paramValue);
        } else if (paramType == double.class || paramType == Double.class) {
            return Double.parseDouble(paramValue);
        } else if (paramType == boolean.class || paramType == Boolean.class) {
            return Boolean.parseBoolean(paramValue);
        } else {
            throw new Exception("Conversion non supportée pour le type : " + paramType.getName());
        }
    }

    private boolean isTypeSimple(Class<?> type) {
        return type == String.class ||
               type == int.class || type == Integer.class ||
               type == long.class || type == Long.class ||
               type == double.class || type == Double.class ||
               type == boolean.class || type == Boolean.class;
    }

    private Object instancierEtRemplirObjet(Class<?> clazz, Map<String, String[]> parameterMap) throws Exception {
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();

        java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName(); // ex: "nom", "age"

            if (parameterMap.containsKey(fieldName)) {
                String[] valeurs = parameterMap.get(fieldName);
                if (valeurs != null && valeurs.length > 0) {
                    String valeurBrute = valeurs[0];
                    
                    Object valeurConvertie = convertirParametre(valeurBrute, field.getType());
                    
                    field.set(instance, valeurConvertie);
                }
            }
        }

        return instance;
    }

    public Properties getProperties(String filePath) throws IOException {
        Properties properties = new Properties();
        try (java.io.InputStream input = getServletContext().getResourceAsStream(filePath)) {
            properties.load(input);
        }
        return properties;
    }
}