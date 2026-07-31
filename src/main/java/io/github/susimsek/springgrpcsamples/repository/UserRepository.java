package io.github.susimsek.springgrpcsamples.repository;

import io.github.susimsek.springgrpcsamples.domain.UserEntity;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    String USER_BY_USERNAME_CACHE = "usersByUsername";

    @EntityGraph(value = "User.withAuthorities")
    @Cacheable(cacheNames = USER_BY_USERNAME_CACHE, key = "#username", unless = "#result == null")
    Optional<UserEntity> findByUsername(String username);
}
