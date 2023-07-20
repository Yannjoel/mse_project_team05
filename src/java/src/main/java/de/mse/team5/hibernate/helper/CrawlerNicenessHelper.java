package de.mse.team5.hibernate.helper;

import com.panforge.robotstxt.Grant;
import com.panforge.robotstxt.RobotsTxt;
import de.mse.team5.hibernate.model.Website;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Class providing information about crawl delay and allowed paths for websites
 */
public class CrawlerNicenessHelper {

    private static final String USER_AGENT = "mse_project_crawler";
    private static final int DEFAULT_CRAWL_DELAY_IN_SECONDS = 1;

    //No need to introduce lazy load since the class is always required while running the crawler
    private static final Logger LOG = LogManager.getLogger(CrawlerNicenessHelper.class);
    private static final CrawlerNicenessHelper singletonInstance = new CrawlerNicenessHelper();
    private final Map<String, RobotsTxt> cachedRobotsTxt;

    private CrawlerNicenessHelper() {
        //prevent initialization - use getCrawlerNicenessHelper instead
        this.cachedRobotsTxt = new HashMap<>();
    }


    /**
     * CrawlerNicenessHelper should be a singleton to ensure that it's cache is used globally
     * @return The singleton instance of the CrawlerNicenessHelper
     */
    public static CrawlerNicenessHelper getCrawlerNicenessHelper() {
        return singletonInstance;
    }


    public boolean isUrlAllowedByRobotsTxt(Website link) {
        if (link.getHostUrl() == null) {
            //we don't want to crawl links without a valid url
            return false;
        }
        String hostUrl = link.getHostUrl();
        String urlPath = link.getUrlPath();
        RobotsTxt robotsTxt = getRobotsTextForUrl(hostUrl);
        if (robotsTxt == null) {
            //assume no restriction if there's no robots.txt
            return true;
        }
        return robotsTxt.query(USER_AGENT, urlPath);
    }

    public int getCrawlDelay(String host) {
        int crawlDelay = DEFAULT_CRAWL_DELAY_IN_SECONDS;
        RobotsTxt robotsTxt = getRobotsTextForUrl(host);
        if (robotsTxt != null) {
            Grant grantedAccess = robotsTxt.ask(USER_AGENT, "/");
            if (grantedAccess != null && grantedAccess.getCrawlDelay() != null) {
                crawlDelay = grantedAccess.getCrawlDelay();
            }
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
        RobotsTxt robotsTxt = null;
        if (cachedRobotsTxt.containsKey(host)) {
            robotsTxt = cachedRobotsTxt.get(host);
        } else {
            String robotsTxtUrl = host + "/robots.txt";
            Document robotsTxtWebsite = HttpRequestHelper.downloadWebsiteContentForUrl(robotsTxtUrl);
            if (robotsTxtWebsite != null) {
                InputStream inputStream = IOUtils.toInputStream(robotsTxtWebsite.body().text(), StandardCharsets.UTF_8);
                try {
                    robotsTxt = RobotsTxt.read(inputStream);
                } catch (IOException e) {
                    LOG.warn(MessageFormat.format("Failed to load robots txt at {0} due to ", robotsTxtUrl), e);
                }
                this.cachedRobotsTxt.put(host, robotsTxt);
            }
        }
        return robotsTxt;
    }
}
