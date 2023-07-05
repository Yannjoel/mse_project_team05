package de.mse.team5.hibernate.model;

import jakarta.persistence.*;

@Entity
@Table(name = "links", indexes = {
        @Index(name = "url_idx", columnList = "url")
})
public class Link {
    @Id
    private String url;

    @Id
    @GeneratedValue
    public int id;

    @Basic
    private String linkText;

    @Basic
    private String surroundingText;

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
}
