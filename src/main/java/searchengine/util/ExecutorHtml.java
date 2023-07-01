package searchengine.util;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.*;
import searchengine.model.repositories.RepositoryIndex;
import searchengine.model.repositories.RepositoryLemma;
import searchengine.model.repositories.RepositoryPage;
import searchengine.model.repositories.RepositorySite;
import searchengine.util.morphology.StartLemmaFind;
import searchengine.services.NetworkService;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Slf4j
public class ExecutorHtml extends RecursiveAction {
    private final String url;
    private final EntitySite entitySite;
    private final RepositoryPage repositoryPage;
    private final RepositorySite repositorySite;
    private final RepositoryIndex repositoryIndex;
    private final RepositoryLemma repositoryLemma;
    private static Pattern patternUrl;
    private static NetworkService network;
    public static volatile boolean stop = false;
    private final static Set<String> setAbsUrls = ConcurrentHashMap.newKeySet();
    private final ExecutorService executorService;

    public ExecutorHtml(String url, EntitySite entitySite, RepositoryPage repositoryPage,
                        RepositorySite repositorySite, RepositoryLemma repositoryLemma,
                        RepositoryIndex repositoryIndex, ExecutorService executorService) {
        this.url = url.trim();
        this.entitySite = entitySite;
        this.repositoryPage = repositoryPage;
        this.repositorySite = repositorySite;
        this.repositoryIndex = repositoryIndex;
        this.repositoryLemma = repositoryLemma;
        this.executorService = executorService;

    }

    public ExecutorHtml(EntitySite entitySite, String url, RepositoryPage repositoryPage, NetworkService network,
                        RepositorySite repositorySite, RepositoryIndex repositoryIndex, RepositoryLemma repositoryLemma) throws IOException {
        this.repositorySite = repositorySite;
        this.entitySite = entitySite;
        this.url = url;
        this.repositoryPage = repositoryPage;
        this.repositoryIndex = repositoryIndex;
        this.repositoryLemma = repositoryLemma;
        ExecutorHtml.network = network;
        patternUrl = Pattern.compile("(jpg)|(JPG)|(PNG)|(png)|(PDF)|(pdf)|(JPEG)|(jpeg)|(BMP)|(bmp)");
        this.executorService = Executors.newFixedThreadPool(16);

    }

    @Override
    protected void compute() {

        CopyOnWriteArrayList<ExecutorHtml> tasks = new CopyOnWriteArrayList<>();

        try {

            if (stop) {
                stopExecute();
                return;
            }
            Thread.sleep(150);
            Connection.Response response = network.getConnection(url);
            if (!network.isAvailableContent(response)) {
                setAbsUrls.add(url);
                return;
            }
            if (!addAbsUrlToSet(url)) {
                setAbsUrls.add(url);
                return;
            }

            Document document = response.parse();

            //TODO update Site time
            updateSiteTime(entitySite);

            //TODO Create and save page
            EntityPage entityPage = getEntityPage(document, url);
            repositoryPage.saveAndFlush(entityPage);

            //TODO Create and save lemmas
            Future<?> submit = executorService.submit(new StartLemmaFind(entitySite, entityPage,
                                                                         repositoryIndex, repositoryLemma));
            submit.get();

            log.info("Добавлена запись " + url + " " + Thread.currentThread());

            Elements elements = document.select("a");
            for (Element element : elements) {
                String absUrl = element.absUrl("href").
                        indexOf(0) == '/' ? entitySite.getUrl() + element.absUrl("href") :
                        element.absUrl("href");

                if (!absUrl.isEmpty()
                        && !absUrl.contains("#")
                        && absUrl.startsWith(entitySite.getUrl())
                        && !patternUrl.matcher(absUrl).find()
                        && !setAbsUrls.contains(absUrl)) {

                    ExecutorHtml executorHtml = new ExecutorHtml(absUrl, entitySite, repositoryPage, repositorySite,
                            repositoryLemma, repositoryIndex, executorService);
                    tasks.add(executorHtml);
                    executorHtml.fork();
                }
            }

            tasks.forEach(ForkJoinTask::join);

        } catch (Exception exception4) {
            setAbsUrls.add(url);
            log.debug("Ошибка подключения к сайту " + url + exception4.getMessage());
        }

    }

    private EntityPage getEntityPage(Document document, String url) {
        EntityPage entityPage = new EntityPage();
        entityPage.setPath(getPath(url));
        entityPage.setSite(entitySite);
        entityPage.setCode(200);
        entityPage.setContent(document.outerHtml());
        return entityPage;
    }

    private String getPath(String url) {
        return url.substring(entitySite.getUrl().length());

    }

    private void updateSiteTime(EntitySite entitySite) {
        entitySite.setStatus_time(new Date());
        repositorySite.saveAndFlush(entitySite);
    }


    private boolean addAbsUrlToSet(String url) {
        return setAbsUrls.add(url);

    }

    private void stopExecute() {
        StartLemmaFind.stop = true;
        StartExecutor.shutdown();
    }

    public static void clearSetAbsUrl() {
        setAbsUrls.clear();
    }


}
