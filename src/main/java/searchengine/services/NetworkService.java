package searchengine.services;

import org.jsoup.Connection;

import java.io.IOException;

public interface NetworkService {

    Connection.Response getConnection(String url) throws IOException;
    Boolean isAvailableContent(Connection.Response response);

}
