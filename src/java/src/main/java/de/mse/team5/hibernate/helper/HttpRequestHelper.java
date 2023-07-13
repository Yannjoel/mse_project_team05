package de.mse.team5.hibernate.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class HttpRequestHelper {
    private static final Logger LOG = LogManager.getLogger(HttpRequestHelper.class);

    public static Document downloadWebsiteContentForUrl(String url) {
        Document doc = null;
        try {
            //Connecting to the web page
            Connection conn = Jsoup.connect(url);
            conn.ignoreContentType(true);
            conn.ignoreHttpErrors(true);
            conn.header("Accept-Language", "en");
            //executing the get request
            doc = conn.get();
        } catch (HttpStatusException e) {
            LOG.info(e.getMessage());
        } catch (IOException e) {
            LOG.warn("Website " + url + " not available due to " + e.getMessage());
        }
        return doc;
    }
}
