package de.mse.team5.crawler;

import de.mse.team5.hibernate.HibernateUtil;
import de.mse.team5.hibernate.helper.CrawlerNicenessHelper;
import de.mse.team5.hibernate.helper.HttpRequestHelper;
import de.mse.team5.hibernate.helper.WebsiteModelUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.jsoup.nodes.Document;

import java.time.Instant;
import java.util.*;

import de.mse.team5.hibernate.model.Link;

public class Crawler{

    public static final Collection<String> entryUrls = Arrays.asList("https://yannick-huber.de");
    public static final Collection<String> allowedUrls = Arrays.asList("https://yannick-huber.de");
    //If enabled the crawler won't touch any other sites that it discovers and will only crawl the website mentioned in the entryUrls
    public static final Boolean siteRestrictedTestMode = Boolean.TRUE;

    protected WebsiteModelUtils websiteUtils;

    public static final Logger LOG = LogManager.getLogger(Crawler.class);
    private Session dbSession;

    private Set<String> crawledLinks = new HashSet<>();
    private Set<String> linksToCrawlSet = new HashSet<>();
    private Queue<String> linksToCrawl = new ArrayDeque<>();

    public static void main(String[] args){
        Crawler crawler = new Crawler();
        crawler.crawl();
    }

    public void crawl(){
        LOG.info("Started crawling");
        //TODO: move into application startup function
        //HibernateUtil.startDatabase();
        this.dbSession = HibernateUtil.getSessionFactory().openSession();
        websiteUtils = new WebsiteModelUtils(dbSession);

        //insert start List
        linksToCrawl.addAll(entryUrls);

        while(!linksToCrawl.isEmpty()) {
            // TODO: add niceness + check for robots
            //Pair<String, Instant> crawlableSites = fillUpCrawlableSitesWithCrawlTimes();
            String url = linksToCrawl.poll();
            linksToCrawlSet.remove(url);
            crawledLinks.add(url);
            Document doc = HttpRequestHelper.downloadWebsiteForUrl(url);
            if (doc != null) {
                //download and save website to db
                String websiteContent = doc.body().text();
                String websiteTitle = doc.title();
                Collection<Link> outgoingLinks = websiteUtils.getOutgoingLinksForDoc(doc);
                websiteUtils.saveWebsite(url, websiteContent, outgoingLinks, websiteTitle);

                //add outgoing links to crawl-list if they fit our selection criteria
                for (Link outgoingLink: outgoingLinks){
                    if(linkShouldBeCrawled(outgoingLink, crawledLinks, linksToCrawlSet)){
                        linksToCrawl.add(outgoingLink.getUrl());
                        linksToCrawlSet.add(outgoingLink.getUrl()); //for faster checking if the list contains the element
                    }
                }
            }
        }

        dbSession.close();
        LOG.info("Finished crawling");
    }

    private Pair<String, Instant> fillUpCrawlableSitesWithCrawlTimes() {
        return null;
    }

    private boolean linkShouldBeCrawled(Link outgoingLink, Set<String> crawledLinks, Set<String> linksToCrawlSet) {
        String urlToCheck = outgoingLink.getUrl();
        return !crawledLinks.contains(urlToCheck)
                && !linksToCrawlSet.contains(urlToCheck)
                && fitsCrawlFilter(outgoingLink)
                && CrawlerNicenessHelper.getCrawlerNicenessHelper().isUrlAllowedByRobotsTxt(urlToCheck);

    }

    private boolean fitsCrawlFilter(Link outgoingLink) {
        //test filter: check if site is in allowed List
        for (String allowedUrl: allowedUrls){
            if(StringUtils.startsWith(outgoingLink.getUrl(), allowedUrl)){
                return true;
            }
        }
        return false;
    }
}