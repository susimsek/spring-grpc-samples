package io.github.susimsek.springgrpcsamples.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springgrpcsamples.IntegrationTest;
import io.github.susimsek.springgrpcsamples.domain.TodoEntity;
import io.github.susimsek.springgrpcsamples.proto.AuthServiceGrpc;
import io.github.susimsek.springgrpcsamples.proto.CreateTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.DeleteTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.DeleteTodoResponse;
import io.github.susimsek.springgrpcsamples.proto.GetTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.ListTodosRequest;
import io.github.susimsek.springgrpcsamples.proto.PageRequest;
import io.github.susimsek.springgrpcsamples.proto.PatchTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.Todo;
import io.github.susimsek.springgrpcsamples.proto.TodoList;
import io.github.susimsek.springgrpcsamples.proto.TodoServiceGrpc;
import io.github.susimsek.springgrpcsamples.proto.Token;
import io.github.susimsek.springgrpcsamples.proto.UpdateTodoRequest;
import io.github.susimsek.springgrpcsamples.repository.TodoRepository;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestConstructor;

@IntegrationTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class TodoGrpcServiceIT {

    private static final Metadata.Key<String> AUTHORIZATION_HEADER =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private final AuthServiceGrpc.AuthServiceBlockingStub authServiceStub;
    private final TodoServiceGrpc.TodoServiceBlockingStub todoServiceStub;
    private final TodoRepository todoRepository;

    TodoGrpcServiceIT(
            AuthServiceGrpc.AuthServiceBlockingStub authServiceStub,
            TodoServiceGrpc.TodoServiceBlockingStub todoServiceStub,
            TodoRepository todoRepository) {
        this.authServiceStub = authServiceStub;
        this.todoServiceStub = todoServiceStub;
        this.todoRepository = todoRepository;
    }

    @BeforeEach
    void setUp() {
        todoRepository.deleteAll();
    }

    @Test
    void createTodoCreatesTodo() {
        Todo response =
                authenticatedTodoStub()
                        .createTodo(CreateTodoRequest.newBuilder().setTitle("New todo").build());

        assertThat(response.getId()).isPositive();
        assertThat(response.getTitle()).isEqualTo("New todo");
        assertThat(response.getCompleted()).isFalse();
        assertThat(response.hasCreatedAt()).isTrue();
        assertThat(response.hasUpdatedAt()).isTrue();
        assertThat(todoRepository.findById(response.getId())).isPresent();
    }

    @Test
    void getTodoReturnsTodo() {
        TodoEntity saved = todoRepository.save(todo("Existing todo", true));

        Todo response =
                authenticatedTodoStub()
                        .getTodo(GetTodoRequest.newBuilder().setId(saved.getId()).build());

        assertThat(response.getId()).isEqualTo(saved.getId());
        assertThat(response.getTitle()).isEqualTo("Existing todo");
        assertThat(response.getCompleted()).isTrue();
    }

    @Test
    void listTodosReturnsPageForAuthenticatedAdmin() {
        todoRepository.save(todo("First todo", false));
        todoRepository.save(todo("Second todo", true));
        todoRepository.save(todo("Third todo", false));

        TodoList response =
                authenticatedTodoStub()
                        .listTodos(
                                ListTodosRequest.newBuilder()
                                        .setPageRequest(
                                                PageRequest.newBuilder()
                                                        .setPage(0)
                                                        .setSize(2)
                                                        .build())
                                        .build());

        assertThat(response.getItemsCount()).isEqualTo(2);
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(3);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.getFirst()).isTrue();
        assertThat(response.getLast()).isFalse();
    }

    @Test
    void updateTodoReplacesTodoFields() {
        TodoEntity saved = todoRepository.save(todo("Before update", false));

        Todo response =
                authenticatedTodoStub()
                        .updateTodo(
                                UpdateTodoRequest.newBuilder()
                                        .setId(saved.getId())
                                        .setTitle("After update")
                                        .setCompleted(true)
                                        .build());

        assertThat(response.getId()).isEqualTo(saved.getId());
        assertThat(response.getTitle()).isEqualTo("After update");
        assertThat(response.getCompleted()).isTrue();
        TodoEntity updated = todoRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("After update");
        assertThat(updated.isCompleted()).isTrue();
    }

    @Test
    void patchTodoUpdatesOnlyProvidedFields() {
        TodoEntity saved = todoRepository.save(todo("Before patch", false));

        Todo response =
                authenticatedTodoStub()
                        .patchTodo(
                                PatchTodoRequest.newBuilder()
                                        .setId(saved.getId())
                                        .setCompleted(true)
                                        .build());

        assertThat(response.getId()).isEqualTo(saved.getId());
        assertThat(response.getTitle()).isEqualTo("Before patch");
        assertThat(response.getCompleted()).isTrue();
        TodoEntity patched = todoRepository.findById(saved.getId()).orElseThrow();
        assertThat(patched.getTitle()).isEqualTo("Before patch");
        assertThat(patched.isCompleted()).isTrue();
    }

    @Test
    void deleteTodoDeletesTodo() {
        TodoEntity saved = todoRepository.save(todo("Delete me", false));

        DeleteTodoResponse response =
                authenticatedTodoStub()
                        .deleteTodo(DeleteTodoRequest.newBuilder().setId(saved.getId()).build());

        assertThat(response.getId()).isEqualTo(saved.getId());
        assertThat(response.getDeleted()).isTrue();
        assertThat(todoRepository.findById(saved.getId())).isEmpty();
    }

    private TodoServiceGrpc.TodoServiceBlockingStub authenticatedTodoStub() {
        Token token =
                authServiceStub.login(
                        io.github.susimsek.springgrpcsamples.proto.LoginRequest.newBuilder()
                                .setUsername("admin")
                                .setPassword("admin")
                                .build());
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION_HEADER, "Bearer " + token.getAccessToken());
        return todoServiceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
    }

    private static TodoEntity todo(String title, boolean completed) {
        TodoEntity todo = new TodoEntity();
        todo.setTitle(title);
        todo.setCompleted(completed);
        return todo;
    }
}
