package de.mse.team5.hibernate.model;

import jakarta.persistence.*;

import java.util.Date;
import java.util.Collection;

@Entity
@Table(name = "website", indexes = {
        @Index(name = "url_idx", columnList = "url")
})
public class Website {
    @Id
    @Column(length = 2048, columnDefinition = "varchar(2048)")
    private String url;

    @Basic(optional = false)
    @Lob
    private String content;

    @Basic(optional = false)
    private String title;

    @Basic(optional = false)
    private Date lastCrawled;

    @Basic(optional = true)
    private Date lastChanged;

    @ManyToMany(cascade=CascadeType.ALL)
    @JoinTable(name = "website_link",
            joinColumns = @JoinColumn(name = "website_url"),
            inverseJoinColumns = @JoinColumn(name = "link_id"))
    private Collection<Link> outgoingLinks;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getLastCrawled() {
        return lastCrawled;
    }

    public void setLastCrawled(Date lastCrawled) {
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
    }

    public Collection<Link> getOutgoingLinks() {
        return outgoingLinks;
    }

    public void setOutgoingLinks(Collection<Link> outgoingLinks) {
        this.outgoingLinks = outgoingLinks;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
