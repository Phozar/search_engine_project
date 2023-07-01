package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.SearchCfg;
import searchengine.dto.statistics.SearchDto;
import searchengine.model.EntityIndex;
import searchengine.model.EntityLemma;
import searchengine.model.EntityPage;
import searchengine.model.EntitySite;
import searchengine.model.repositories.RepositoryIndex;
import searchengine.model.repositories.RepositoryLemma;
import searchengine.model.repositories.RepositoryPage;
import searchengine.model.repositories.RepositorySite;
import searchengine.util.morphology.Morphology;
import searchengine.services.SearchService;
import searchengine.util.GetSearchDto;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final Morphology morphology;

    private final RepositoryLemma lemmaRepository;
    private final RepositoryPage pageRepository;
    private final RepositoryIndex indexRepository;
    private final RepositorySite siteRepository;
    private final static int newFixedThreadPool = Runtime.getRuntime().availableProcessors();

    @SneakyThrows
    @Override
    public List<SearchDto> allSiteSearch(SearchCfg searchCfg) {
        List<EntitySite> entitySites = siteRepository.findAll();
        HashMap<EntityPage, Float> entityPageFloatHashMap = new HashMap<>();
        Set<String> lemmaSet = getLemmaSet(searchCfg.getQuery());
        for (EntitySite entitySite : entitySites) {
            List<EntityLemma> foundLemmaList = getLemmaListFromSite(lemmaSet, entitySite);
            if (foundLemmaList.isEmpty()) {
                log.debug("Поисковый запрос сайта " + entitySite.getName() + " обработан. Ответ пустой.");
                continue;
            }
            log.info("Поисковый запрос сайта " + entitySite.getName() + " обработан. Ответ получен.");
            entityPageFloatHashMap.putAll(getPageList(foundLemmaList, entitySite, searchCfg.getLimit()));
        }
        if (entityPageFloatHashMap.isEmpty()) {
            return new ArrayList<>();
        }
        HashMap<EntityPage, Float> resultMap = getResultMap(entityPageFloatHashMap, searchCfg.getLimit());
        return getSearchDto(resultMap, lemmaSet);
    }

    @SneakyThrows
    @Override
    public List<SearchDto> siteSearch(SearchCfg searchCfg) {
        EntitySite site = siteRepository.findEntitySiteByUrl(searchCfg.getSite());
        Set<String> lemmaSet = getLemmaSet(searchCfg.getQuery());
        List<EntityLemma> foundLemmaList = getLemmaListFromSite(lemmaSet, site);
        if (foundLemmaList.isEmpty()) {
            log.debug("Поисковый запрос обработан. Ответ пустой.");
            return new ArrayList<>();
        }
        log.info("Поисковый запрос обработан. Ответ получен.");
        HashMap<EntityPage, Float> pageFloatHashMap = getPageList(foundLemmaList, site, searchCfg.getLimit());
        HashMap<EntityPage, Float> resultMap = getResultMap(pageFloatHashMap, searchCfg.getLimit());
        if (pageFloatHashMap.isEmpty()) {
            return new ArrayList<>();
        }
        return getSearchDto(resultMap, lemmaSet);
    }

    private List<SearchDto> getSearchDto(HashMap<EntityPage, Float> entityPageFloatHashMap, Set<String> entityLemmas) throws ExecutionException, InterruptedException {
        List<SearchDto> resultList = new ArrayList<>();
        List<Future> tasks = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(newFixedThreadPool);
        for (Map.Entry<EntityPage, Float> entry : entityPageFloatHashMap.entrySet()) {
            GetSearchDto searchDto = new GetSearchDto(entry, entityLemmas, morphology);
            var submit = executorService.submit(searchDto);
            tasks.add(submit);
        }
        for (Future future : tasks) {
            SearchDto searchDto = (SearchDto) future.get();
            resultList.add(searchDto);
        }
        return resultList;
    }

    private HashMap<EntityPage, Float> getPageList(List<EntityLemma> entityLemmas,
                                                   EntitySite entitySite, Integer limit) {
        HashMap<EntityPage, Float> resultMap = new HashMap<>();
        String firstLemma = entityLemmas.stream().findFirst().get().getLemma();
        CopyOnWriteArrayList<EntityPage> entityPages = pageRepository.findByLemma(firstLemma, entitySite);
        if (entityPages.isEmpty()) {
            return new HashMap<>();
        } else {
            if (entityLemmas.size() == 1) {
                for (EntityPage entityPage : entityPages) {
                    EntityIndex entityIndex = indexRepository.findByPageAndLemma(entityPage, entityLemmas.get(0));
                    resultMap.put(entityPage, entityIndex.getRank());
                }
                return getSortedMap(resultMap, limit);
            }
            List<EntityIndex> entityIndices = indexRepository.findByPagesAndLemmas(entityLemmas, entityPages);
            Map<EntityPage, EntityIndex> collect1 = new HashMap<>();
            for (EntityIndex entityIndex : entityIndices) {
                collect1.put(entityIndex.getPage(), entityIndex);
            }

            for (EntityLemma entityLemma : entityLemmas) {
                if (entityLemma.getLemma().equals(firstLemma)) {
                    continue;
                }
                for (EntityPage entityPage : entityPages) {
                    if (collect1.get(entityPage) == null) {
                        resultMap.remove(entityPage);
                        entityPages.remove(entityPage);
                    } else {
                        float rank = resultMap.get(entityPage) == null ? 0 : resultMap.get(entityPage);
                        resultMap.put(entityPage, collect1.get(entityPage).getRank() + rank);
                    }
                }
            }
            return getSortedMap(resultMap, limit);
        }

    }

    private HashMap<EntityPage, Float> getSortedMap(HashMap<EntityPage, Float> resultMap, Integer limit) {
        return resultMap.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .limit(limit).collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private HashMap<EntityPage, Float> getResultMap(HashMap<EntityPage, Float> map, Integer limit) {
        Float maxRank = map.entrySet().stream().findFirst().get().getValue();
        map.replaceAll((k, v) -> v / maxRank);
        return map.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .limit(limit).collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private Set<String> getLemmaSet(String searchText) {
        String[] splitText = searchText.split("\\s+");
        Set<String> result = new HashSet<>();
        for (String word : splitText) {
            List<String> lemma = morphology.getLemma(word);
            result.addAll(lemma);
        }
        return result;
    }

    private List<EntityLemma> getLemmaListFromSite(Set<String> words, EntitySite entitySite) {
        List<EntityLemma> entityLemmas = lemmaRepository.selectLemmasBySite(words, entitySite);
        entityLemmas.sort(Comparable::compareTo);
        return entityLemmas;
    }
}