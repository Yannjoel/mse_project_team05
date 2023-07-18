package de.mse.team5.crawler.backgroundtasks;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import de.mse.team5.crawler.dto.DownloadedDocDTO;
import de.mse.team5.hibernate.HibernateUtil;
import de.mse.team5.hibernate.helper.WebsiteModelUtils;
import de.mse.team5.hibernate.model.Website;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.StatelessSession;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class SaveDownloadedWebsiteToDbRunnable implements Runnable {
    private static final String TUEBINGEN = "TÜBINGEN";
    private final Collection<DownloadedDocDTO> downloadedWebsitesToProcess;

    private final LanguageDetector detector;

    public SaveDownloadedWebsiteToDbRunnable(Collection<DownloadedDocDTO> downloadedWebsitesToProcess) {
        this.downloadedWebsitesToProcess = downloadedWebsitesToProcess;
        detector = LanguageDetectorBuilder
                .fromAllLanguages()
                .withMinimumRelativeDistance(0.6)
                .build();
    }

    @Override
    public void run() {
        try (StatelessSession dbSession = HibernateUtil.getSessionFactory().openStatelessSession()) {
            WebsiteModelUtils websiteUtils = new WebsiteModelUtils(dbSession);

            for (DownloadedDocDTO docDTO : downloadedWebsitesToProcess) {
                saveDownloadedWebsiteToDb(docDTO, websiteUtils);
            }
        }
    }

    private void saveDownloadedWebsiteToDb(DownloadedDocDTO docDTO, WebsiteModelUtils websiteUtils) {
        Website website = docDTO.getSite();
        Document doc = docDTO.getDownloadedData();

        if (doc != null && siteContainsTuebingen(doc)) {
            if (isEnglish(doc)) {
                addDocDataToWebsite(website, doc, websiteUtils);
            } else {
                website.setRelevantForSearch(false);
            }
            addOutgoingLinksForWebsite(website, doc, websiteUtils);
        } else {
            website.setRelevantForSearch(false);
        }
        website.setStagedForCrawling(false);
        website.setLastCrawled(docDTO.getFetchTime());
        websiteUtils.updateWebsiteInDb(website);
    }

    private void addDocDataToWebsite(Website website, Document doc, WebsiteModelUtils websiteUtils) {
        String websiteContent = doc.text();
        String websiteTitle = doc.title();

        //only use a max title length due to db limitation
        if(StringUtils.isNotEmpty(websiteTitle) && websiteTitle.length() > 80){
            websiteTitle = StringUtils.abbreviate(websiteTitle, "\u2026",80);
        }

        if (!StringUtils.equals(websiteContent, website.getWholeDocument())) {
            website.setWholeDocument(websiteContent);
            String mainText;
            if(!doc.body().select("main").isEmpty()){
                mainText = doc.body().select("main").text();
            }
            else {
                mainText = doc.body().text();
            }
            website.setBody(mainText);
            website.setLastChanged(new Date());
        }
        website.setTitle(websiteTitle);
        website.setRelevantForSearch(true);
    }

    private void addOutgoingLinksForWebsite(Website website, Document doc, WebsiteModelUtils websiteUtils) {
        String url = website.getUrl();
        Collection<Website> outgoingLinks = websiteUtils.getOutgoingLinksForDoc(doc, url);
        website.setOutgoingLinks(outgoingLinks);
    }

    private boolean siteContainsTuebingen(Document doc) {
        //tuebingen in url
        if (StringUtils.containsIgnoreCase(doc.location(), TUEBINGEN))
            return true;

        //tuebingen in title
        if (StringUtils.containsIgnoreCase(doc.title(), TUEBINGEN))
            return true;

        //tuebingen in content
        return StringUtils.containsIgnoreCase(doc.text(), TUEBINGEN);
    }

    private boolean isEnglish(Document doc) {
        SortedMap<Language, Double> detectedLanguages = detector.computeLanguageConfidenceValues(doc.text());
        Double englishPrediction = detectedLanguages.get(Language.ENGLISH);
        return englishPrediction != null && detectedLanguages.get(Language.ENGLISH) > 0.92;
    }
}
