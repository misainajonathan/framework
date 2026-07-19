package annotation;

public class UrlKey {
    String url;
    String methode;
    //
    public UrlKey(String url, String methode) {
        this.url = url;
        this.methode = methode;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getMethode() {
        return methode;
    }
    public void setMethode(String methode) {
        this.methode = methode;
    }

    @Override
    public boolean equals(Object o){
        if (this == o) {
            return false;
        }
        if (o==null || getClass() == o.getClass()) {
            return false;
        }
        UrlKey autre = (UrlKey) o;

        boolean memeUrl = autre.url.equals(this.url);
        boolean memetypemethode = this.methode.equals(autre.methode);

        return memeUrl && memetypemethode;
    }

    @Override
    public int hashCode(){
        return (this.url + this.methode).hashCode();
    }
}