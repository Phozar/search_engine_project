package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.EntityPage;
import searchengine.model.EntitySite;

import java.util.concurrent.CopyOnWriteArrayList;


@Repository
public interface RepositoryPage extends JpaRepository<EntityPage, Integer> {

    @Query("select COUNT(*) from EntityPage AS e group by e.site having e.site =:site")
    Integer findCountBySite(EntitySite site);

    EntityPage findByPathAndSiteId(String path, Integer siteId);

    @Query("select p from EntityPage as p " +
            "join EntityIndex as i on p.id = i.page.id " +
            "where i.lemma.lemma =:lemma and p.site =:site")
    CopyOnWriteArrayList<EntityPage> findByLemma(String lemma, EntitySite site);
}
