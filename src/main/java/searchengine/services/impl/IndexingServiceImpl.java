package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.SiteCfg;
import searchengine.config.SitesListCfg;
import searchengine.dto.Response.IndexResponse;
import searchengine.model.EntityPage;
import searchengine.model.EntitySite;
import searchengine.model.Status;
import searchengine.model.repositories.RepositoryIndex;
import searchengine.model.repositories.RepositoryLemma;
import searchengine.model.repositories.RepositoryPage;
import searchengine.model.repositories.RepositorySite;
import searchengine.util.morphology.StartLemmaFind;
import searchengine.services.IndexingService;
import searchengine.services.NetworkService;
import searchengine.util.ExecutorHtml;
import searchengine.util.StartExecutor;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;


@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesListCfg sitesList;
    private final RepositoryLemma repositoryLemma;
    private final RepositoryIndex repositoryIndex;
    private final RepositorySite repositorySite;
    private final RepositoryPage repositoryPage;
    private final NetworkService network;
    private final Pattern pattern = Pattern.compile("^(https?://)?([\\w-]{1,32}\\.[\\w-]{1,32})[^\\s@]*$");


    @SneakyThrows
    @Override
    public IndexResponse startIndexing() {
        ExecutorHtml.stop = false;
        if (isIndexing()) {
            return new IndexResponse(false, "Индексация уже запущена");
        }
        Thread thread = new Thread(() -> {
            log.info("Индексация запущена.");
            repositorySite.deleteAll();
            ExecutorHtml.clearSetAbsUrl();

            for (SiteCfg site : sitesList.getSites()) {
                EntitySite entitySite = getEntitySite(site, Status.INDEXING);
                repositorySite.saveAndFlush(entitySite);
                StartExecutor startExecutor = new StartExecutor(entitySite, repositoryPage, network, repositorySite,
                        repositoryLemma, repositoryIndex);
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.submit(startExecutor);
            }
        });
        thread.start();

        return new IndexResponse(true, "");
    }

    @SneakyThrows
    @Override
    public IndexResponse stopIndexing() {
        ExecutorHtml.stop = true;
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.schedule(() -> {
            List<EntitySite> all = repositorySite.findAll();
            for (EntitySite entitySite : all) {
                if (entitySite.getStatus().equals(Status.INDEXING)) {
                    entitySite.setStatus(Status.FAILED);
                    entitySite.setLast_error("Индексация остановлена пользователем");
                    repositorySite.saveAndFlush(entitySite);
                }
            }
        }, 3000, TimeUnit.MILLISECONDS);

        return new IndexResponse(true);
    }

    @Override
    public IndexResponse indexPage(String url) {
        try {
            if (url.isEmpty() || !pattern.matcher(url).find()) {
                return new IndexResponse(false, "Вы ввели неверный url");
            }
            Connection.Response connection = network.getConnection(url);
            Document document = connection.parse();
            if (connection.statusCode() == HttpStatus.OK.value()) {
                StartLemmaFind.stop = false;
                for (SiteCfg site : sitesList.getSites()) {
                    EntitySite entitySite = repositorySite.findEntitySiteByUrl(site.getUrl());
                    if (url.contains(site.getUrl()) && entitySite == null) {
                        saveEntitySiteAndPage(site, url, document);
                    } else if (url.contains(site.getUrl()) && entitySite != null) {
                        EntityPage entityPage = repositoryPage.findByPathAndSiteId(getPath(url, site), entitySite.getId());
                        if (entityPage != null) {
                            startLemmaFind(entitySite, entityPage);
                        } else {
                            EntityPage entityPage2 = getEntityPage(document, getPath(url, site), entitySite);
                            repositoryPage.saveAndFlush(entityPage2);
                            startLemmaFind(entitySite, entityPage2);
                        }
                    }
                    log.info("Страница " + url + " проиндексирована.");
                    return new IndexResponse(true);
                }
            }
        } catch (IOException e) {
            return new IndexResponse(false, "Сайт не доступен");
        }
        return new IndexResponse(false, "Данная страница находится за пределами сайтов, " +
                "указанных в конфигурационном файле");
    }

    private EntitySite getEntitySite(SiteCfg site, Status status) {
        EntitySite entitySite = new EntitySite();
        entitySite.setStatus(status);
        entitySite.setName(site.getName());
        entitySite.setUrl(site.getUrl());
        entitySite.setStatus_time(new Date());
        entitySite.setLast_error("");
        return entitySite;
    }

    private String getPath(String url, SiteCfg site) {
        return url.substring(site.getUrl().length());

    }

    private EntityPage getEntityPage(Document document, String path, EntitySite entitySite) {
        EntityPage entityPage = new EntityPage();
        entityPage.setPath(path);
        entityPage.setSite(entitySite);
        entityPage.setCode(200);
        entityPage.setContent(document.html());
        return entityPage;
    }

    private boolean isIndexing() {
        List<EntitySite> all = repositorySite.findAll();
        for (EntitySite entitySite : all) {
            if (entitySite.getStatus().equals(Status.INDEXING)) {
                return true;
            }
        }
        return false;
    }

    private void saveEntitySiteAndPage(SiteCfg site, String url, Document document) {
        EntitySite entitySite = getEntitySite(site, Status.INDEXED);
        repositorySite.saveAndFlush(entitySite);
        EntityPage entityPage = getEntityPage(document, getPath(url, site), entitySite);
        repositoryPage.saveAndFlush(entityPage);
        startLemmaFind(entitySite, entityPage);
    }

    private void startLemmaFind(EntitySite entitySite, EntityPage entityPage) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new StartLemmaFind(entitySite, entityPage, repositoryIndex, repositoryLemma));
    }
}
