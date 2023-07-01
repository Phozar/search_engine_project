package searchengine.services;

import searchengine.dto.Response.IndexResponse;

public interface IndexingService {
    IndexResponse startIndexing();

    IndexResponse stopIndexing();

    IndexResponse indexPage(String url);


}
