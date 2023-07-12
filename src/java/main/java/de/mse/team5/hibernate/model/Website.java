package de.mse.team5.hibernate.model;

import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.ColumnDefault;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.Collection;

@Entity
@Table(name = "website", indexes = {
        @Index(name = "url_idx", columnList = "url")
})
public class Website {

    private static final Logger LOG = LogManager.getLogger(Website.class);

    @Id
    @Column(length = 2048, columnDefinition = "varchar(2048)")
    private String url;

    @Basic
    @Lob
    private String content;

    @Basic
    private String title;

    @Basic
    private Instant lastCrawled;

    @Basic
    private Date lastChanged;

    @ManyToMany(cascade=CascadeType.ALL)
    private Collection<Website> outgoingLinks;

    @Transient
    private URL urlObj;

    @Transient
    private boolean calculatedUrlObj = false;

    @Basic(optional = false)
    private String hostUrl;

    @Basic()
    @ColumnDefault(value="false")
    private boolean stagedForCrawling;

    @Basic()
    @ColumnDefault(value="false")
    private boolean blockedByRobotsTxt;

    public boolean isBlockedByRobotsTxt() {
        return blockedByRobotsTxt;
    }

    public void setBlockedByRobotsTxt(boolean blockedByRobotsTxt) {
        this.blockedByRobotsTxt = blockedByRobotsTxt;
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

    public String getUrlPath() {
        URL ownUrlObj = getUrlObj();
        if (ownUrlObj == null) {
            return StringUtils.EMPTY;
        }
        return ownUrlObj.getPath();
    }


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getLastCrawled() {
        return lastCrawled;
    }

    public void setLastCrawled(Instant lastCrawled) {
        this.lastCrawled = lastCrawled;
    }

    public Date getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(Date lastChanged) {
        this.lastChanged = lastChanged;
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        this.hostUrl = extractHostUrl();
    }

    private String extractHostUrl() {
        URL ownUrlObj = getUrlObj();
        if (ownUrlObj == null) {
            return StringUtils.EMPTY;
        }
        return ownUrlObj.getProtocol() + "://" + ownUrlObj.getHost();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Collection<Website> getOutgoingLinks() {
        return outgoingLinks;
    }

    public void setOutgoingLinks(Collection<Website> outgoingLinks) {
        this.outgoingLinks = outgoingLinks;
    }

    public String getHostUrl() {
        return hostUrl;
    }

    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }

    public boolean isStagedForCrawling() {
        return stagedForCrawling;
    }

    public void setStagedForCrawling(boolean stagedForCrawling) {
        this.stagedForCrawling = stagedForCrawling;
    }
}
