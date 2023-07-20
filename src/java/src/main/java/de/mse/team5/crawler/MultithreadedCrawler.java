package de.mse.team5.crawler;

import de.mse.team5.crawler.backgroundtasks.DownloadedWebsiteToRamRunner;
import de.mse.team5.crawler.backgroundtasks.SaveDownloadedWebsiteToDbRunnable;
import de.mse.team5.crawler.dto.DownloadedDocDTO;
import de.mse.team5.hibernate.HibernateUtil;
import de.mse.team5.hibernate.helper.WebsiteModelUtils;
import de.mse.team5.hibernate.model.Website;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.StatelessSession;
import org.hibernate.query.MutationQuery;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 * Webcrawler, which follows all links starting at all links provided in entryUrls
 * Using multiple parallel thread for fetching and processing fetched websites
 */
public class MultithreadedCrawler {
    private static final int MAX_CRAWL_SCHEDULE_BATCH_SIZE = 250;
    private static final int MIN_AMOUNT_OF_WEBSITES_TO_CRAWL_IN_PARALLEL = 20;
    private static final int MAX_AMOUNT_OF_WEBSITES_TO_CRAWL_IN_PARALLEL = 65;
    private static final int FETCH_THREAD_POOL_SIZE = 10;
    private static final int PROCESSING_THREAD_POOL_SIZE = 4;
    private static final String TRUSTSTORE_NAME = "mozilla_trustStore.jks";

    public static final Logger LOG = LogManager.getLogger(MultithreadedCrawler.class);

    //Websites that the crawler should start at
    private static final Collection<String> entryUrls = Arrays.asList("https://uni-tuebingen.de/en/",
            "https://www.reddit.com/r/Tuebingen/",
            "https://www.mygermanyvacation.com/best-things-to-do-and-see-in-tubingen-germany/",
            "https://www.viamichelin.com/web/Tourist-Attractions/Tourist-Attractions-Tubingen-72070-Baden_Wurttemberg-Germany",
            "https://www.krone-tuebingen.de/en/a-holiday-in-tuebingen/tuebingen-the-surrounding-area/",
            "https://www.viamichelin.com/web/Tourist-Attractions/Tourist-Attractions-Tubingen-72070-Baden_Wurttemberg-Germany",
            "https://en.wikipedia.org/wiki/Category:Tourist_attractions_in_Tübingen");

    private final Set<String> currentlyCrawledHosts = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<Website>> hostSpecificLinksToCrawlSet = new ConcurrentHashMap<>();
    private final Set<DownloadedDocDTO> downloadedWebsitesToProcess = ConcurrentHashMap.newKeySet();

    private final ThreadPoolExecutor websiteProcessingService = (ThreadPoolExecutor) Executors.newFixedThreadPool(PROCESSING_THREAD_POOL_SIZE);
    private final ThreadPoolExecutor websiteFetchingService = (ThreadPoolExecutor) Executors.newFixedThreadPool(FETCH_THREAD_POOL_SIZE);

    private MultithreadedCrawler() {
        //prevent outside initialization
    }

    /**
     * Main method starting crawler
     * @param args can be empty
     */
    public static void main(String[] args) {
        try {
            MultithreadedCrawler crawler = new MultithreadedCrawler();
            crawler.crawl();
        }
        finally {
            HibernateUtil.getSingletonInstance().shutdown();
        }
    }

    public void crawl(){
        LOG.info("Started Crawling");
        updateSSLCertsToMozillaCerts();
        //insert start List
        initializeCrawlerWithEntryUrls();
        cleanUpInterruptedCrawling();
        LOG.info("Finished initializing of crawler");

        int updateCounter = 0;
        while (crawlingNotFinishedOrStopped()) {
            updateCounter ++;
            runCrawlCycle();
            //Only check in time intervals
            sleepOneSec();

            //LOG status
            if(updateCounter >= 60) {
                logCurrentCrawlStatus();
                updateCounter=0;
            }
        }
        LOG.info("Finished crawling");
    }

