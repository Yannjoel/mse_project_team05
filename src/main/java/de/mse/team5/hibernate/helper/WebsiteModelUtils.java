package de.mse.team5.hibernate.helper;

import de.mse.team5.hibernate.model.Link;
import de.mse.team5.hibernate.model.Website;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class WebsiteModelUtils {

    private static final Logger LOG = LogManager.getLogger(WebsiteModelUtils.class);

    private final Session dbSession;

    public WebsiteModelUtils(Session dbSession) {
        this.dbSession = dbSession;
    }

    public Collection<Link> getOutgoingLinksForDoc(Document doc, String currentFullUrl) {
        Set<Link> outgoingLinks = new HashSet<>();
        Elements linkHtmlElements = doc.select("a");
        linkHtmlElements.forEach(link -> {
            if (linkMatchesFilter(link)) {
                Link outgoingLink = Link.createNewLink(link.attr("href"), currentFullUrl, link.text());
                if (outgoingLink != null) {
                    outgoingLinks.add(outgoingLink);
                }
            }
        });
        //TODO duplicate check?
        return outgoingLinks;
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
        return StringUtils.startsWith(link.attr("href"), "http") || StringUtils.startsWith(link.attr("href"), "/");
    }

    /**
     * Creates or updates a hibernate model for the website and saves it to the db
     *
     * @param url            URL of the website
     * @param websiteContent The content of the website
     * @param outgoingLinks  All outgoing Links on the website
     */
    public void saveWebsite(String url, String websiteContent, Collection<Link> outgoingLinks, String title) {
        Website website = dbSession.find(Website.class, url);
        if (website == null) {
            website = new Website();
            website.setContent(websiteContent);
            website.setUrl(url);
            website.setLastCrawled(new Date());
            website.setOutgoingLinks(outgoingLinks);
            website.setTitle(title);
            saveWebsiteToDb(website);
        } else {
            if (!StringUtils.equals(websiteContent, website.getContent())) {
                website.setContent(websiteContent);
                website.setLastChanged(new Date());
                saveWebsiteToDb(website);
            }
        }
    }

    private void saveWebsiteToDb(Website website) {
        try {
            saveWebsiteLinksToDb(website.getOutgoingLinks());
            dbSession.getTransaction().begin();
            dbSession.persist(website);
            dbSession.flush();
            dbSession.getTransaction().commit();
        } catch (HibernateException e) {
            LOG.warn("Failed to save website \"" + website.getUrl() + "\" to db due to ", e);
        }
    }

    private void saveWebsiteLinksToDb(Collection<Link> outgoingLinks) {
        for (Link link : outgoingLinks) {
            try {
                Link existingIdenticalLinkInDb = findIdenticalLinkInDb(link);
                if(existingIdenticalLinkInDb != null){
                    link = existingIdenticalLinkInDb;
                }
                else {
                    dbSession.getTransaction().begin();
                    dbSession.persist(link);
                    dbSession.flush();
                    dbSession.getTransaction().commit();
                }
            } catch (HibernateException e) {
                LOG.warn("Failed to save Link \"" + link.getUrl() + "\" to db due to ", e);
            }
        }
    }

    private Link findIdenticalLinkInDb(Link link) {
        Link existingLink = null;

        CriteriaBuilder cb = dbSession.getCriteriaBuilder();
        CriteriaQuery<Link> cr = cb.createQuery(Link.class);
        Root<Link> root = cr.from(Link.class);
        cr.select(root).where(cb.equal(root.get("url"), link.getUrl()));

        Query<Link> query = dbSession.createQuery(cr);
        List<Link> results = query.getResultList();

        for(Link result: results){
            if(StringUtils.equals(result.getLinkText(),link.getLinkText())){
                existingLink = result;
                break;
            }
        }

        return existingLink;
    }
}
