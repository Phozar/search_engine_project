package searchengine.services;

import searchengine.dto.statistics.SearchDto;
import searchengine.config.SearchCfg;

import java.util.List;

public interface SearchService {
    List<SearchDto> allSiteSearch(SearchCfg searchCfg);
    List<SearchDto> siteSearch(SearchCfg searchCfg);
}