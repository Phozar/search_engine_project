package searchengine.util.morphology;

import lombok.extern.slf4j.Slf4j;
import searchengine.model.EntityIndex;
import searchengine.model.EntityLemma;
import searchengine.model.EntityPage;
import searchengine.model.EntitySite;
import searchengine.model.repositories.RepositoryIndex;
import searchengine.model.repositories.RepositoryLemma;
import searchengine.util.ClearHtmlCode;

import java.io.IOException;
import java.util.*;

@Slf4j

public class StartLemmaFind implements Runnable {

    private final EntitySite entitySite;
    private final Morphology morphology = new MorphologyAnalyzer();
    public static volatile boolean stop = false;
    private final EntityPage entityPage;
    private final RepositoryLemma repositoryLemma;
    private final RepositoryIndex repositoryIndex;


    public StartLemmaFind(EntitySite entitySite, EntityPage entityPage, RepositoryIndex repositoryIndex,
                          RepositoryLemma repositoryLemma) {
        this.repositoryLemma = repositoryLemma;
        this.repositoryIndex = repositoryIndex;
        this.entitySite = entitySite;

        stop = false;
        this.entityPage = entityPage;

    }

    @Override
    public void run() {
        try {
            startLemmaFinder(entityPage);
        } catch (IOException e) {
            log.debug(e.getMessage());
        }

    }

    private void startLemmaFinder(EntityPage entityPage) throws IOException {
        String title = ClearHtmlCode.clear(entityPage.getContent(), "title");
        String body = ClearHtmlCode.clear(entityPage.getContent(), "body");
        String x = title.concat(" " + body);
        HashMap<String, Integer> textLemmaList = morphology.getLemmaList(x);
        Set<String> allWords = new HashSet<>(textLemmaList.keySet());
        for (String lemma : allWords) {

            Integer count = textLemmaList.get(lemma);
            if (stop) {
                continue;
            }
            saveLemmaAndIndex(lemma, entityPage, count);

        }
    }

    private void saveLemmaAndIndex(String lemma, EntityPage entityPage, Integer count) {
        synchronized (entitySite) {
            EntityLemma lemma1 = repositoryLemma.findByLemmaAndSite(lemma, entitySite);
            if (lemma1 == null) {
                EntityLemma entityLemma = new EntityLemma();
                entityLemma.setSite(entitySite);
                entityLemma.setLemma(lemma);
                entityLemma.setFrequency(1);
                lemma1 = repositoryLemma.saveAndFlush(entityLemma);
            } else {
                lemma1.setFrequency(lemma1.getFrequency() + 1);
                repositoryLemma.saveAndFlush(lemma1);
            }
            EntityIndex entityIndex = new EntityIndex();
            entityIndex.setPage(entityPage);
            entityIndex.setRank(count);
            entityIndex.setLemma(lemma1);
            repositoryIndex.saveAndFlush(entityIndex);
        }
    }
}
