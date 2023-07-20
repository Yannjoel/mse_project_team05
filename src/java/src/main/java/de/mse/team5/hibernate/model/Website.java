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
        @Index(name = "host_idx", columnList = "hostUrl")
})
public class Website {

    private static final Logger LOG = LogManager.getLogger(Website.class);

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    Long id;

    @Column(length = 2048, columnDefinition = "varchar(2048)")
    private String url;

    @Basic
    private String anchor;

    @Column(columnDefinition = "LONGTEXT")
    private String wholeDocument;

    @Column(columnDefinition = "LONGTEXT")
    private String body;

    @Basic
    private String title;

    @Basic
    private Instant lastCrawled;

    @Basic
    private Date lastChanged;

    @ManyToMany(cascade = { CascadeType.ALL })
    @JoinTable(
            name = "Website_Website",
            joinColumns = { @JoinColumn(name = "website_url") },
            inverseJoinColumns = { @JoinColumn(name = "website_url") }
    )

    @Transient
    private Collection<Website> outgoingLinks;

    @Basic
    private boolean relevantForSearch;

    @Basic
    @ColumnDefault(value="false")
    private boolean failedToDownload;

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


    public String getWholeDocument() {
        return wholeDocument;
    }

    public void setWholeDocument(String wholeDocument) {
        this.wholeDocument = wholeDocument;
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

    public boolean isRelevantForSearch() {
        return relevantForSearch;
    }

    public void setRelevantForSearch(boolean relevantForSearch) {
        this.relevantForSearch = relevantForSearch;
    }

    public boolean isFailedToDownload() {
        return failedToDownload;
    }

    public void setFailedToDownload(boolean failedToDownload) {
        this.failedToDownload = failedToDownload;
    }

    public String getAnchor() {
        return anchor;
    }

    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
