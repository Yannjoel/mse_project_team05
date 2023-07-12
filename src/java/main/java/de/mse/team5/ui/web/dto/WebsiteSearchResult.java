package de.mse.team5.ui.web.dto;

import de.mse.team5.hibernate.model.Website;

/**
 * DTO for search results containing ranking and additional display information
 * ToDo: specify and reduce data used for display
 */
public class WebsiteSearchResult {
    private double ranking;
    private Website website;

    public double getRanking() {
        return ranking;
    }

    public void setRanking(double ranking) {
        this.ranking = ranking;
    }

    public Website getWebsite() {
        return website;
    }

    public void setWebsite(Website website) {
        this.website = website;
    }
}
