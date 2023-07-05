package de.mse.team5.ui.web.controller;

import de.mse.team5.ui.web.dto.WebsiteSearchResult;
import de.mse.team5.ui.web.service.SearchService;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.core.appender.rewrite.MapRewritePolicy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SearchController {

    private SearchService searchService = new SearchService();

    @GetMapping(value="/", produces = MediaType.TEXT_HTML_VALUE)
    public String initialSearchPage(Model model){
        return "index.jsp";
    }

    @GetMapping(value = "/startCrawler", produces = MediaType.TEXT_HTML_VALUE)
    public String startCrawler(Model model){
        return "crawler.jsp";
    }

    @GetMapping(value = "/search",produces = MediaType.TEXT_HTML_VALUE)
    public String search(Model model, @RequestParam(name = "query") String query){
        List<WebsiteSearchResult> results = this.searchService.getWebsitesForQuery(query);
        model.addAttribute("results", results);
        model.addAttribute("query", query);

        return "searchResult.jsp";
    }
}
