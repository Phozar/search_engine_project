package searchengine.services;

import searchengine.config.SearchCfg;
import searchengine.dto.statistics.SearchDto;

import java.util.List;

public interface SearchService {
    List<SearchDto> allSiteSearch(SearchCfg searchCfg);
    List<SearchDto> siteSearch(SearchCfg searchCfg);
}