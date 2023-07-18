package de.mse.team5.hibernate.helper;

import de.mse.team5.hibernate.model.Website;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.StatelessSession;
import org.hibernate.query.Query;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static de.mse.team5.crawler.CrawlerFilters.fitsCrawlFilter;

public class WebsiteModelUtils {

    private static final Logger LOG = LogManager.getLogger(WebsiteModelUtils.class);

    private final StatelessSession dbSession;

    public WebsiteModelUtils(StatelessSession dbSession) {
        this.dbSession = dbSession;
    }

    public Collection<Website> getOutgoingLinksForDoc(Document doc, String currentFullUrl) {
        Set<Website> outgoingLinks = new HashSet<>();
        Elements linkHtmlElements = doc.select("a");
        linkHtmlElements.forEach(link -> {
            if (!linkMatchesFilter(link)) {
                return;
            }
            String linkUrl = formatLinkUrl(link.attr("abs:href"), currentFullUrl);
            if (linkShouldBeCrawled(linkUrl)) {
                Website outgoingLink = getOrCreateWebsite(link.attr("abs:href"), currentFullUrl);
                if (outgoingLink != null) {
                    outgoingLinks.add(outgoingLink);
                }
            }
        });
        return outgoingLinks;
    }

    public Website getOrCreateWebsite(String href, String currentFullUrl) {
        String linkUrl = formatLinkUrl(href, currentFullUrl);
        Website website = null;

        if (linkUrl != null) {
            website = getWebsiteForUrl(linkUrl);
            if (website == null) {
                website = new Website();
                website.setUrl(linkUrl);
                insertWebsiteIntoDb(website);
            }
        }
        return website;
    }

    /**
     * Ensures that all links are equally formatted
     *
     * @param linkUrl no yet formatted http(s) link
     * @return formatted link
     */
    private static String formatLinkUrl(String linkUrl, String currentFullUrl) {
        if (StringUtils.isEmpty(linkUrl)) {
            return null;
        }
        String formattedURL = null;
        try {
            //append current url if link is relative or anchor link
            if (StringUtils.startsWith(linkUrl, "/") || StringUtils.startsWith(linkUrl, "#")) {
                URL currentUrl = new URL(currentFullUrl);
                String currentPathPrefix = currentUrl.getProtocol() + "://" + currentUrl.getHost();
                linkUrl = currentPathPrefix + linkUrl;
            }
            URL url = new URL(linkUrl);
            formattedURL = formatLinkUrl(url);
        } catch (MalformedURLException | IllegalArgumentException e) {
            LOG.warn("malformed url " + linkUrl, e);
        }
        return formattedURL;
    }

    private static String formatLinkUrl(URL url) {
        String formattedUrl = url.getProtocol() + "://" + url.getHost() + url.getFile();
        if(StringUtils.isNotEmpty(url.getQuery())){
            formattedUrl += "?" + url.getQuery();
        }
        return formattedUrl;
    }

    /**
     * Check if a link is relevant for crawling
     * For example exclude links to anchors on the same site or mailto links
     *
     * @param link link as found in the href tag
     * @return true if the link should be saved and crawled
     */
    private boolean linkMatchesFilter(Element link) {
        if (!link.hasAttr("href")) {
            return false;
        }
        return StringUtils.startsWith(link.attr("abs:href"), "http") || StringUtils.startsWith(link.attr("abs:href"), "/");
    }


    private boolean linkShouldBeCrawled(String urlToCheck) {
        if (!fitsCrawlFilter(urlToCheck))
            return false;
        if (StringUtils.length(urlToCheck) > 2048) {
            LOG.debug("Skipping too long url: " + urlToCheck);
            return false;
        }
        return true;
    }


    public void updateWebsiteInDb(Website website) {
        try {
            dbSession.getTransaction().begin();
            dbSession.update(website);
            dbSession.getTransaction().commit();
        } catch (HibernateException e) {
            LOG.warn("Failed to save website \"" + website.getUrl() + "\" to db due to ", e);
        }
    }

    public void insertWebsiteIntoDb(Website website) {
        try {
            synchronized (dbSession) {
                dbSession.getTransaction().begin();
                dbSession.insert(website);
                dbSession.getTransaction().commit();
            }
        } catch (HibernateException e) {
            LOG.warn("Failed to save website \"" + website.getUrl() + "\" to db due to ", e);
        }
    }

    private Website getWebsiteForUrl(String url) {
        Website existingWebsite = null;

        CriteriaBuilder cb = dbSession.getCriteriaBuilder();
        CriteriaQuery<Website> cr = cb.createQuery(Website.class);
        Root<Website> root = cr.from(Website.class);
        cr.select(root).where(cb.equal(root.get("url"), url));

        Query<Website> query = dbSession.createQuery(cr);
        List<Website> results = query.getResultList();

        if (!results.isEmpty()) {
            existingWebsite = results.get(0); //only one possible result due to unique key
        }
        return existingWebsite;
    }

    public Collection<Website> getUncrawledWebsites(int maxNrOfResults, Set<String> hostsToExclude) {
        CriteriaBuilder cb = dbSession.getCriteriaBuilder();
        CriteriaQuery<Website> cr = cb.createQuery(Website.class);
        Root<Website> root = cr.from(Website.class);

        Predicate notCrawled = cb.isNull(root.get("lastCrawled"));
        Predicate firstNamePredicate = cb.isFalse(root.get("stagedForCrawling"));

        Predicate combinedPredicate = cb.and(notCrawled, firstNamePredicate);

        if (hostsToExclude != null && !hostsToExclude.isEmpty()) {
            Predicate excludeHosts = cb.not(root.get("hostUrl").in(hostsToExclude));
            combinedPredicate = cb.and(combinedPredicate, excludeHosts);
        }

        cr.select(root).where(combinedPredicate);

        Query<Website> query = dbSession.createQuery(cr);
        query.setMaxResults(maxNrOfResults);

        List<Website> results = query.getResultList();
        LOG.debug("queued " + results.stream().map(Website::getUrl).toList());
        return results;
    }

    public Collection<Website> getLongestNotCrawledWebsites(int maxNrOfResults, Set<String> hostToExclude) {
        CriteriaBuilder cb = dbSession.getCriteriaBuilder();
        CriteriaQuery<Website> cr = cb.createQuery(Website.class);
        Root<Website> root = cr.from(Website.class);

        Predicate selectionPredicates = cb.isFalse(root.get("stagedForCrawling"));

        if (hostToExclude != null && !hostToExclude.isEmpty()) {
            Predicate excludeHosts = root.get("hostUrl").in(hostToExclude).not();
            selectionPredicates = cb.and(selectionPredicates, excludeHosts);
        }
        cr.select(root)
                .where(selectionPredicates)
                .orderBy(cb.asc(root.get("lastCrawled")))
        ;

        Query<Website> query = dbSession.createQuery(cr);
        query.setMaxResults(maxNrOfResults);

        return query.getResultList();
    }
}