    /**
     * Running one crawl cycle completing the following steps in order
     * 1. if there aren't enough crawling threads:
     *      1.1. queue more websites for downloading
     *      1.2. queue more websites for websites that are currently not crawled if there aren't enough websites crawled in parallel
     */
    private void runCrawlCycle() {
        if(crawlerNotToBusy()) {
            if (hostSpecificLinksToCrawlSet.isEmpty()) {
                scheduleMoreWebsitesForCrawling();
            }

            //We want to crawl multiple hosts at once, due to each host having an individual crawl delay
            if (hostSpecificLinksToCrawlSet.keySet().size() < MIN_AMOUNT_OF_WEBSITES_TO_CRAWL_IN_PARALLEL) {
                scheduleMoreWebsitesFromOtherHostsForCrawling();
            }

            if (emptySlotInCrawlerPool() && moreWebsitesAvailableToCrawl()) {
                fetchMoreWebsites();
            }
        }
        if (unprocessedDownloadedWebsitesAvailable()) {
            addWebsiteToAsynchronousProcessing();
        }
    }

    private void sleepOneSec() {
        Duration waitTime = Duration.of(1, ChronoUnit.SECONDS);
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            LOG.warn("Unexpected Interrupt: ", e);
            Thread.currentThread().interrupt();
        }
    }

    private boolean crawlerNotToBusy() {
        return downloadedWebsitesToProcess.size() < 100 && currentlyCrawledHosts.size() < MAX_AMOUNT_OF_WEBSITES_TO_CRAWL_IN_PARALLEL;
    }

    private void logCurrentCrawlStatus() {
        if(LOG.isInfoEnabled()) {
            LOG.info(MessageFormat.format("Currently crawled hosts ({0})={1}", currentlyCrawledHosts.size(), currentlyCrawledHosts));
            LOG.info(MessageFormat.format("Currently crawled number of sites to crawl={0}", hostSpecificLinksToCrawlSet.values().size()));
            LOG.info(MessageFormat.format("downloadedWebsitesToProcess Count ={0}", downloadedWebsitesToProcess.size()));
            LOG.info(MessageFormat.format("Staged process task {0}", websiteProcessingService.getQueue().size()));
        }
    }

    /**
     * The JVM doesn't know a lot of important root certificates by default - we therefor use the current trusted certificate List from Mozialla, that is for example included in Firefox
     * See List of installed certs at <a href="https://hg.mozilla.org/mozilla-central/raw-file/tip/security/nss/lib/ckfw/builtins/certdata.txt">...</a>
     */
    private void updateSSLCertsToMozillaCerts() {
        String truststorePath = System.getProperty("user.dir") + '/' + TRUSTSTORE_NAME;

        if(LOG.isInfoEnabled()) {
            LOG.info(MessageFormat.format("Setting truststore to {0}", truststorePath));
        }

        System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_NAME);
        System.setProperty("javax.net.ssl.trustStorePassword=", "changeit");
        System.setProperty("javax.net.ssl.trustStoreType", "jks");
    }


    /**
     * The db contains information, about which websites were queued for downloading and processing.
     * If the crawling process gets interrupted, the sites are still marked as in process
     * This methode removes this mark from all website entries in the db
     */
    private void cleanUpInterruptedCrawling() {
        try (StatelessSession dbSession = HibernateUtil.openStatelessSession()) {
            MutationQuery query = dbSession.createMutationQuery("UPDATE Website SET stagedForCrawling=:stagedForCrawling");
            query.setParameter("stagedForCrawling", Boolean.FALSE);
            dbSession.getTransaction().begin();
            query.executeUpdate();
            dbSession.getTransaction().commit();
        }
    }

    private void scheduleMoreWebsitesForCrawlingExcludingHosts(Set<String> hostsToExclude) {
        Collection<Website> websites;
        try (StatelessSession dbSession = HibernateUtil.openStatelessSession()) {
            WebsiteModelUtils websiteUtils = new WebsiteModelUtils(dbSession);
            websites = websiteUtils.getUncrawledWebsites(MAX_CRAWL_SCHEDULE_BATCH_SIZE, hostsToExclude);
            //re-crawl old data if there are no new links
            if(websites.size() < MAX_CRAWL_SCHEDULE_BATCH_SIZE){
                int diff= MAX_CRAWL_SCHEDULE_BATCH_SIZE-websites.size();
                Collection<Website> oldWebsites = websiteUtils.getLongestNotCrawledWebsites(diff, hostsToExclude);
                websites.addAll(oldWebsites);
            }
            websites.forEach(site -> {
                site.setStagedForCrawling(true);
                websiteUtils.updateWebsiteInDb(site);
            });
        }
        Map<String, Set<Website>> websitesGroupedByHost = websites.stream().collect(Collectors.groupingBy(Website::getHostUrl, toSet()));
        hostSpecificLinksToCrawlSet.putAll(websitesGroupedByHost);

    }
    private void scheduleMoreWebsitesForCrawling() {
        scheduleMoreWebsitesForCrawlingExcludingHosts(null);
    }


    private void scheduleMoreWebsitesFromOtherHostsForCrawling() {
        scheduleMoreWebsitesForCrawlingExcludingHosts(currentlyCrawledHosts);
    }


    private void fetchMoreWebsites() {
        Collection<Runnable> tasks = new ArrayList<>();
        synchronized (currentlyCrawledHosts) {
            synchronized (hostSpecificLinksToCrawlSet) {
                Set<String> hostsWithAvailableUrls = hostSpecificLinksToCrawlSet.keySet();
                //We should only crawl hosts, that aren't currently crawled by another thread to be able to comply with their crawl delay
                List<String> crawlableHosts = hostsWithAvailableUrls.stream().filter(host -> !currentlyCrawledHosts.contains(host)).toList();

                for (String host : crawlableHosts) {
                    Runnable task = getCrawTaskForHost(host);
                    tasks.add(task);
                }
            }
        }
        for (Runnable task : tasks) {
            websiteFetchingService.execute(task);
        }
    }

    private Runnable getCrawTaskForHost(String host) {
        Set<Website> sitesToDownload = hostSpecificLinksToCrawlSet.get(host);
        if (LOG.isDebugEnabled()) {
            LOG.debug(MessageFormat.format("Starting new batch fetch of size {0} for host: {1}", sitesToDownload.size(), host));
        }
        hostSpecificLinksToCrawlSet.remove(host);
        Runnable task = new DownloadedWebsiteToRamRunner(sitesToDownload, currentlyCrawledHosts, host, downloadedWebsitesToProcess);
        currentlyCrawledHosts.add(host);
        return task;
    }

    private boolean unprocessedDownloadedWebsitesAvailable() {
        return !downloadedWebsitesToProcess.isEmpty();
    }

    /**
     * Creates db entries for all entry url, which are then picked up by the crawler
     */
    private void initializeCrawlerWithEntryUrls() {
        try (StatelessSession dbSession = HibernateUtil.openStatelessSession()) {
            WebsiteModelUtils websiteUtils = new WebsiteModelUtils(dbSession);
            for (String entryUrl : entryUrls) {
                websiteUtils.getOrCreateWebsite(entryUrl, StringUtils.EMPTY);
            }
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
        //only run if there are multiple websites to queue to prevent overhead
        if(downloadedWebsitesToProcess.size() < 25){
            return;
        }
        Runnable task;
        synchronized (downloadedWebsitesToProcess) {
            //only work on part of the elements if list is too big
            List<DownloadedDocDTO> downloadedWebsitesToProcessCopy = new ArrayList<>(downloadedWebsitesToProcess);
            if(downloadedWebsitesToProcess.size() > 150) {
                downloadedWebsitesToProcessCopy = downloadedWebsitesToProcessCopy.subList(0,149);
            }
            if(LOG.isDebugEnabled()) {
                String websitesInFetch = Arrays.toString(downloadedWebsitesToProcessCopy.stream().map(DownloadedDocDTO::getSite).map(Website::getUrl).toArray());
                LOG.debug(MessageFormat.format("Starting new batch processing for urls: {0}", websitesInFetch));
            }
            task = new SaveDownloadedWebsiteToDbRunnable(downloadedWebsitesToProcessCopy);
            downloadedWebsitesToProcessCopy.forEach(downloadedWebsitesToProcess::remove);
        }
        websiteProcessingService.execute(task);
    }
}