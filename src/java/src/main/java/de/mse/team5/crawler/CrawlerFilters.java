package de.mse.team5.crawler;

import org.apache.commons.lang3.StringUtils;
public class CrawlerFilters {

    private CrawlerFilters(){
        //prevent initialization
    }

    private static final String[] URL_ENDINGS_TO_IGNORE = new String[]{".pdf", ".png", ".jpg", ".jpeg", ".mp3", ".xls 2"};

    public static boolean fitsCrawlFilter(String url) {
        return !StringUtils.endsWithAny(url, URL_ENDINGS_TO_IGNORE);
    }
}
