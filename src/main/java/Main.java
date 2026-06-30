import controler.FrameworkServlet;

public class Main {
    public static void main(String[] args){
        FrameworkServlet f = new FrameworkServlet();
        try {
            f.init();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }
}
