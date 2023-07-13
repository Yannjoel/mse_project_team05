package de.mse.team5.crawler.dto;

import de.mse.team5.hibernate.model.Website;
import org.jsoup.nodes.Document;

import java.time.Instant;

public class DownloadedDocDTO {
    private final Document downloadedData;
    private final Instant fetchTime;

    private final Website site;

    public DownloadedDocDTO(Website site, Document doc, Instant fetchTime) {
        this.site = site;
        this.downloadedData =doc;
        this.fetchTime = fetchTime;
    }

    public Document getDownloadedData() {
        return downloadedData;
    }

    public Instant getFetchTime() {
        return fetchTime;
    }

    public Website getSite() {
        return site;
    }
}
