package io.github.susimsek.springgrpcsamples.repository;

import io.github.susimsek.springgrpcsamples.domain.TodoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<TodoEntity, Long> {}
