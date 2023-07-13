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
    private static final String[] EN_LANGUAGE_CODES = {"en", "en-gb", "en-us", "en-au", "uk", "en-uk", "en-029", "en-BZ", "en-ca"};
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
        try(StatelessSession dbSession = HibernateUtil.getSessionFactory().openStatelessSession()) {
            WebsiteModelUtils websiteUtils = new WebsiteModelUtils(dbSession);

            for (DownloadedDocDTO docDTO : downloadedWebsitesToProcess) {
                saveDownloadedWebsiteToDb(docDTO, websiteUtils);
            }
        }
    }

    private void saveDownloadedWebsiteToDb(DownloadedDocDTO docDTO, WebsiteModelUtils websiteUtils) {
        Website website = docDTO.getSite();
        Document doc = docDTO.getDownloadedData();

        if (doc != null && isSiteRelevantForSearch(doc)) {
            addDocDataToWebsite(website, doc, websiteUtils);
        } else {
            website.setRelevantForSearch(false);
        }
        website.setStagedForCrawling(false);
        website.setLastCrawled(docDTO.getFetchTime());
        websiteUtils.updateWebsiteInDb(website);
    }

    private void addDocDataToWebsite(Website website, Document doc, WebsiteModelUtils websiteUtils) {
        String websiteContent = doc.text();
        String url = website.getUrl();

        String websiteTitle = doc.title();
        Collection<Website> outgoingLinks = websiteUtils.getOutgoingLinksForDoc(doc, url);

        if (!StringUtils.equals(websiteContent, website.getWholeDocument())) {
            website.setWholeDocument(websiteContent);
            website.setBody(doc.body().text());
            website.setLastChanged(new Date());
        }
        website.setOutgoingLinks(outgoingLinks);
        website.setTitle(websiteTitle);
        website.setRelevantForSearch(true);
    }

    private boolean isSiteRelevantForSearch(Document doc) {
        if(isEnglish(doc)){
            //tuebingen in url
            if(StringUtils.containsIgnoreCase(doc.location(), TUEBINGEN))
                return true;

            //tuebingen in title
            if(StringUtils.containsIgnoreCase(doc.title(), TUEBINGEN))
                return true;

            //tuebingen in content
            return StringUtils.containsIgnoreCase(doc.text(), TUEBINGEN);
        }
        return false;
    }

    private boolean isEnglish(Document doc) {
        Element htmlElement = doc.select("html").first();
        if(htmlElement != null) {
            String providedLangAttribute = htmlElement.attr("lang");
            if (StringUtils.isNotEmpty(providedLangAttribute) && !StringUtils.containsAnyIgnoreCase(providedLangAttribute, EN_LANGUAGE_CODES)) {
                return false;
            }
        }
        //Recheck if lang was en due to too many false flagged sites
        SortedMap<Language, Double> detectedLanguages = detector.computeLanguageConfidenceValues(doc.text());
        Double englishPrediction = detectedLanguages.get(Language.ENGLISH);
        return englishPrediction != null && detectedLanguages.get(Language.ENGLISH) > 0.95;
    }
}
