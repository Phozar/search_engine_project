package searchengine.dto.statistics;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class SearchDto implements Comparable<SearchDto>{
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private Float relevance;


    @Override
    public int compareTo(SearchDto o) {
        return this.getRelevance().compareTo(o.getRelevance());
    }
}