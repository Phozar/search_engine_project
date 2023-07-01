package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteCfg;
import searchengine.config.SitesListCfg;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.EntitySite;
import searchengine.model.Status;
import searchengine.model.repositories.RepositoryLemma;
import searchengine.model.repositories.RepositoryPage;
import searchengine.model.repositories.RepositorySite;
import searchengine.services.StatisticsService;

import java.lang.constant.Constable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final RepositorySite repositorySite;
    private final RepositoryPage repositoryPage;
    private final SitesListCfg sites;
    private final RepositoryLemma repositoryLemma;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        List<SiteCfg> sitesList = sites.getSites();
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        total.setIndexing(false);
        for (SiteCfg site : sitesList) {
            Status statusSiteByUrl = repositorySite.findByUrl(site.getUrl());
            if (statusSiteByUrl != null && statusSiteByUrl.equals(Status.INDEXING)) {
                total.setIndexing(true);
            }
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            EntitySite entitySite = repositorySite.findEntitySiteByUrl(site.getUrl());
            int pages = repositoryPage.findCountBySite(entitySite) == null ? 0 : repositoryPage.findCountBySite(entitySite);
            item.setPages(pages);
            int lemmas = repositoryLemma.countBySite(entitySite);
            item.setLemmas(lemmas);

            item.setStatus(statusSiteByUrl == null ? "" : statusSiteByUrl.toString());
            Constable errorSiteByUrl = repositorySite.findErrorByUrl(site.getUrl());
            item.setError(errorSiteByUrl == null ? "" : errorSiteByUrl.toString());
            item.setStatusTime(new Date().getTime());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        return getResponse(total, detailed);
    }

    private StatisticsResponse getResponse(TotalStatistics totalStatistics, List<DetailedStatisticsItem> items) {
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(totalStatistics);
        data.setDetailed(items);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
