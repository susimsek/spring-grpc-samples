package io.github.susimsek.springgrpcsamples.repository;

import io.github.susimsek.springgrpcsamples.domain.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @EntityGraph(value = "User.withAuthorities")
    Optional<UserEntity> findByUsername(String username);
}
