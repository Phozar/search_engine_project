package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.EntitySite;
import searchengine.model.Status;


@Repository
public interface RepositorySite extends JpaRepository<EntitySite, Integer> {

    @Query("select e.status from EntitySite as e where e.url =:url")
    Status findByUrl(String url);

    EntitySite findEntitySiteByUrl(String url);


    @Query("select e.last_error  from EntitySite as e where e.url=:url")
    String findErrorByUrl(String url);
}

