package de.mse.team5.crawler;

import de.mse.team5.hibernate.HibernateUtil;
import de.mse.team5.hibernate.helper.CrawlerNicenessHelper;
import de.mse.team5.hibernate.helper.HttpRequestHelper;
import de.mse.team5.hibernate.helper.WebsiteModelUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.annotations.BatchSize;
import org.jsoup.nodes.Document;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import de.mse.team5.hibernate.model.Link;

public class Crawler {

    public static final Collection<String> entryUrls = Arrays.asList("https://yannick-huber.de");
    public static final Collection<String> allowedUrls = Arrays.asList("https://yannick-huber.de");
    //If enabled the crawler won't touch any other sites that it discovers and will only crawl the website mentioned in the entryUrls
    public static final Boolean siteRestrictedTestMode = Boolean.TRUE;
    private static final int CRAWL_BATCH_SIZE = 200; //only calculating a fixed amount of crawl times to prevent errors due to congestion

    protected WebsiteModelUtils websiteUtils;

    public static final Logger LOG = LogManager.getLogger(Crawler.class);

    private final Set<String> crawledLinks = new HashSet<>();
    private final Set<String> linksToCrawlSet = new HashSet<>();
    private final Queue<String> linksToCrawl = new ArrayDeque<>();

    List<Pair<String, Instant>> linksWithEarliestCrawlTime = new ArrayList<>();

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

        while (!linksToCrawl.isEmpty() || !linksWithEarliestCrawlTime.isEmpty()) {
            fillUpCrawlAbleSitesWithCrawlTimes();
            Queue<String> linksReadyForCrawling = getReadyToCrawlLinksRespectingCrawlDelay();
            while(!linksReadyForCrawling.isEmpty()) {
                String url = linksReadyForCrawling.poll();
                crawlAndIndexWebsite(url);

            }
            sleepUntilNextCrawlTime();
        }

        dbSession.close();
        LOG.info("Finished crawling");
    }

    private void crawlAndIndexWebsite(String url) {
        Document doc = HttpRequestHelper.downloadWebsiteForUrl(url);
        if (doc != null) {
            //download and save website to db
            String websiteContent = doc.body().text();
            String websiteTitle = doc.title();
            Collection<Link> outgoingLinks = websiteUtils.getOutgoingLinksForDoc(doc, url);
            websiteUtils.saveWebsite(url, websiteContent, outgoingLinks, websiteTitle);

            //add outgoing links to crawl-list if they fit our selection criteria
            for (Link outgoingLink : outgoingLinks) {
                if (linkShouldBeCrawled(outgoingLink, crawledLinks, linksToCrawlSet)) {
                    linksToCrawl.add(outgoingLink.getUrl());
                    linksToCrawlSet.add(outgoingLink.getUrl()); //for faster checking if the list contains the element
                }
            }
        }
    }

    /**
     * Lets the thread sleep until the next available crawl time (respecting the crawl delay)
     */
    private void sleepUntilNextCrawlTime() {
        //Sleep if only waiting for crawl delay
        if (linksToCrawl.isEmpty() && !linksWithEarliestCrawlTime.isEmpty()){
            Instant nextCrawlTime = getNearestCrawlDelayTime();
            Duration waitTime = Duration.between(Instant.now(), nextCrawlTime);
            LOG.debug("Waiting for crawl delay - wait time: " + waitTime.toString());
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                LOG.warn("Waiting interrupted due to ", e);
            }
        }
    }

    /**
     * @return all Link urls from linksWithNextCrawlTime who's timestamp of their crawl delay has passed by
     */
    private Queue<String> getReadyToCrawlLinksRespectingCrawlDelay() {
        Instant compareTime = Instant.now();
        List<Pair<String, Instant>> linksReadyForCrawlingWithTime = this.linksWithEarliestCrawlTime.stream()
                .filter(pair -> pair.getRight().isBefore(compareTime))
                .toList();
        //ToDo: move side effect?
        linksWithEarliestCrawlTime.removeAll(linksReadyForCrawlingWithTime);

        Queue<String>  linksReadyForCrawlingAsQueue = new ArrayDeque<>();
        for(Pair<String, Instant> linkTimePair: linksReadyForCrawlingWithTime) {
            linksReadyForCrawlingAsQueue.add(linkTimePair.getLeft());
        }

        return linksReadyForCrawlingAsQueue;
    }

    /**
     * @return Time of the link with the shortest crawl delay
     */
    private Instant getNearestCrawlDelayTime() {
        Optional<Instant> nearestCrawlTime = this.linksWithEarliestCrawlTime.stream()
                .sorted(Comparator.comparing(Pair::getRight))
                .map(Pair::getRight)
                .findFirst();
        return nearestCrawlTime.orElseGet(Instant::now);
    }

    private void fillUpCrawlAbleSitesWithCrawlTimes() {
        int numberOfAddedUrls = 0;
        while(!linksToCrawl.isEmpty() && numberOfAddedUrls < CRAWL_BATCH_SIZE) {
            String url = linksToCrawl.poll();
            linksToCrawlSet.remove(url);
            crawledLinks.add(url);

            Instant nextCrawlTimeForUrl = CrawlerNicenessHelper.getCrawlerNicenessHelper().getNextCrawlTime(url);
            Pair<String, Instant> linkWithCrawlTime = new ImmutablePair<>(url, nextCrawlTimeForUrl);
            this.linksWithEarliestCrawlTime.add(linkWithCrawlTime);

            numberOfAddedUrls+=1;
        }
    }

    private boolean linkShouldBeCrawled(Link outgoingLink, Set<String> crawledLinks, Set<String> linksToCrawlSet) {
        String urlToCheck = outgoingLink.getUrl();
        return !crawledLinks.contains(urlToCheck) && !linksToCrawlSet.contains(urlToCheck) && fitsCrawlFilter(outgoingLink) && CrawlerNicenessHelper.getCrawlerNicenessHelper().isUrlAllowedByRobotsTxt(urlToCheck);

    }

    private boolean fitsCrawlFilter(Link outgoingLink) {
        boolean fitsFilter = true;

        //test filter: check if site is in allowed List
        if(siteRestrictedTestMode) {
            boolean isInAllowedUrls = false;
            for (String allowedUrl : allowedUrls) {
                if (StringUtils.startsWith(outgoingLink.getUrl(), allowedUrl)) {
                    isInAllowedUrls = true;
                    break;
                }
            }
            fitsFilter = isInAllowedUrls;
        }
        return fitsFilter;
    }
}