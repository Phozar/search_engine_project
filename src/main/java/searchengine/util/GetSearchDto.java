package searchengine.util;

import lombok.extern.slf4j.Slf4j;
import searchengine.dto.statistics.SearchDto;
import searchengine.model.EntityPage;
import searchengine.util.morphology.Morphology;

import java.util.*;
import java.util.concurrent.Callable;

@Slf4j
public class GetSearchDto implements Callable {
    private final Map.Entry<EntityPage, Float> entry;
    private final Set<String> entityLemmas;
    private final Morphology morphology;

    public GetSearchDto(Map.Entry<EntityPage, Float> entry, Set<String> entityLemmas, Morphology morphology) {
        this.entry = entry;
        this.entityLemmas = entityLemmas;
        this.morphology = morphology;
    }

    @Override
    public SearchDto call() {
        SearchDto searchDto = new SearchDto();
        searchDto.setRelevance(entry.getValue());
        searchDto.setUri(entry.getKey().getPath());
        searchDto.setSite(entry.getKey().getSite().getUrl());
        searchDto.setSiteName(entry.getKey().getSite().getName());
        StringBuilder stringBuilder = new StringBuilder();

        String body = ClearHtmlCode.clear(entry.getKey().getContent(), "body");
        String title = getTitle(body);
        searchDto.setTitle(title);
        stringBuilder.append(title).append(" ").append(body);
        searchDto.setSnippet(getSnippet(stringBuilder.toString(), entityLemmas));
        return searchDto;
    }

    private String getSnippet(String content, Set<String> lemmaList) {
        List<Integer> lemmaIndex = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        for (String lemma : lemmaList) {
            lemmaIndex.addAll(morphology.findLemmaIndexInText(content, lemma));
        }
        Collections.sort(lemmaIndex);
        List<String> wordsList = getWordsFromContent(content, lemmaIndex);
        for (int i = 0; i < wordsList.size(); i++) {
            result.append(wordsList.get(i)).append("... ");
            if (i > 3) {
                break;
            }
        }
        return result.toString();
    }

    private List<String> getWordsFromContent(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmaIndex.size(); i++) {
            int start = lemmaIndex.get(i);

            int end = content.indexOf(" ", start);

            int nextPoint = i + 1;
            while (nextPoint < lemmaIndex.size() && lemmaIndex.get(nextPoint) - end > 0 && lemmaIndex.get(nextPoint) - end < 5) {
                end = content.indexOf(" ", lemmaIndex.get(nextPoint));
                nextPoint += 1;
            }
            i = nextPoint - 1;
            String text = getWordsFromIndex(start, end, content);
            result.add(text);
        }
        result.sort(Comparator.comparingInt(String::length).reversed());
        return result;
    }

    private String getWordsFromIndex(int start, int end, String content) {
        String word = content.substring(start, end);
        int prevPoint;
        int lastPoint;
        if (content.lastIndexOf(" ", start) != -1) {
            prevPoint = content.lastIndexOf(" ", start);
        } else prevPoint = start;
        if (content.indexOf(" ", end + 30) != -1) {
            lastPoint = content.indexOf(" ", end + 30);
        } else lastPoint = content.indexOf(" ", end);
        String text = content.substring(prevPoint, lastPoint);
        try {
            text = text.replaceAll(word, "<b>" + word + "</b>");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return text;
    }

    private String getTitle(String body) {
        String title = ClearHtmlCode.clear(entry.getKey().getContent(), "title");
        if (title.length() < 2) {
            int indexOfFirsWord = body.indexOf(" ");
            int indexOfTwoWord = body.indexOf(" ", indexOfFirsWord + 1);
            return body.substring(0, indexOfTwoWord);
        }
        return title;
    }
}
