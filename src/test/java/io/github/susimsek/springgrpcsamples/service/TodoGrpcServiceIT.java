package io.github.susimsek.springgrpcsamples.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springgrpcsamples.IntegrationTest;
import io.github.susimsek.springgrpcsamples.domain.TodoEntity;
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
import io.github.susimsek.springgrpcsamples.proto.UpdateTodoRequest;
import io.github.susimsek.springgrpcsamples.repository.TodoRepository;
import io.github.susimsek.springgrpcsamples.security.AuthoritiesConstants;
import io.github.susimsek.springgrpcsamples.security.JwtService;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestConstructor;

@IntegrationTest
@ImportGrpcClients(basePackageClasses = TodoServiceGrpc.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class TodoGrpcServiceIT {

    private static final Metadata.Key<String> AUTHORIZATION_HEADER =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private final TodoServiceGrpc.TodoServiceBlockingStub todoServiceStub;
    private final TodoRepository todoRepository;
    private final JwtService jwtService;

    TodoGrpcServiceIT(
            TodoServiceGrpc.TodoServiceBlockingStub todoServiceStub,
            TodoRepository todoRepository,
            JwtService jwtService) {
        this.todoServiceStub = todoServiceStub;
        this.todoRepository = todoRepository;
        this.jwtService = jwtService;
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
        assertThat(response.getCreatedBy()).isEqualTo("admin");
        assertThat(response.getLastModifiedBy()).isEqualTo("admin");
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
        assertThat(response.getCreatedBy()).isEqualTo("system");
        assertThat(response.getLastModifiedBy()).isEqualTo("system");
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
        assertThat(response.getCreatedBy()).isEqualTo("system");
        assertThat(response.getLastModifiedBy()).isEqualTo("admin");
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
        assertThat(response.getCreatedBy()).isEqualTo("system");
        assertThat(response.getLastModifiedBy()).isEqualTo("admin");
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
        String token =
                jwtService.generateToken(
                        UsernamePasswordAuthenticationToken.authenticated(
                                "admin",
                                "N/A",
                                List.of(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN))));
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION_HEADER, "Bearer " + token);
        return todoServiceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
    }

    private static TodoEntity todo(String title, boolean completed) {
        TodoEntity todo = new TodoEntity();
        todo.setTitle(title);
        todo.setCompleted(completed);
        return todo;
    }
}
