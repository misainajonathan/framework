package util;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpSession;

public class MySession {

    // Le contexte : session_id -> (cle -> valeur)
    private static Map<String, Map<String, Object>> sessions = new HashMap<>();

    private HttpSession httpSession;
    private Map<String, Object> donnees;

    public MySession(HttpSession httpSession) {
        this.httpSession = httpSession;
        String id = httpSession.getId();
        synchronized (sessions) {
            Map<String, Object> existant = sessions.get(id);
            if (existant == null) {
                existant = new HashMap<>();
                sessions.put(id, existant);
            }
            this.donnees = existant;
        }
    }

    public static void supprimer(String sessionId) {
        synchronized (sessions) {
            sessions.remove(sessionId);
        }
    }

    public String getSessionId() {
        return httpSession.getId();
    }

    public Object get(String cle) {
        return donnees.get(cle);
    }

    public void put(String cle, Object valeur) {
        donnees.put(cle, valeur);
    }

    public void remove(String cle) {
        donnees.remove(cle);
    }

    public void invalidate() {
        httpSession.invalidate();
    }
}