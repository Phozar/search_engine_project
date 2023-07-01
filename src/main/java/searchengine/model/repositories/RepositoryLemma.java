package searchengine.model.repositories;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.EntityLemma;
import searchengine.model.EntitySite;

import java.util.List;
import java.util.Set;

@Repository
@ConfigurationProperties(prefix = "config-frequency")
public interface RepositoryLemma extends JpaRepository<EntityLemma, Integer> {

    EntityLemma findByLemmaAndSite(String lemma, EntitySite entitySite);

    Integer countBySite(EntitySite entitySite);

    @Query("select a from EntityLemma as a where a.frequency<300" +
            " and a.lemma in (:lemmas) " +
            " and a.site=:site")
    List<EntityLemma> selectLemmasBySite(Set<String> lemmas, EntitySite site);

}
