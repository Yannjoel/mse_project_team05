package de.mse.team5.ui.web.service;

import de.mse.team5.hibernate.HibernateUtil;
import de.mse.team5.hibernate.model.Website;
import de.mse.team5.ui.web.dto.WebsiteSearchResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import smile.nlp.SimpleCorpus;
import smile.nlp.Text;
import smile.nlp.relevance.BM25;
import smile.nlp.relevance.Relevance;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SearchService {

    public List<Website> getAllWebsites() {
        Session dbSession = HibernateUtil.getSessionFactory().getCurrentSession();
        //should probably use block-wise fetching when using bigger datasets
        CriteriaBuilder cb = dbSession.getCriteriaBuilder();
        CriteriaQuery<Website> cr = cb.createQuery(Website.class);
        Root<Website> root = cr.from(Website.class);
        cr.select(root);

        Query<Website> query = dbSession.createQuery(cr);
        List<Website> results = query.getResultList();

        return results;
    }

    public List<WebsiteSearchResult> getWebsitesForQuery(String query) {
        //ToDo: implement own search logic
        List<Website> allWebsites = getAllWebsites();

        BM25 testRanker = new BM25();
        SimpleCorpus corpus = new SimpleCorpus();
        for (Website website : allWebsites) {
            Text text = new Text(website.getUrl(), website.getTitle(), website.getContent());
            corpus.add(text);
        }
        List<WebsiteSearchResult> results = new ArrayList<>();
        var bm25Results = corpus.search(testRanker, query);
        while (bm25Results.hasNext()) {
            Relevance bm25Result = bm25Results.next();
            WebsiteSearchResult result = new WebsiteSearchResult();
            result.setRanking(bm25Result.score);

            String websiteId = bm25Result.text.id;
            Optional<Website> websiteForId = allWebsites.stream().filter(website -> StringUtils.equals(websiteId, website.getUrl())).findFirst();

            if (websiteForId.isPresent()) {
                result.setWebsite(websiteForId.get());

                results.add(result);
            }
        }

        saveResultToFile(query, results);

        return results;
    }


    private void saveResultToFile(String query, List<WebsiteSearchResult> results) {
        StringBuilder txtResult = new StringBuilder();
        txtResult.append("query: ").append(query).append("\n");
        txtResult.append("Results\n");
        int rank = 0;
        for (WebsiteSearchResult result : results) {
            rank++;
            txtResult.append(rank).append(".\t url: ").append(result.getWebsite().getUrl()).append("\n");
            txtResult.append("\t score: ").append(result.getRanking()).append("\n");
        }
        txtResult.append("--------------------------------------------------------------------\n");
        try {
            FileUtils.writeStringToFile(new File("results.txt"), txtResult.toString(), StandardCharsets.UTF_8, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
