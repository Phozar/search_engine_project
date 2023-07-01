package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesListCfg {
    private String name;
    private String referer;
    private Integer timeout;
    private List<SiteCfg> sites;
    private String contentType;
}
