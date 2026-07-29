package io.github.susimsek.springgrpcsamples.repository;

import io.github.susimsek.springgrpcsamples.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(value = "User.withAuthorities")
    Optional<User> findByUsername(String username);
}
