package authService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.GrantedAuthority;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@EnableJpaAuditing
public class Role implements GrantedAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private RoleType name;

    private String description;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    public Role(RoleType name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    @Override
    public String getAuthority() {
        return name.name();
    }
}
