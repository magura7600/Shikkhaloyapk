import java.net.URI;
import java.net.URL;

public class TestUrl {
    public static void main(String[] args) throws Exception {
        String urlStr = "https://pub-9c8b1336709a4769b38c8bcb72283a9e.r2.dev/HSC'%2027/%E0%A6%A4%E0%A6%A5%E0%A7%8D%E0%A6%AF%20%E0%A6%93%20%E0%A6%AF%E0%A7%8B%E0%A6%97%E0%A6%BE%E0%A6%AF%E0%A7%8B%E0%A6%97%20%E0%A6%AA%E0%A7%8D%E0%A6%B0%E0%A6%AF%E0%A7%81%E0%A6%95%E0%A7%8D%E0%A6%A4%E0%A6%BF/%E0%A6%AC%E0%A6%BF%E0%A6%B7%E0%A7%9F%E0%A6%AD%E0%A6%BF%E0%A6%A4%E0%A7%8D%E0%A6%A4%E0%A6%BF%E0%A6%95%20%E0%A6%97%E0%A6%BE%E0%A6%87%E0%A6%A1%E0%A6%B2%E0%A6%BE%E0%A6%87%E0%A6%A8ict.pdf";
        try {
            System.out.println("URL: " + urlStr);
            URL url = new URL(urlStr);
            System.out.println("Parsed URL: " + url);
            URI uri = url.toURI();
            System.out.println("Parsed URI: " + uri);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
