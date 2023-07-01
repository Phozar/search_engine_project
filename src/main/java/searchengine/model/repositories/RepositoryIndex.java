package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.EntityIndex;
import searchengine.model.EntityLemma;
import searchengine.model.EntityPage;

import java.util.List;

@Repository
public interface RepositoryIndex extends JpaRepository<EntityIndex, Integer> {


    @Query("select i from EntityIndex i where i.lemma IN :lemmas AND i.page IN :pages")
    List<EntityIndex> findByPagesAndLemmas(List<EntityLemma> lemmas,
                                           List<EntityPage> pages);

    EntityIndex findByPageAndLemma(EntityPage entityPage, EntityLemma entityLemma);
}
