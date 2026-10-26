package util;

public class Upload {
    public static String getHtmlUpload() {
        return "<form action=\"file\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                "    <input type=\"file\" name=\"file\" id=\"file\">\n" +
                "    <input type=\"submit\" value=\"Upload\">\n" +
                "</form>";
    }
}
