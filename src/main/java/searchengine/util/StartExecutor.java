package searchengine.util;

import lombok.extern.slf4j.Slf4j;
import searchengine.model.EntitySite;
import searchengine.model.Status;
import searchengine.model.repositories.RepositoryIndex;
import searchengine.model.repositories.RepositoryLemma;
import searchengine.model.repositories.RepositoryPage;
import searchengine.model.repositories.RepositorySite;
import searchengine.services.NetworkService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.concurrent.*;

@Slf4j
public class StartExecutor implements Runnable {
    private final EntitySite entitySite;
    private final RepositoryPage repositoryPage;
    private static ForkJoinPool fjp;
    private final NetworkService network;
    private final RepositorySite repositorySite;
    private final RepositoryIndex repositoryIndex;
    private final RepositoryLemma repositoryLemma;


    public StartExecutor(EntitySite entitySite, RepositoryPage repositoryPage,
                         NetworkService network, RepositorySite repositorySite,
                         RepositoryLemma repositoryLemma, RepositoryIndex repositoryIndex) {
        this.entitySite = entitySite;
        this.repositoryPage = repositoryPage;
        this.network = network;
        this.repositorySite = repositorySite;
        this.repositoryLemma = repositoryLemma;
        this.repositoryIndex = repositoryIndex;
        fjp = new ForkJoinPool();


    }

    @Override
    public void run() {
        try {
            long startTime = System.currentTimeMillis();
            ExecutorHtml executorHtml = new ExecutorHtml(entitySite, entitySite.getUrl() + "/", repositoryPage,
                    network, repositorySite, repositoryIndex, repositoryLemma);
            fjp.invoke(executorHtml);

            if (!fjp.isShutdown()) {
                siteIndexed();
                log.info("Индексация сайта " + entitySite.getName() + " завершена, за время: " +
                        (System.currentTimeMillis() - startTime));
            }


        } catch (MalformedURLException exception1) {
            log.info("Ошибка старта индексации " + exception1.getMessage());
        } catch (IOException exception2) {
            log.info("Ошибка создания конструктора ExecutorHtml " + exception2.getMessage());
        }
    }


    public static void shutdown() {
        if (fjp != null && !fjp.isShutdown()) {
            fjp.shutdownNow();
        }
    }

    private void siteIndexed() {
        entitySite.setStatus_time(new Date());
        entitySite.setStatus(Status.INDEXED);
        repositorySite.saveAndFlush(entitySite);
    }
}

