package io.github.susimsek.springgrpcsamples.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springgrpcsamples.domain.HibernateProxySupport.ProxyTodoEntity;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TodoEntityTest {

    @Test
    void accessorsAndEqualityWork() {
        TodoEntity todo = new TodoEntity();
        Instant createdAt = Instant.EPOCH;
        Instant updatedAt = Instant.EPOCH.plusSeconds(1);

        todo.setId(1L);
        todo.setTitle("Title");
        todo.setCompleted(true);
        todo.setCreatedAt(createdAt);
        todo.setUpdatedAt(updatedAt);

        TodoEntity same = new TodoEntity(1L, "Other", false);
        TodoEntity different = new TodoEntity(2L, "Title", true);
        TodoEntity withoutId = new TodoEntity(null, "Title", true);
        TodoEntity otherWithoutId = new TodoEntity(null, "Other", false);

        assertThat(todo.getId()).isEqualTo(1L);
        assertThat(todo.getTitle()).isEqualTo("Title");
        assertThat(todo.isCompleted()).isTrue();
        assertThat(todo.getCreatedAt()).isEqualTo(createdAt);
        assertThat(todo.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(todo)
                .isEqualTo(todo)
                .isEqualTo(same)
                .isNotEqualTo(different)
                .isNotEqualTo(withoutId)
                .isNotEqualTo(otherWithoutId)
                .isNotEqualTo(null)
                .isNotEqualTo("todo");
        assertThat(todo.hashCode()).isEqualTo(TodoEntity.class.hashCode());
        assertThat(withoutId).isNotEqualTo(todo);
    }

    @Test
    void equalitySupportsHibernateProxy() {
        TodoEntity todo = new TodoEntity(1L, "Title", false);
        ProxyTodoEntity proxy = new ProxyTodoEntity(TodoEntity.class);
        proxy.setId(1L);

        assertThat(todo).isEqualTo(proxy);
        assertThat(proxy).isEqualTo(todo);
        assertThat(proxy.hashCode()).isEqualTo(TodoEntity.class.hashCode());
    }
}
