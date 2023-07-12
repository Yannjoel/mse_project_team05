package de.mse.team5.crawler.backgroundtasks;

import de.mse.team5.crawler.dto.DownloadedDocDTO;
import de.mse.team5.hibernate.helper.HttpRequestHelper;
import de.mse.team5.hibernate.model.Website;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static de.mse.team5.hibernate.helper.CrawlerNicenessHelper.getCrawlerNicenessHelper;

public class DownloadedWebsiteToRamRunner implements Runnable {
    public static final Logger LOG = LogManager.getLogger(DownloadedWebsiteToRamRunner.class);
    private final Set<String> currentlyCrawledHostsCopy;
    private final Set<Website> sitesToDownload;
    private final String host;
    private final Set<DownloadedDocDTO> downloadedDocsToProcessCopy;

    public DownloadedWebsiteToRamRunner(Set<Website> sitesToDownload, Set<String> currentlyCrawledHosts, String host, Set<DownloadedDocDTO> downloadedDocsToProcess) {
        this.sitesToDownload = sitesToDownload;
        this.currentlyCrawledHostsCopy = currentlyCrawledHosts;
        this.host = host;
        this.downloadedDocsToProcessCopy = downloadedDocsToProcess;
    }

    @Override
    public void run() {
        int hostCrawlDelay = getCrawlerNicenessHelper().getCrawlDelay(host);

        Duration waitTime = Duration.of(hostCrawlDelay, ChronoUnit.SECONDS);
        for (Website site : sitesToDownload) {
            String siteUrl = site.getUrl();
            LOG.debug("fetching " + siteUrl);
            //download website
            Document doc = null;
            boolean blockedByRobotsTxt = true;
            if (getCrawlerNicenessHelper().isUrlAllowedByRobotsTxt(site)) {
                blockedByRobotsTxt = false;
                doc = HttpRequestHelper.downloadWebsiteContentForUrl(siteUrl);
            }
            site.setBlockedByRobotsTxt(blockedByRobotsTxt);
            DownloadedDocDTO downloadDoc = new DownloadedDocDTO(site, doc, blockedByRobotsTxt, Instant.now());
            if (doc == null) {
                continue;
            }
            downloadedDocsToProcessCopy.add(downloadDoc);
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                LOG.warn("Waiting interrupted due to ", e);
            }
        }
        currentlyCrawledHostsCopy.remove(host);
    }
}
