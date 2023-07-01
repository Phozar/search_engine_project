package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Setter
@Getter
@Table(name = "index")
public class EntityIndex implements Comparable<EntityIndex> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private EntityLemma lemma;
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private EntityPage page;

    @Column(name = "rank", nullable = false)
    private float rank;


    @Override
    public int compareTo(EntityIndex o) {
        return Float.compare(o.getRank(), this.getRank());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityIndex that = (EntityIndex) o;
        return lemma.equals(that.lemma) && page.equals(that.page);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lemma, page);
    }
}