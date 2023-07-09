package de.mse.team5.crawler;

import de.mse.team5.crawler.backgroundtasks.DownloadedWebsiteToRamRunner;
import de.mse.team5.crawler.backgroundtasks.SaveDownloadedWebsiteToDbRunnable;
import de.mse.team5.hibernate.model.Link;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class MultithreadedCrawler {
    public static final Logger LOG = LogManager.getLogger(MultithreadedCrawler.class);
    public static final Collection<Link> entryUrls = Arrays.asList(Link.createNewLink("https://yannick-huber.de/"), Link.createNewLink("https://www.tuebingen.de/"));
    private final Set<String> crawledLinks = ConcurrentHashMap.newKeySet();
    private final Set<String> currentlyCrawledHosts = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<String>> hostSpecificLinksToCrawlSet = new ConcurrentHashMap<>();
    private final Set<Document> downloadedWebsitesToProcess =  ConcurrentHashMap.newKeySet();

    ThreadPoolExecutor websiteProcessingService = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
    ThreadPoolExecutor websiteFetchingService = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws InterruptedException {
        MultithreadedCrawler crawler = new MultithreadedCrawler();
        crawler.crawl();
    }

    public void crawl() throws InterruptedException {
        LOG.info("Started Crawling");
        //insert start List
        initializeCrawlerWithEntryUrls();
        LOG.info("Finished initializing");

        Duration waitTime = Duration.of(1, ChronoUnit.SECONDS);

        while (crawlingNotFinished()) {
            boolean didAnything = false;
            if (emptySlotInCrawlerPool() && moreWebsitesAvailableToCrawl()) {
                fetchMoreWebsites();
            }
            if (unprocessedDownloadedWebsitesAvailable()) {
                addWebsiteToAsynchronousProcessing();
            }

            //Only check in time intervals
            Thread.sleep(waitTime);
        }

        LOG.info("Finished crawling");
    }

    private void printData() {

        System.out.println("currentlyCrawledHosts:" + Arrays.toString(currentlyCrawledHosts.toArray()));

        System.out.println("hostSpecificLinksToCrawlSet");

        for (String keys : hostSpecificLinksToCrawlSet.keySet())
        {
            System.out.println(keys + ":"+ hostSpecificLinksToCrawlSet.get(keys));
        }
    }

    private void fetchMoreWebsites() {
        Collection<Runnable> tasks = new ArrayList<>();
        synchronized (currentlyCrawledHosts) {
            synchronized (hostSpecificLinksToCrawlSet) {
                Set<String> hostsWithAvailableUrls = hostSpecificLinksToCrawlSet.keySet();
                List<String> crawlableHosts = hostsWithAvailableUrls.stream().filter(host -> !currentlyCrawledHosts.contains(host)).toList();

                for(String host: crawlableHosts){
                    Set<String> urlsOnHostToDownload = hostSpecificLinksToCrawlSet.get(host);
                    LOG.info("Starting new batch fetch for urls: " + Arrays.toString(urlsOnHostToDownload.toArray()));
                    crawledLinks.addAll(urlsOnHostToDownload);
                    hostSpecificLinksToCrawlSet.remove(host);
                    Runnable task = new DownloadedWebsiteToRamRunner(urlsOnHostToDownload, currentlyCrawledHosts, host, downloadedWebsitesToProcess);
                    tasks.add(task);
                    currentlyCrawledHosts.add(host);
                }
            }
        }
        for(Runnable task: tasks) {
            websiteFetchingService.execute(task);
        }
    }

    private boolean unprocessedDownloadedWebsitesAvailable() {
        return !downloadedWebsitesToProcess.isEmpty();
    }

    private void initializeCrawlerWithEntryUrls() {
        for (Link entryUrl : entryUrls) {
            if (!hostSpecificLinksToCrawlSet.containsKey(entryUrl.getHostUrl())) {
                hostSpecificLinksToCrawlSet.put(entryUrl.getHostUrl(), ConcurrentHashMap.newKeySet());
            }
            hostSpecificLinksToCrawlSet.get(entryUrl.getHostUrl()).add(entryUrl.getUrl());
        }
    }

    private boolean moreWebsitesAvailableToCrawl() {
        if (hostSpecificLinksToCrawlSet.isEmpty())
            return false;
        if (currentlyCrawledHosts.containsAll(hostSpecificLinksToCrawlSet.keySet())) {
            return false;
        }
        return true;
    }

    private boolean emptySlotInCrawlerPool() {
        return websiteFetchingService.getActiveCount() < websiteFetchingService.getMaximumPoolSize();
    }

    private boolean crawlingNotFinished() {
        if (!currentlyCrawledHosts.isEmpty())
            return true;
        if (!hostSpecificLinksToCrawlSet.isEmpty())
            return true;
        if (!websiteProcessingService.isTerminated())
            return true;
        return false;
    }

    /**
     * Converts all downloaded but not yet processed Websites to a Website Object and saves it to the db
     */
    private void addWebsiteToAsynchronousProcessing() {
        Runnable task;
        synchronized (downloadedWebsitesToProcess) {
            Collection<Document> downloadedWebsitesToProcessCopy = new ArrayList<>(downloadedWebsitesToProcess);
            LOG.info("Starting new batch processing for urls: " + Arrays.toString(downloadedWebsitesToProcessCopy.stream().map(doc -> doc.location()).toArray()));
            task = new SaveDownloadedWebsiteToDbRunnable(downloadedWebsitesToProcessCopy, hostSpecificLinksToCrawlSet, crawledLinks);
            downloadedWebsitesToProcess.clear();
        }
        websiteProcessingService.execute(task);
    }
}