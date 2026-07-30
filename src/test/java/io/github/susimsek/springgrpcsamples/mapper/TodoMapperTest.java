package io.github.susimsek.springgrpcsamples.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springgrpcsamples.domain.TodoEntity;
import io.github.susimsek.springgrpcsamples.proto.CreateTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.PatchTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.Todo;
import io.github.susimsek.springgrpcsamples.proto.UpdateTodoRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class TodoMapperTest {

    private final TodoMapper mapper = Mappers.getMapper(TodoMapper.class);

    @Test
    void mapsCreateTodoRequestToEntity() {
        TodoEntity todo =
                mapper.toEntity(CreateTodoRequest.newBuilder().setTitle("Mapped title").build());

        assertThat(todo.getId()).isNull();
        assertThat(todo.getTitle()).isEqualTo("Mapped title");
        assertThat(todo.isCompleted()).isFalse();
        assertThat(todo.getCreatedAt()).isNull();
        assertThat(todo.getUpdatedAt()).isNull();
    }

    @Test
    void updatesEntityFromUpdateTodoRequest() {
        TodoEntity todo = new TodoEntity(1L, "Old title", false);
        todo.setCreatedAt(Instant.EPOCH);
        todo.setUpdatedAt(Instant.EPOCH);

        mapper.updateEntity(
                UpdateTodoRequest.newBuilder()
                        .setId(1L)
                        .setTitle("Mapped title")
                        .setCompleted(true)
                        .build(),
                todo);

        assertThat(todo.getId()).isEqualTo(1L);
        assertThat(todo.getTitle()).isEqualTo("Mapped title");
        assertThat(todo.isCompleted()).isTrue();
        assertThat(todo.getCreatedAt()).isEqualTo(Instant.EPOCH);
        assertThat(todo.getUpdatedAt()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void partiallyUpdatesOnlyPresentPatchFields() {
        TodoEntity todo = new TodoEntity(1L, "Old title", false);

        mapper.patchEntity(
                PatchTodoRequest.newBuilder().setId(1L).setCompleted(true).build(), todo);

        assertThat(todo.getTitle()).isEqualTo("Old title");
        assertThat(todo.isCompleted()).isTrue();
    }

    @Test
    void mapsTodoToResponseWithTimestamps() {
        TodoEntity todo = new TodoEntity(1L, "Mapped title", true);
        todo.setCreatedAt(Instant.EPOCH);
        todo.setUpdatedAt(Instant.EPOCH.plusSeconds(1));

        Todo response = mapper.toProto(todo);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Mapped title");
        assertThat(response.getCompleted()).isTrue();
        assertThat(response.hasCreatedAt()).isTrue();
        assertThat(response.hasUpdatedAt()).isTrue();
    }

    @Test
    void mapsTodoToResponseWithoutTimestamps() {
        Todo response = mapper.toProto(new TodoEntity(1L, "Mapped title", false));

        assertThat(response.hasCreatedAt()).isFalse();
        assertThat(response.hasUpdatedAt()).isFalse();
    }
}
