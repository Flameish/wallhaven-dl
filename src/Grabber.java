import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Grabber {
    private String apiURL = "https://wallhaven.cc/search?{{categories}}{{purity}}{{resolutions}}{{ratios}}{{topRange}}{{sorting}}{{order}}{{search}}";
    private String saveLocation = "";
    private boolean login = false;
    private String username;
    private String password;
    private int limit = 20;
    public Map<String, String> loginCookies;

    public Grabber(Map<String, List<String>> params) {
        // Default options
        String categories = "categories=111";
        String purity = "";
        StringBuilder resolutions = new StringBuilder();
        StringBuilder ratios = new StringBuilder();
        String topRange = "";
        String sorting = "&sorting=hot";
        String order = "&order=desc";
        String search = "";
        // URL building
        // Categories (General, Anime, People)
        if(params.containsKey("categories")) {
            categories = "categories="+params.get("categories").get(0);
        }
        // Purity (SFW, Sketchy, NSFW)
        if(params.containsKey("purity")) {
            apiURL = apiURL.replace("{{purity}}","&purity="+params.get("purity").get(0));
        }
        // Resolution
        if(params.containsKey("resolutions")) {
            resolutions = new StringBuilder("&resolutions=");
            for(String param: params.get("resolutions")) {
                resolutions.append(param).append("%2C");
            }
        }
        // Ratio
        if(params.containsKey("ratios")) {
            ratios = new StringBuilder("&ratios=");
            for(String param: params.get("ratios")) {
                ratios.append(param).append("%2C");
            }
        }
        // Sorting
        if(params.containsKey("favorites")) {
            sorting = "&sorting=favorites";
        }
        if(params.containsKey("toplist")) {
            sorting = "&sorting=toplist";
            // Set date range
            if(params.get("toplist").isEmpty()) {
                topRange = "&topRange=1M";
            } else {
                topRange = "&topRange="+params.get("toplist").get(0);
            }
        }
        if(params.containsKey("views")) {
            sorting = "&sorting=views";
        }
        if(params.containsKey("relevance")) {
            sorting = "&sorting=relevance";
        }
        if(params.containsKey("date_added")) {
            sorting = "&sorting=date_added";
        }
        if(params.containsKey("hot")) {
            sorting = "&sorting=hot";
        }
        if(params.containsKey("random")) {
            sorting = "&sorting=random";
        }
        // Order
        if(params.containsKey("desc")) {
            order = "&order=desc";
        }
        if(params.containsKey("asc")) {
            order = "&order=asc";
        }
        // Search
        if(params.containsKey("search")) {
            search = "&q="+params.get("search").get(0);
        }

        apiURL = apiURL.replace("{{categories}}", categories);
        apiURL = apiURL.replace("{{purity}}", purity);
        apiURL = apiURL.replace("{{resolutions}}", resolutions.toString());
        apiURL = apiURL.replace("{{ratios}}", ratios.toString());
        apiURL = apiURL.replace("{{topRange}}",topRange);
        apiURL = apiURL.replace("{{sorting}}", sorting);
        apiURL = apiURL.replace("{{order}}", order);
        apiURL = apiURL.replace("{{search}}", search);

        // Internal options
        if(params.containsKey("limit")) {
            limit = Integer.parseInt(params.get("limit").get(0));
        }
        if(params.containsKey("saveTo")) {
            saveLocation = params.get("saveTo").get(0);
        }
        if(params.containsKey("login")) {
            login = true;
            username = params.get("login").get(0);
            password = params.get("login").get(1);
        }
        System.out.println(apiURL);
    }

    public void login() {
        try {
            System.out.print("Login...");
            // First visit to generate csrf-token
            Connection.Response res = Jsoup.connect("https://wallhaven.cc/login")
                    .method(Connection.Method.GET)
                    .execute();
            // Get login cookies
            res = Jsoup.connect("https://wallhaven.cc/auth/login")
                    .data("_token", res.parse().select("meta[name=csrf-token]").attr("content"))
                    .data("username", username)
                    .data("password", password)
                    .cookies(res.cookies())
                    .method(Connection.Method.POST)
                    .execute();
            loginCookies = res.cookies();
            System.out.println("successful");
        } catch (IOException e) {
            System.err.println("\nLogin failed: "+e.getMessage());
        }
    }

    public void download() {
        int imageCounter = 0;
        int pageCounter = 0;
        while(true) {
            try {
                // Fetch page with images
                Document doc;
                if(loginCookies != null) {
                    doc = Jsoup.connect(apiURL+"&page="+(++pageCounter)).cookies(loginCookies).get();
                } else {
                    doc = Jsoup.connect(apiURL+"&page="+(++pageCounter)).get();
                }

                // Loop through images of page
                Elements images = doc.select("figure");
                if(images.size() == 0) {
                    System.out.println("\nAll available images downloaded.");
                    return;
                }
                for(Element image: images) {
                    if(imageCounter++ >= limit) {
                        System.out.println("\nFinished.");
                        return;
                    }
                    // Build image url
                    String imageURL = image.select("img").attr("data-src")
                            .replace("//th.","//w.").replace("/small/","/full/");
                    String imageName = "wallhaven-"+image.attr("data-wallpaper-id");
                    if(!image.select(".thumb-info span.png").isEmpty()) {
                        imageName = imageName + ".png";
                    } else {
                        imageName = imageName+".jpg";
                    }
                    imageURL = imageURL.substring(0,imageURL.lastIndexOf("/"))+"/"+imageName;

                    // Actual download and write to file
                    System.out.print("Downloading["+(imageCounter)+"/"+limit+"]...\r");
                    File f = new File(saveLocation+imageName);
                    if(!f.exists()) {
                        try(InputStream in = read(new URL(imageURL))){
                            Files.copy(in, Paths.get(saveLocation+imageName));
                        }
                    } else imageCounter--;
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println(e.getMessage());
            }
        }
    }

    public boolean useLogin() {
        return login;
    }

    private static InputStream read(URL url) {
        try {
            HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:83.0) Gecko/20100101 Firefox/83.0");

            return httpcon.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
