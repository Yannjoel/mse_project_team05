package de.mse.team5.crawler.backgroundtasks;

import de.mse.team5.crawler.dto.DownloadedDocDTO;
import de.mse.team5.hibernate.HibernateUtil;
import de.mse.team5.hibernate.helper.WebsiteModelUtils;
import de.mse.team5.hibernate.model.Website;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.jsoup.nodes.Document;

import java.util.*;

public class SaveDownloadedWebsiteToDbRunnable implements Runnable {

    private static final Logger LOG = LogManager.getLogger(SaveDownloadedWebsiteToDbRunnable.class);
    private final Collection<DownloadedDocDTO> downloadedWebsitesToProcess;

    public SaveDownloadedWebsiteToDbRunnable(Collection<DownloadedDocDTO> downloadedWebsitesToProcess) {
        this.downloadedWebsitesToProcess = downloadedWebsitesToProcess;
    }

    @Override
    public void run() {
        StatelessSession dbSession = HibernateUtil.getSessionFactory().openStatelessSession();
        WebsiteModelUtils websiteUtils = new WebsiteModelUtils(dbSession);

        for (DownloadedDocDTO docDTO : downloadedWebsitesToProcess) {
            Website website = docDTO.getSite();
            String url = website.getUrl();
            LOG.debug("processing " + url);
            Document doc = docDTO.getDownloadedData();

            if (doc != null) {
                String websiteContent = doc.body().text();
                String websiteTitle = doc.title();
                Collection<Website> outgoingLinks = websiteUtils.getOutgoingLinksForDoc(doc, url);

                if (!StringUtils.equals(websiteContent, website.getContent())) {
                    website.setContent(websiteContent);
                    website.setLastChanged(new Date());
                }
                website.setLastCrawled(docDTO.getFetchTime());
                website.setOutgoingLinks(outgoingLinks);
                website.setTitle(websiteTitle);
            }
            website.setStagedForCrawling(false);
            websiteUtils.updateWebsiteInDb(website);
        }
    }
}
