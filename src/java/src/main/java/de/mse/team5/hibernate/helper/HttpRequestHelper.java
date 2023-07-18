package de.mse.team5.hibernate.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.NoRouteToHostException;
import java.time.Duration;

public class HttpRequestHelper {
    private static final Logger LOG = LogManager.getLogger(HttpRequestHelper.class);
    private static final int MAX_CONNECTION_RETRIES = 15;

    public static Document downloadWebsiteContentForUrl(String url, Duration waitTime) {
        Document doc = null;
        try {
            int tries = 1;
            do {
                try {
                    //Connecting to the web page
                    Connection conn = Jsoup.connect(url);
                    conn.ignoreContentType(true);
                    conn.ignoreHttpErrors(true);
                    conn.followRedirects(true);
                    conn.header("Accept-Language", "en");
                    //executing the get request
                    Connection.Response response = conn.execute();
                    if (response.statusCode() == 200) {
                        doc = response.parse();
                    } else {
                        LOG.debug("Got HTTP STATUS " + response.statusCode() + " for " + url);
                    }
                    break;
                } catch (NoRouteToHostException e) {
                    tries += 1;
                    Thread.sleep(waitTime);
                }
                LOG.warn("Failed to find route to host " + url + " retry count=" + tries);
            } while (tries < MAX_CONNECTION_RETRIES);
        } catch (IOException | IllegalArgumentException | InterruptedException e) {
            LOG.info("Website " + url + " not available due to " + e.getMessage());
        }
        return doc;
    }
}
