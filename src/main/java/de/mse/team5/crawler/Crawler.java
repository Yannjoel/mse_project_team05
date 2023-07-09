package de.mse.team5.crawler;

import de.mse.team5.hibernate.HibernateUtil;
import de.mse.team5.hibernate.helper.CrawlerNicenessHelper;
import de.mse.team5.hibernate.helper.HttpRequestHelper;
import de.mse.team5.hibernate.helper.WebsiteModelUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.jsoup.nodes.Document;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import de.mse.team5.hibernate.model.Link;

import static de.mse.team5.crawler.CrawlerFilters.fitsCrawlFilter;
import static de.mse.team5.hibernate.helper.CrawlerNicenessHelper.getCrawlerNicenessHelper;

public class Crawler {

    public static final Collection<Link> entryUrls = Arrays.asList(Link.createNewLink("https://yannick-huber.de/"), Link.createNewLink("https://www.tuebingen.de/"));

    protected WebsiteModelUtils websiteUtils;

    public static final Logger LOG = LogManager.getLogger(Crawler.class);

    private final Set<String> crawledLinks = new HashSet<>();
    private final Set<String> linksToCrawlSet = new HashSet<>();
    private final Queue<Link> linksToCrawl = new ArrayDeque<>();

    private final Map<String, Instant> hostToLastCrawltimeMap = new HashMap<>();

    public static void main(String[] args) {
        Crawler crawler = new Crawler();
        crawler.crawl();
    }

    public void crawl() {
        LOG.info("Started crawling");
        Session dbSession = HibernateUtil.getSessionFactory().openSession();
        websiteUtils = new WebsiteModelUtils(dbSession);

        //insert start List
        linksToCrawl.addAll(entryUrls);
        linksToCrawlSet.addAll(entryUrls.stream().map(Link::getUrl).toList());

        while (!linksToCrawl.isEmpty() && crawledLinks.size() < 100) {
            Link link = linksToCrawl.poll();
            linksToCrawlSet.remove(link.getUrl());

            Instant crawlTime = getCrawlTimeForLink(link);
            sleepUntilCrawlTime(crawlTime);
            crawlAndIndexWebsite(link);
        }

        dbSession.close();
        LOG.info("Finished crawling");
    }

    private void sleepUntilCrawlTime(Instant nextCrawlTime) {
        Instant now = Instant.now();
        if (!now.isBefore(nextCrawlTime)) {
            return;
        }
        //Sleep while waiting for crawl delay
        Duration waitTime = Duration.between(now, nextCrawlTime);
        LOG.debug("Waiting for crawl delay - wait time: " + waitTime.toString());
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            LOG.warn("Waiting interrupted due to ", e);
        }
    }

    private Instant getCrawlTimeForLink(Link link) {
        String host = link.getHostUrl();
        Instant lastCrawlTime = this.hostToLastCrawltimeMap.get(host);
        if (lastCrawlTime == null) {
            return Instant.now();
        }
        int crawlDelay = getCrawlerNicenessHelper().getCrawlDelay(link);
        return lastCrawlTime.plusSeconds(crawlDelay);
    }

    private void crawlAndIndexWebsite(Link link) {
        LOG.info("crawling " + link.getUrl());

        String url = link.getUrl();
        //download website
        Document doc = HttpRequestHelper.downloadWebsiteForUrl(url);
        this.hostToLastCrawltimeMap.put(link.getHostUrl(), Instant.now());
        crawledLinks.add(url);

        if (doc != null) {
            //save website to db
            String websiteContent = doc.body().text();
            String websiteTitle = doc.title();
            Collection<Link> outgoingLinks = websiteUtils.getOutgoingLinksForDoc(doc, url);
            websiteUtils.saveWebsite(url, websiteContent, outgoingLinks, websiteTitle);

            //add outgoing links to crawl-list if they fit our selection criteria
            for (Link outgoingLink : outgoingLinks) {
                if (linkShouldBeCrawled(outgoingLink)) {
                    linksToCrawl.add(outgoingLink);
                    linksToCrawlSet.add(outgoingLink.getUrl()); //for faster checking if the list contains the element
                }
            }
        }
    }

    private boolean linkShouldBeCrawled(Link outgoingLink) {
        String urlToCheck = outgoingLink.getUrl();
        if (crawledLinks.contains(urlToCheck))
            return false;
        if (linksToCrawlSet.contains(urlToCheck))
            return false;
        if (!fitsCrawlFilter(outgoingLink))
            return false;
        if(!getCrawlerNicenessHelper().isUrlAllowedByRobotsTxt(outgoingLink))
            return false;
        if(StringUtils.length(urlToCheck) < 2048){
            LOG.info("Skipping too long url: " + urlToCheck);
            return false;
        }
        return true;
    }
}