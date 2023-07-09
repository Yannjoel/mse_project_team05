package de.mse.team5.crawler.backgroundtasks;

import de.mse.team5.hibernate.HibernateUtil;
import de.mse.team5.hibernate.helper.WebsiteModelUtils;
import de.mse.team5.hibernate.model.Link;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static de.mse.team5.crawler.CrawlerFilters.fitsCrawlFilter;
import static de.mse.team5.hibernate.helper.CrawlerNicenessHelper.getCrawlerNicenessHelper;

public class SaveDownloadedWebsiteToDbRunnable implements Runnable {

    private static final Logger LOG = LogManager.getLogger(SaveDownloadedWebsiteToDbRunnable.class);
    private final Collection<Document> downloadedWebsitesToProcess;
    private final Set<String> crawledLinksCopy;
    private final Map<String, Set<String>> hostSpecificLinksToCrawlSetCopy;

    public SaveDownloadedWebsiteToDbRunnable(Collection<Document> downloadedWebsitesToProcess, Map<String, Set<String>> hostSpecificLinksToCrawlSet, Set<String> crawledLinks) {
        this.downloadedWebsitesToProcess = downloadedWebsitesToProcess;
        this.hostSpecificLinksToCrawlSetCopy = hostSpecificLinksToCrawlSet;
        this.crawledLinksCopy = crawledLinks;
    }

    @Override
    public void run() {
        Session dbSession = HibernateUtil.getSessionFactory().openSession();
        WebsiteModelUtils websiteUtils = new WebsiteModelUtils(dbSession);

        for (Document doc : downloadedWebsitesToProcess) {
            if (doc == null) {
                continue;
            }
            //download and save website to db
            String url = doc.location();
            LOG.debug("processing " + url);
            String websiteContent = doc.body().text();
            String websiteTitle = doc.title();
            Collection<Link> outgoingLinks = websiteUtils.getOutgoingLinksForDoc(doc, url);
            websiteUtils.saveWebsite(url, websiteContent, outgoingLinks, websiteTitle);

            //add outgoing links to crawl-list if they fit our selection criteria
            for (Link outgoingLink : outgoingLinks) {
                if (linkShouldBeCrawled(outgoingLink)) {
                    if (!hostSpecificLinksToCrawlSetCopy.containsKey(outgoingLink.getHostUrl())) {
                        hostSpecificLinksToCrawlSetCopy.put(outgoingLink.getHostUrl(), ConcurrentHashMap.newKeySet());
                    } else {
                        hostSpecificLinksToCrawlSetCopy.get(outgoingLink.getHostUrl()).add(outgoingLink.getUrl());
                    }
                }
            }
        }
    }

    private boolean linkShouldBeCrawled(Link outgoingLink) {
        String urlToCheck = outgoingLink.getUrl();
        if (crawledLinksCopy.contains(urlToCheck))
            return false;
        if (!fitsCrawlFilter(outgoingLink))
            return false;
        if (StringUtils.length(urlToCheck) > 2048) {
            LOG.info("Skipping too long url: " + urlToCheck);
            return false;
        }
        Set<String> hostCrawlList = hostSpecificLinksToCrawlSetCopy.get(outgoingLink.getHostUrl());
        if (hostCrawlList != null && hostCrawlList.contains(urlToCheck))
            return false;
        if (!getCrawlerNicenessHelper().isUrlAllowedByRobotsTxt(outgoingLink)) {
            return false;
        }
        return true;
    }
}
