package io.github.susimsek.springgrpcsamples.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.github.susimsek.springgrpcsamples.domain.TodoEntity;
import io.github.susimsek.springgrpcsamples.exception.TodoNotFoundException;
import io.github.susimsek.springgrpcsamples.mapper.TodoMapper;
import io.github.susimsek.springgrpcsamples.proto.CreateTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.DeleteTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.DeleteTodoResponse;
import io.github.susimsek.springgrpcsamples.proto.GetTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.ListTodosRequest;
import io.github.susimsek.springgrpcsamples.proto.PageRequest;
import io.github.susimsek.springgrpcsamples.proto.PatchTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.Todo;
import io.github.susimsek.springgrpcsamples.proto.TodoList;
import io.github.susimsek.springgrpcsamples.proto.UpdateTodoRequest;
import io.github.susimsek.springgrpcsamples.repository.TodoRepository;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TodoGrpcServiceTest {

    @Mock private TodoRepository todoRepository;

    @Spy private TodoMapper todoMapper = Mappers.getMapper(TodoMapper.class);

    @InjectMocks private TodoGrpcService service;

    private Map<Long, TodoEntity> todos;
    private long sequence;
    private long auditSequence;

    @BeforeEach
    void setUp() {
        todos = new LinkedHashMap<>();
        sequence = 1;
        auditSequence = 1;

        lenient()
                .when(todoRepository.save(any(TodoEntity.class)))
                .thenAnswer(invocation -> saveTodo(invocation.getArgument(0)));
        lenient()
                .when(todoRepository.findById(anyLong()))
                .thenAnswer(invocation -> findTodo(invocation.getArgument(0)));
        lenient()
                .when(todoRepository.findAll(any(Pageable.class)))
                .thenAnswer(invocation -> findTodos(invocation.getArgument(0)));
        lenient()
                .when(todoRepository.existsById(anyLong()))
                .thenAnswer(invocation -> todos.containsKey(invocation.getArgument(0)));
        lenient()
                .doAnswer(
                        invocation -> {
                            TodoEntity todo = invocation.getArgument(0);
                            todos.remove(todo.getId());
                            return null;
                        })
                .when(todoRepository)
                .delete(any(TodoEntity.class));
    }

    @Test
    void createTodoStoresNewIncompleteTodo() {
        RecordingObserver<Todo> observer = new RecordingObserver<>();

        service.createTodo(
                CreateTodoRequest.newBuilder().setTitle("Create unit tests").build(), observer);

        assertThat(observer.values())
                .singleElement()
                .satisfies(
                        todo -> {
                            assertThat(todo.getId()).isPositive();
                            assertThat(todo.getTitle()).isEqualTo("Create unit tests");
                            assertThat(todo.getCompleted()).isFalse();
                            assertThat(todo.hasCreatedAt()).isTrue();
                            assertThat(todo.hasUpdatedAt()).isTrue();
                        });
        assertThat(observer.completed()).isTrue();
        assertThat(observer.error()).isNull();
    }

    @Test
    void createTodoHandlesUnhandledExceptionsAsInternalStatus() {
        RecordingObserver<Todo> observer = new RecordingObserver<>();
        when(todoRepository.save(any(TodoEntity.class)))
                .thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(
                        () ->
                                service.createTodo(
                                        CreateTodoRequest.newBuilder()
                                                .setTitle("Trigger failure")
                                                .build(),
                                        observer))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(observer.completed()).isFalse();
        assertThat(observer.values()).isEmpty();
    }

    @Test
    void getTodoValidatesIdAndMissingRows() {
        RecordingObserver<Todo> missingObserver = new RecordingObserver<>();

        assertThatThrownBy(
                        () ->
                                service.getTodo(
                                        GetTodoRequest.newBuilder().setId(999L).build(),
                                        missingObserver))
                .isInstanceOf(TodoNotFoundException.class)
                .hasMessage("todo not found with id: 999");
    }

    @Test
    void getTodoReturnsExistingTodo() {
        Todo created = createTodo("Fetch me");
        RecordingObserver<Todo> observer = new RecordingObserver<>();

        service.getTodo(GetTodoRequest.newBuilder().setId(created.getId()).build(), observer);

        assertThat(observer.values()).containsExactly(created);
        assertThat(observer.completed()).isTrue();
    }

    @Test
    void listTodosReturnsRequestedPage() {
        Todo first = createTodo("First");
        createTodo("Second");
        Todo third = createTodo("Third");
        RecordingObserver<TodoList> observer = new RecordingObserver<>();

        service.listTodos(
                ListTodosRequest.newBuilder()
                        .setPageRequest(PageRequest.newBuilder().setPage(1).setSize(2).build())
                        .build(),
                observer);

        assertThat(observer.values())
                .singleElement()
                .satisfies(
                        response -> {
                            assertThat(response.getItemsList()).containsExactly(third);
                            assertThat(response.getPage()).isEqualTo(1);
                            assertThat(response.getSize()).isEqualTo(2);
                            assertThat(response.getTotalElements()).isEqualTo(3);
                            assertThat(response.getTotalPages()).isEqualTo(2);
                            assertThat(response.getFirst()).isFalse();
                            assertThat(response.getLast()).isTrue();
                        });
        assertThat(observer.completed()).isTrue();
        assertThat(first.getId()).isEqualTo(1L);
    }

    @Test
    void listTodosValidatesPaginationRequest() {
        assertThat(listTodos(0, 0).values())
                .singleElement()
                .satisfies(
                        response -> {
                            assertThat(response.getPage()).isZero();
                            assertThat(response.getSize()).isEqualTo(20);
                            assertThat(response.getFirst()).isTrue();
                            assertThat(response.getLast()).isTrue();
                        });
    }

    @Test
    void updateTodoValidatesRequest() {
        assertThatThrownBy(() -> updateTodo(999L, "New title"))
                .isInstanceOf(TodoNotFoundException.class)
                .hasMessage("todo not found with id: 999");
    }

    @Test
    void updateTodoReplacesExistingTodo() {
        Todo created = createTodo("Old title");

        RecordingObserver<Todo> observer = updateTodo(created.getId(), "New title");

        assertThat(observer.values())
                .singleElement()
                .satisfies(
                        todo -> {
                            assertThat(todo.getId()).isEqualTo(created.getId());
                            assertThat(todo.getTitle()).isEqualTo("New title");
                            assertThat(todo.getCompleted()).isTrue();
                            assertThat(todo.hasCreatedAt()).isTrue();
                            assertThat(todo.hasUpdatedAt()).isTrue();
                        });
        assertThat(observer.completed()).isTrue();
    }

    @Test
    void patchTodoValidatesRequest() {
        assertThatThrownBy(
                        () ->
                                patchTodo(
                                        PatchTodoRequest.newBuilder()
                                                .setId(999L)
                                                .setTitle("Patched title")
                                                .build()))
                .isInstanceOf(TodoNotFoundException.class)
                .hasMessage("todo not found with id: 999");
    }

    @Test
    void patchTodoUpdatesOnlyProvidedFields() {
        Todo created = createTodo("Keep title");

        RecordingObserver<Todo> completedOnly =
                patchTodo(
                        PatchTodoRequest.newBuilder()
                                .setId(created.getId())
                                .setCompleted(true)
                                .build());
        RecordingObserver<Todo> titleOnly =
                patchTodo(
                        PatchTodoRequest.newBuilder()
                                .setId(created.getId())
                                .setTitle("New title")
                                .build());

        assertThat(completedOnly.values())
                .singleElement()
                .satisfies(
                        todo -> {
                            assertThat(todo.getTitle()).isEqualTo("Keep title");
                            assertThat(todo.getCompleted()).isTrue();
                        });
        assertThat(titleOnly.values())
                .singleElement()
                .satisfies(
                        todo -> {
                            assertThat(todo.getTitle()).isEqualTo("New title");
                            assertThat(todo.getCompleted()).isTrue();
                        });
    }

    @Test
    void deleteTodoValidatesRequest() {
        assertThatThrownBy(() -> deleteTodo(999L))
                .isInstanceOf(TodoNotFoundException.class)
                .hasMessage("todo not found with id: 999");
    }

    @Test
    void deleteTodoRemovesExistingTodo() {
        Todo created = createTodo("Delete me");

        RecordingObserver<DeleteTodoResponse> observer = deleteTodo(created.getId());
        RecordingObserver<Todo> getObserver = new RecordingObserver<>();

        assertThat(observer.values())
                .singleElement()
                .satisfies(
                        response -> {
                            assertThat(response.getId()).isEqualTo(created.getId());
                            assertThat(response.getDeleted()).isTrue();
                        });
        assertThat(observer.completed()).isTrue();
        assertThatThrownBy(
                        () ->
                                service.getTodo(
                                        GetTodoRequest.newBuilder().setId(created.getId()).build(),
                                        getObserver))
                .isInstanceOf(TodoNotFoundException.class)
                .hasMessage("todo not found with id: " + created.getId());
    }

    private Todo createTodo(String title) {
        RecordingObserver<Todo> observer = new RecordingObserver<>();
        service.createTodo(CreateTodoRequest.newBuilder().setTitle(title).build(), observer);
        return observer.values().getFirst();
    }

    private RecordingObserver<Todo> updateTodo(long id, String title) {
        RecordingObserver<Todo> observer = new RecordingObserver<>();
        service.updateTodo(
                UpdateTodoRequest.newBuilder().setId(id).setTitle(title).setCompleted(true).build(),
                observer);
        return observer;
    }

    private RecordingObserver<Todo> patchTodo(PatchTodoRequest request) {
        RecordingObserver<Todo> observer = new RecordingObserver<>();
        service.patchTodo(request, observer);
        return observer;
    }

    private RecordingObserver<TodoList> listTodos(int page, int size) {
        RecordingObserver<TodoList> observer = new RecordingObserver<>();
        service.listTodos(
                ListTodosRequest.newBuilder()
                        .setPageRequest(
                                PageRequest.newBuilder().setPage(page).setSize(size).build())
                        .build(),
                observer);
        return observer;
    }

    private RecordingObserver<DeleteTodoResponse> deleteTodo(long id) {
        RecordingObserver<DeleteTodoResponse> observer = new RecordingObserver<>();
        service.deleteTodo(DeleteTodoRequest.newBuilder().setId(id).build(), observer);
        return observer;
    }

    private static final class RecordingObserver<T> implements StreamObserver<T> {

        private final List<T> values = new ArrayList<>();
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(T value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable throwable) {
            error = throwable;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }

        private List<T> values() {
            return values;
        }

        private Throwable error() {
            return error;
        }

        private boolean completed() {
            return completed;
        }
    }

    private TodoEntity saveTodo(TodoEntity todo) {
        if (todo.getId() == null) {
            todo.setId(sequence++);
        }
        if (todo.getCreatedAt() == null) {
            todo.setCreatedAt(nextAuditTime());
        }
        todo.setUpdatedAt(nextAuditTime());
        todos.put(todo.getId(), todo);
        return todo;
    }

    private Optional<TodoEntity> findTodo(Long id) {
        return Optional.ofNullable(todos.get(id));
    }

    private Page<TodoEntity> findTodos(Pageable pageable) {
        List<TodoEntity> values = List.copyOf(todos.values());
        int start = Math.min((int) pageable.getOffset(), values.size());
        int end = Math.min(start + pageable.getPageSize(), values.size());
        return new PageImpl<>(values.subList(start, end), pageable, values.size());
    }

    private Instant nextAuditTime() {
        return Instant.EPOCH.plusSeconds(auditSequence++);
    }
}
