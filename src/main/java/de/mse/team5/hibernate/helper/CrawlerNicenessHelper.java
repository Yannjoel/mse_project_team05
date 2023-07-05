package de.mse.team5.hibernate.helper;

import com.panforge.robotstxt.Grant;
import com.panforge.robotstxt.RobotsTxt;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CrawlerNicenessHelper {

    private static final String USER_AGENT = "mse_project_crawler";
    private static final int DEFAULT_CRAWL_DELAY_IN_SECONDS = 5;
    private static final Logger LOG = LogManager.getLogger(CrawlerNicenessHelper.class);
    private static CrawlerNicenessHelper singletonInstance;
    private final Map<String, RobotsTxt> cachedRobotsTxt;
    private final Map<String, Instant> domainCrawlTimes;

    private CrawlerNicenessHelper() {
        //prevent initialization - use getCrawlerNicenessHelper instead
        this.cachedRobotsTxt = new HashMap<>();
        this.domainCrawlTimes = new HashMap<>();
    }



    public static CrawlerNicenessHelper getCrawlerNicenessHelper() {
        if (singletonInstance == null) {
            singletonInstance = new CrawlerNicenessHelper();
        }
        return singletonInstance;
    }


    public boolean isUrlAllowedByRobotsTxt(String fullUrl) {
        try {
            URL url = URI.create(fullUrl).toURL();
            String baseUrl = url.getHost();
            String urlPath = url.getFile();
            return getRobotsTextForUrl(baseUrl).query(USER_AGENT, urlPath);
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public Instant getNextCrawlTime(String fullUrl) {

        Instant nextCrawlTime = null;
        try {
            URL url = URI.create(fullUrl).toURL();
            String host = url.getHost();
            String urlPath = url.getFile();

            int crawlDelayInSec = getCrawlDelay(host, urlPath);

            if(domainCrawlTimes.containsKey(host)){
                Instant lastCrawlTime = domainCrawlTimes.get(host);
                nextCrawlTime = lastCrawlTime.plusSeconds(crawlDelayInSec);
            }
            else{
                nextCrawlTime = Instant.now();
            }
            domainCrawlTimes.put(host, nextCrawlTime);
        } catch (MalformedURLException e) {
            LOG.warn("Error while calculation delay for " + fullUrl + " - caused by", e);
        }
        return nextCrawlTime;
    }

    public int getCrawlDelay(String host, String urlPath) {
        int crawlDelay = DEFAULT_CRAWL_DELAY_IN_SECONDS;
            Grant grantedAccess = getRobotsTextForUrl(host).ask(USER_AGENT, urlPath);
            if (grantedAccess != null && grantedAccess.getCrawlDelay() != null) {
                crawlDelay = grantedAccess.getCrawlDelay();
            }
        return crawlDelay;
    }

    /**
     * Gets the RobotsTxt information for a base url either from the cache or from the website itself
     *
     * @param host base url of the website (www.example.com)
     * @return RobotsTxt object of the website (if available)
     */
    private RobotsTxt getRobotsTextForUrl(String host) {
        RobotsTxt robotsTxt;
        if (cachedRobotsTxt.containsKey(host)) {
            robotsTxt = cachedRobotsTxt.get(host);
        } else {
            Document robotsTxtWebsite = HttpRequestHelper.downloadWebsiteForUrl("http://" + host + "/robots.txt");
            InputStream inputStream = IOUtils.toInputStream(robotsTxtWebsite.body().text(), StandardCharsets.UTF_8);
            try {
                robotsTxt = RobotsTxt.read(inputStream);
                this.cachedRobotsTxt.put(host, robotsTxt);
            } catch (IOException e) {
                //ToDo: Error handling
                throw new RuntimeException(e);
            }
        }

        return robotsTxt;
    }
}
