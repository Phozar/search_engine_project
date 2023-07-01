package searchengine.model;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;


@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "page",uniqueConstraints=@UniqueConstraint(columnNames={"site_id", "path"}))
public class EntityPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Integer id;
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private EntitySite site;

    @Column(columnDefinition = "TEXT NOT NULL")
    private String path;
    @Column(nullable = false)
    private Integer code;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

}
