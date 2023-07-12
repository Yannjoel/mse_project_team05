package de.mse.team5.crawler;

import de.mse.team5.crawler.backgroundtasks.DownloadedWebsiteToRamRunner;
import de.mse.team5.crawler.backgroundtasks.SaveDownloadedWebsiteToDbRunnable;
import de.mse.team5.crawler.dto.DownloadedDocDTO;
import de.mse.team5.hibernate.HibernateUtil;
import de.mse.team5.hibernate.helper.WebsiteModelUtils;
import de.mse.team5.hibernate.model.Website;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.query.Query;
import org.jsoup.nodes.Document;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

public class MultithreadedCrawler {
    private static final int MAX_CRAWL_SCHEDULE_BATCH_SIZE = 100;
    private StatelessSession dbSession;

    private MultithreadedCrawler() {
        //prevent outside initialization
    }

    public static final Logger LOG = LogManager.getLogger(MultithreadedCrawler.class);
    public static final Collection<String> entryUrls = Arrays.asList("https://yannick-huber.de/","https://www.tuebingen.de/519.html");
    private final Set<String> currentlyCrawledHosts = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<Website>> hostSpecificLinksToCrawlSet = new ConcurrentHashMap<>();
    private final Set<DownloadedDocDTO> downloadedWebsitesToProcess = ConcurrentHashMap.newKeySet();

    ThreadPoolExecutor websiteProcessingService = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
    ThreadPoolExecutor websiteFetchingService = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws InterruptedException {
        MultithreadedCrawler crawler = new MultithreadedCrawler();
        crawler.crawl();
    }

    public void crawl() throws InterruptedException {
        LOG.info("Started Crawling");

        dbSession = HibernateUtil.getSessionFactory().openStatelessSession();
        //insert start List
        initializeCrawlerWithEntryUrls();
        cleanUpInteruptedCrawling();
        LOG.info("Finished initializing of crawler");

        Duration waitTime = Duration.of(1, ChronoUnit.SECONDS);

        while (crawlingNotFinishedOrStopped()) {
            if(hostSpecificLinksToCrawlSet.isEmpty()){
                scheduleMoreWebsitesForCrawling();
            }

            if (emptySlotInCrawlerPool() && moreWebsitesAvailableToCrawl()) {
                fetchMoreWebsites();
            }
            if (unprocessedDownloadedWebsitesAvailable()) {
                addWebsiteToAsynchronousProcessing();
            }

            //Only check in time intervals
            Thread.sleep(waitTime);
        }

        dbSession.close();
        LOG.info("Finished crawling");
    }

    private void cleanUpInteruptedCrawling() {
        Query query = dbSession.createQuery("UPDATE Website SET stagedForCrawling=:stagedForCrawling");
        query.setParameter("stagedForCrawling", Boolean.FALSE);
        dbSession.getTransaction().begin();
        query.executeUpdate();
        dbSession.getTransaction().commit();
    }

    private void scheduleMoreWebsitesForCrawling() {
        WebsiteModelUtils websiteUtils = new WebsiteModelUtils(dbSession);
        Collection<Website> websites = websiteUtils.getUncrawledWebsites(MAX_CRAWL_SCHEDULE_BATCH_SIZE);
        //re-crawl old data if there are no new links
        if(websites.size() < MAX_CRAWL_SCHEDULE_BATCH_SIZE){
            int diff= MAX_CRAWL_SCHEDULE_BATCH_SIZE-websites.size();
            Collection<Website> oldWebsites = websiteUtils.getLongestNotCrawledWebsites(MAX_CRAWL_SCHEDULE_BATCH_SIZE);
            websites.addAll(oldWebsites);
        }
        websites.stream().forEach(site -> {
            site.setStagedForCrawling(true);
            websiteUtils.updateWebsiteInDb(site);
        });

        Map<String, Set<Website>> websitesGroupedByHost = websites.stream().collect(Collectors.groupingBy(Website::getHostUrl, toSet()));
        hostSpecificLinksToCrawlSet.putAll(websitesGroupedByHost);
    }

    private void fetchMoreWebsites() {
        Collection<Runnable> tasks = new ArrayList<>();
        synchronized (currentlyCrawledHosts) {
            synchronized (hostSpecificLinksToCrawlSet) {
                Set<String> hostsWithAvailableUrls = hostSpecificLinksToCrawlSet.keySet();
                List<String> crawlableHosts = hostsWithAvailableUrls.stream().filter(host -> !currentlyCrawledHosts.contains(host)).toList();

                for (String host : crawlableHosts) {
                    Set<Website> sitesToDownload = hostSpecificLinksToCrawlSet.get(host);
                    LOG.info("Starting new batch fetch of size " + sitesToDownload.size() + " for host: " + host);
                    hostSpecificLinksToCrawlSet.remove(host);
                    Runnable task = new DownloadedWebsiteToRamRunner(sitesToDownload, currentlyCrawledHosts, host, downloadedWebsitesToProcess);
                    tasks.add(task);
                    currentlyCrawledHosts.add(host);
                }
            }
        }
        for (Runnable task : tasks) {
            websiteFetchingService.execute(task);
        }
    }

    private boolean unprocessedDownloadedWebsitesAvailable() {
        return !downloadedWebsitesToProcess.isEmpty();
    }

    private void initializeCrawlerWithEntryUrls() {
        WebsiteModelUtils websiteUtils = new WebsiteModelUtils(dbSession);
        for (String entryUrl : entryUrls) {
            websiteUtils.getOrCreateWebsite(entryUrl, StringUtils.EMPTY);
        }
    }

    private boolean moreWebsitesAvailableToCrawl() {
        if (hostSpecificLinksToCrawlSet.isEmpty())
            return false;
        return !currentlyCrawledHosts.containsAll(hostSpecificLinksToCrawlSet.keySet());
    }

    private boolean emptySlotInCrawlerPool() {
        return websiteFetchingService.getActiveCount() < websiteFetchingService.getMaximumPoolSize();
    }

    private boolean crawlingNotFinishedOrStopped() {
        if (!currentlyCrawledHosts.isEmpty())
            return true;
        if (!hostSpecificLinksToCrawlSet.isEmpty())
            return true;
        return !websiteProcessingService.isTerminated();
    }

    /**
     * Converts all downloaded but not yet processed Websites to a Website Object and saves it to the db
     */
    private void addWebsiteToAsynchronousProcessing() {
        Runnable task = null;
        synchronized (downloadedWebsitesToProcess) {
            //only work on part of the elements if list is too big
            List<DownloadedDocDTO> downloadedWebsitesToProcessCopy = new ArrayList<>(downloadedWebsitesToProcess);
            if(downloadedWebsitesToProcess.size() > 50) {
                downloadedWebsitesToProcessCopy = downloadedWebsitesToProcessCopy.subList(0,49);
            }
            LOG.info("Starting new batch processing for urls: " + Arrays.toString(downloadedWebsitesToProcessCopy.stream().map(DownloadedDocDTO::getDownloadedData).map(Document::location).toArray()));
            task = new SaveDownloadedWebsiteToDbRunnable(downloadedWebsitesToProcessCopy);
            downloadedWebsitesToProcess.clear();

            downloadedWebsitesToProcess.removeAll(downloadedWebsitesToProcessCopy);
        }
        websiteProcessingService.execute(task);
    }
}