package de.mse.team5.hibernate.model;

import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

@Entity
@Table(name = "link", indexes = {
        @Index(name = "custom_id_idx", columnList = "id"),
        @Index(name = "custom_url_idx", columnList = "url")
})
public class Link {

    private static final Logger LOG = LogManager.getLogger(Link.class);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    public int id;

    @Column(length = 2048, columnDefinition = "varchar(2048)")
    private String url;


    @Basic
    @Column(length = 2048, columnDefinition = "varchar(2048)")
    private String linkText;

    @Basic
    private String surroundingText;

    @Transient
    private URL urlObj;

    @Transient
    private boolean calculatedUrlObj = false;

    public static Link createNewLink(String href, String currentFullUrl, String linkText) {
        Link link = null;
        String linkUrl = formatLinkUrl(href, currentFullUrl);
        if (linkUrl != null) {
            //ToDo: get existing link for db if possible?
            link = new Link();
            link.setUrl(linkUrl);
            link.setLinkText(linkText);
        }
        return link;
    }

    public static Link createNewLink(String formattedLink) {
        return createNewLink(formattedLink , StringUtils.EMPTY, StringUtils.EMPTY);
    }

    public String getHostUrl() {
        URL ownUrlObj = getUrlObj();
        if (ownUrlObj == null) {
            return StringUtils.EMPTY;
        }
        return ownUrlObj.getProtocol() + "://" + ownUrlObj.getHost();
    }


    public URL getUrlObj() {
        if (!this.calculatedUrlObj) {
            try {
                this.urlObj = URI.create(this.url).toURL();
            } catch (MalformedURLException e) {
                LOG.warn("Malformed url " + this.url, e);
                this.urlObj = null;
            }
            this.calculatedUrlObj = true;
        }
        return this.urlObj;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLinkText() {
        return linkText;
    }

    public void setLinkText(String linkText) {
        this.linkText = linkText;
    }

    public String getSurroundingText() {
        return surroundingText;
    }

    public void setSurroundingText(String surroundingText) {
        this.surroundingText = surroundingText;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Ensures that all links are equally formatted
     *
     * @param linkUrl no yet formatted http(s) link
     * @return formatted link
     */
    private static String formatLinkUrl(String linkUrl, String currentFullUrl) {
        String formattedURL = null;

        try {
            //append current url if link is relative or anchor link
            if (StringUtils.startsWith(linkUrl, "/") || StringUtils.startsWith(linkUrl, "#")) {
                URL currentUrl = URI.create(currentFullUrl).toURL();
                String currentPathPrefix = currentUrl.getProtocol() + "://" + currentUrl.getHost();
                linkUrl = currentPathPrefix + linkUrl;
            }
            URL url = URI.create(linkUrl).toURL();
            formattedURL = formatLinkUrl(url);
            //ToDo: check if we need to also focus on parameters
        } catch (Exception e) {
            LOG.warn("malformed url " + linkUrl, e);
        }
        return formattedURL;
    }

    private static String formatLinkUrl(URL url) {
        return url.getProtocol() + "://" + url.getHost() + url.getFile();
    }

    public String getUrlPath() {
        URL ownUrlObj = getUrlObj();
        if (ownUrlObj == null) {
            return StringUtils.EMPTY;
        }
        return ownUrlObj.getPath();
    }
}
