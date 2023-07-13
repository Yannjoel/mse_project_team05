package de.mse.team5.crawler;

import de.mse.team5.hibernate.model.Website;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;

public class CrawlerFilters {


    //If enabled the crawler won't touch any other sites that it discovers and will only crawl the website mentioned in the entryUrls
    public static final Boolean siteRestrictedTestMode = Boolean.FALSE;

    public static final Collection<String> allowedUrls = Arrays.asList("https://www.tuebingen.de/", "https://yannick-huber.de", "https://tue.schulamt-bw.de/Startseite", "http://www.schulamt-tuebingen.de/", "https://www.figurentheater-tuebingen.de", "https://www.landestheater-tuebingen.de/", "https://www.zimmertheater-tuebingen.de/", "http://www.harlekintheater.de/", "http://www.theater-teo-tiger.de/", "https://www.vorstadttheater.de/");

    private static final String[] URL_ENDINGS_TO_IGNORE = new String[]{".pdf", ".png", ".jpg", ".jpeg", ".mp3", ".xls 2"};

    public static boolean fitsCrawlFilter(String url) {
        boolean fitsFilter = true;

        //test filter: check if site is in allowed List
        if (siteRestrictedTestMode) {
            boolean isInAllowedUrls = false;
            for (String allowedUrl : allowedUrls) {
                if (StringUtils.startsWith(url, allowedUrl)) {
                    isInAllowedUrls = true;
                    break;
                }
            }
            fitsFilter = isInAllowedUrls;
        }

        //Todo: Check for better filter to detect files
        if (StringUtils.endsWithAny(url, URL_ENDINGS_TO_IGNORE)) {
            fitsFilter = false;
        }
        return fitsFilter;
    }
}
