package de.mse.team5.hibernate.helper;

import de.mse.team5.hibernate.model.Link;
import de.mse.team5.hibernate.model.Website;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
            if (link.hasAttr("href")) {
                String linkUrl = formatLinkUrl(link.attr("href"), currentFullUrl);
                if (linkUrl != null) {
                    String linkText = link.text();

                    //ToDo: get existing link for db if possible?
                    Link outgoingLink = new Link();
                    outgoingLink.setUrl(linkUrl);
                    outgoingLink.setLinkText(linkText);
                    outgoingLinks.add(outgoingLink);
                }
            }
        });
        //TODO duplicate check?
        return outgoingLinks;
    }

    /**
     * Ensures that all links are equally formatted
     *
     * @param linkUrl no yet formatted http(s) link
     * @return formatted link
     */
    private String formatLinkUrl(String linkUrl, String currentFullUrl) {
        String formattedURL = null;

        try {
            //append current url if link is relative or anchor link
            if(StringUtils.startsWith(linkUrl, "/") || StringUtils.startsWith(linkUrl, "#")  ){
                URL currentUrl = URI.create(currentFullUrl).toURL();
                String currentPathPrefix = currentUrl.getProtocol() + "://" + currentUrl.getHost();
                linkUrl = currentPathPrefix + linkUrl;
            }
            URL url = URI.create(linkUrl).toURL();
            formattedURL = url.getProtocol() + "://" + url.getHost() + url.getFile();
            //ToDo: check if we need to also focus on parameters
        } catch (Exception e) {
            LOG.warn("malformed url " + linkUrl, e);
        }
        return formattedURL;
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
        saveWebsiteLinksToDb(website.getOutgoingLinks());
        dbSession.getTransaction().begin();
        dbSession.persist(website);
        dbSession.getTransaction().commit();
    }

    private void saveWebsiteLinksToDb(Collection<Link> outgoingLinks) {
        for (Link link : outgoingLinks) {
            dbSession.getTransaction().begin();
            dbSession.persist(link);
            dbSession.getTransaction().commit();
        }
    }
}
