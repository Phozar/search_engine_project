package searchengine.util;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.SitesListCfg;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class NetworkUtil {

    private final SitesListCfg sitesList;


    public Connection.Response getConnection(String url) throws IOException {
        return Jsoup.connect(url).
                ignoreContentType(true).
                userAgent(sitesList.getName())
                .referrer(sitesList.getReferer()).
                timeout(sitesList.getTimeout()).
                followRedirects(false).
                execute();
    }


    public Boolean isAvailableContent(Connection.Response response) {
        return ((response != null)
                && (response.contentType().equalsIgnoreCase(sitesList.getContentType()) &&
                (response.statusCode() == HttpStatus.OK.value())));
    }
}
