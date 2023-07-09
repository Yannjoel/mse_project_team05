package de.mse.team5.crawler.backgroundtasks;

import de.mse.team5.crawler.MultithreadedCrawler;
import de.mse.team5.hibernate.helper.CrawlerNicenessHelper;
import de.mse.team5.hibernate.helper.HttpRequestHelper;
import de.mse.team5.hibernate.model.Link;
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
    private final Set<String> urlsToDownload;
    private final String host;
    private final Set<Document> downloadedDocsToProcessCopy;

    public DownloadedWebsiteToRamRunner(Set<String> urlsOnHostToDownload, Set<String> currentlyCrawledHosts, String host, Set<Document> downloadedDocsToProcess) {
        this.urlsToDownload = urlsOnHostToDownload;
        this.currentlyCrawledHostsCopy = currentlyCrawledHosts;
        this.host = host;
        this.downloadedDocsToProcessCopy =downloadedDocsToProcess;
    }

    @Override
    public void run() {
        Link hostLink = Link.createNewLink(this.host);
        int hostCrawlDelay = getCrawlerNicenessHelper().getCrawlDelay(hostLink);
        Duration waitTime = Duration.of(hostCrawlDelay, ChronoUnit.SECONDS);
        for (String url : urlsToDownload) {
            LOG.debug("fetching " + url);
            //download website
            Document doc = HttpRequestHelper.downloadWebsiteForUrl(url);
            if(doc==null){
                continue;
            }
            downloadedDocsToProcessCopy.add(doc);
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                LOG.warn("Waiting interrupted due to ", e);
            }
        }
        currentlyCrawledHostsCopy.remove(host);
    }
}
